package com.chumor.downgrader.data.remote

import android.util.Base64
import com.chumor.downgrader.domain.model.FirmwarePackage
import com.chumor.downgrader.domain.model.QueryError
import com.chumor.downgrader.domain.model.QueryException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.HttpsURLConnection
import org.json.JSONException
import org.json.JSONObject

class VivoUpgradeApi {
    fun query(credential: String): FirmwarePackage {
        request(AUTO_DETECT_ENDPOINT, credential)
        val packageResponse = request(DEGRADE_PACKAGE_ENDPOINT, credential)

        val data = packageResponse.optJSONObject("data")
            ?: throw QueryException(
                QueryError.PackageUnavailable,
                message = "POST $DEGRADE_PACKAGE_ENDPOINT\n$packageResponse",
            )

        return FirmwarePackage(
            systemVersion = data.optString("sysVer", ""),
            androidVersion = data.optString("androidVer", ""),
            romVersion = data.optString("romVer", ""),
            sha256 = data.optString("sha256", ""),
            fileSizeBytes = data.optLong("fileSize", 0L),
            releaseTime = data.optString("releaseTime", ""),
            downloadUrl = data.optString("downloadUrl", ""),
            certificate = data.optString("certificate", ""),
        )
    }

    private fun requireSuccess(response: JSONObject) {
        val returnCode = try {
            response.getInt("retcode")
        } catch (exception: JSONException) {
            throw QueryException(QueryError.InvalidResponse, exception)
        }
        if (returnCode != SUCCESS_CODE) {
            throw QueryException(
                QueryError.ServerRejected(
                    code = returnCode,
                    serverMessage = response.optString("message", ""),
                ),
            )
        }
    }

    private fun request(endpoint: String, credential: String): JSONObject = try {
        executeRequest(endpoint, credential).also(::requireSuccess)
    } catch (exception: QueryException) {
        throw QueryException(exception.error, exception, "POST $endpoint")
    }

