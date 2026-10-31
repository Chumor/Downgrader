package com.chumor.downgrader.data.device

import com.chumor.downgrader.domain.model.DeviceInfo
import com.chumor.downgrader.domain.model.DeviceReadError
import com.chumor.downgrader.domain.model.DeviceReadException
import com.chumor.downgrader.domain.model.DeviceReadResult
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class DevicePropertiesReader {
    fun read(): DeviceReadResult {
        val process = try {
            ProcessBuilder("getprop")
                .redirectErrorStream(true)
                .start()
        } catch (exception: IOException) {
            throw DeviceReadException(DeviceReadError.Unavailable, exception)
        } catch (exception: SecurityException) {
            throw DeviceReadException(DeviceReadError.Unavailable, exception)
        }

        val properties = try {
            BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use { reader ->
                buildMap<String, String> {
                    while (true) {
                        val line = reader.readLine() ?: break
                        PROPERTY_LINE.matchEntire(line)?.let { match ->
                            val name = match.groupValues[1]
                            if (name in REQUIRED_PROPERTIES) put(name, match.groupValues[2])
                        }
                    }
                }
            }
        } catch (exception: IOException) {
            process.destroyForcibly()
            throw DeviceReadException(DeviceReadError.Unavailable, exception)
        }

        val exitCode = try {
            process.waitFor()
        } catch (exception: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            throw DeviceReadException(DeviceReadError.Interrupted, exception)
        }

        if (exitCode != 0) {
            throw DeviceReadException(
                DeviceReadError.Failed(exitCode),
            )
        }

        val credential = buildString {
            for (index in 0..5) {
                properties["sys.vivo.dota_info$index"]
                    ?.takeIf(String::isNotEmpty)
                    ?.let(::append)
            }
        }

        return DeviceReadResult(
            info = DeviceInfo(
                model = properties["ro.product.model.bbk"],
                softwareVersion = properties["ro.vivo.product.version"],
                productName = properties["ro.vivo.market.name"],
                serial = properties["ro.boot.serialno"],
                androidVersion = properties["ro.build.version.release"],
            ),
            credential = credential,
        )
    }

    private companion object {
        val PROPERTY_LINE = Regex("""^\[([^\]]+)]\s*:\s*\[(.*)]$""")
        val REQUIRED_PROPERTIES = setOf(
            "ro.product.model.bbk",
            "ro.vivo.product.version",
            "ro.vivo.market.name",
            "ro.boot.serialno",
            "ro.build.version.release",
        ) + (0..5).map { "sys.vivo.dota_info$it" }
    }
}