    private fun executeRequest(endpoint: String, credential: String): JSONObject {
        val sessionBytes = ByteArray(SESSION_RANDOM_BYTES).also(secureRandom::nextBytes)
        val session = sessionBytes.toHexString()
        val encryptedVsd = try {
            val requestData = JSONObject()
                .put("sv", TOOL_VERSION)
                .put("vsd", session)
                .toString()
                .toByteArray(StandardCharsets.UTF_8)
            val rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            rsa.init(Cipher.ENCRYPT_MODE, loadServerPublicKey())
            Base64.encodeToString(
                rsa.doFinal(requestData),
                Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING,
            )
        } catch (exception: GeneralSecurityException) {
            throw QueryException(QueryError.InvalidResponse, exception)
        } catch (exception: JSONException) {
            throw QueryException(QueryError.InvalidResponse, exception)
        }

        val connection = try {
            (URL("$HOST$endpoint?vsd=$encryptedVsd").openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                connectTimeout = CONNECTION_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Content-Type", "text/plain")
            }
        } catch (exception: IOException) {
            throw QueryException(QueryError.Network, exception)
        }

        try {
            connection.outputStream.use { output ->
                // The service accepts the device credential in the raw request body.
                output.write("jvq_param=$credential".toByteArray(StandardCharsets.UTF_8))
            }

            val httpStatus = connection.responseCode
            if (httpStatus in 300..399) {
                throw QueryException(QueryError.HttpStatus(httpStatus), message = connection.responseMessage)
            }

            val responseStream = if (httpStatus >= 400) connection.errorStream else connection.inputStream
            if (responseStream == null) {
                if (httpStatus !in 200..299) {
                    throw QueryException(
                        QueryError.HttpStatus(httpStatus),
                        message = "HTTP $httpStatus: empty response body",
                    )
                }
                throw QueryException(
                    QueryError.InvalidResponse,
                    message = "HTTP $httpStatus: empty response body",
                )
            }
            val responseBody = responseStream.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { it.readText() }
            }.trim()
            if (responseBody.isEmpty()) {
                if (httpStatus !in 200..299) {
                    throw QueryException(
                        QueryError.HttpStatus(httpStatus),
                        message = "HTTP $httpStatus: empty response body",
                    )
                }
                throw QueryException(
                    QueryError.InvalidResponse,
                    message = "HTTP $httpStatus: empty response body",
                )
            }

            try {
                return decryptResponse(responseBody, session)
            } catch (exception: QueryException) {
                throw QueryException(
                    error = if (httpStatus !in 200..299) QueryError.HttpStatus(httpStatus) else exception.error,
                    cause = exception,
                    message = "HTTP $httpStatus\n$responseBody",
                )
            }
        } catch (exception: QueryException) {
            throw exception
        } catch (exception: IOException) {
            throw QueryException(QueryError.Network, exception)
        } finally {
            connection.disconnect()
        }
    }

    private fun decryptResponse(base64Payload: String, session: String): JSONObject {
        try {
            val encrypted = Base64.decode(base64Payload, Base64.DEFAULT)
            val aes = Cipher.getInstance("AES/CBC/NoPadding")
            aes.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(RESPONSE_KEY, "AES"),
                IvParameterSpec(session.toByteArray(StandardCharsets.US_ASCII)),
            )
            val plaintext = String(aes.doFinal(encrypted), StandardCharsets.UTF_8)
            val jsonStart = plaintext.indexOf('{')
            val jsonEnd = plaintext.lastIndexOf('}')
            if (jsonStart < 0 || jsonEnd <= jsonStart) {
                throw QueryException(
                    QueryError.InvalidResponse,
                    message = "Decrypted response does not contain a JSON object",
                )
            }
            return JSONObject(plaintext.substring(jsonStart, jsonEnd + 1))
        } catch (exception: IllegalArgumentException) {
            throw QueryException(QueryError.InvalidResponse, exception)
        } catch (exception: GeneralSecurityException) {
            throw QueryException(QueryError.InvalidResponse, exception)
        } catch (exception: JSONException) {
            throw QueryException(QueryError.InvalidResponse, exception)
        }
    }

    private fun loadServerPublicKey() = try {
        val encoded = PUBLIC_KEY_PEM
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .filterNot(Char::isWhitespace)
        val der = Base64.decode(encoded, Base64.DEFAULT)
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(der))
    } catch (exception: IllegalArgumentException) {
        throw GeneralSecurityException("Invalid server key", exception)
    }

    private companion object {
        const val HOST = "https://sysupgrade.vivo.com.cn"
        const val AUTO_DETECT_ENDPOINT = "/degrade/getAutoDetectSwitch"
        const val DEGRADE_PACKAGE_ENDPOINT = "/degrade/getDegradePkg"
        const val TOOL_VERSION = "1.1.1"
        const val SUCCESS_CODE = 200
        const val SESSION_RANDOM_BYTES = 8
        const val CONNECTION_TIMEOUT_MILLIS = 30_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val USER_AGENT = "vivoAssist"
        val secureRandom = SecureRandom()
        val RESPONSE_KEY = "181120800577d693a5746b4cf212c068".toByteArray(StandardCharsets.US_ASCII)
        val PUBLIC_KEY_PEM = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxdbDMVMCozRht8/A6wyR
            oGXKhHVsPLTUVxLg3d9MRaAoA69Fv54hX+OxqR2CNH5hHQizuQhDQ29bq7X3ze3L
            0WxwzsKbcyzvgJ8B4OUjhwdhtoFC0NW1tZAHM/HqxbmI9H7m3wop10EXfi7+MChD
            xldlu7rxV10Nebm3C3fLJLfacyLfDED1j7tdBUu51g9lgZZjPwwxTzGS7e4Xx4W+
            5co+nU2/5qGL7NLVeKXxqIHDvAnQ7zYoGX7UL3hHEhhHgCg9VIGLdlzxvFSx1idw
            4kPGi+cXEOZoVVn+B5EQ9UaobdpScbKCKoYKvXdqS3ZhnmafkIVkxMv2FfMh1jGH
            awIDAQAB
            -----END PUBLIC KEY-----
        """.trimIndent()
    }
}
