package com.chumor.downgrader.data.storage

import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import com.chumor.downgrader.domain.model.isSha256Digest
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext

class FirmwareDownloader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    @OptIn(InternalCoroutinesApi::class)
    suspend fun download(
        downloadUrl: String,
        fileName: String,
        expectedSizeBytes: Long,
        expectedSha256: String,
        onProgress: suspend (Long) -> Unit,
        onVerifying: suspend (Long) -> Unit,
    ) = withContext(ioDispatcher) {
        require(fileName.isNotBlank() && File(fileName).name == fileName)
        require(expectedSizeBytes > 0L)
        require(expectedSha256.isSha256Digest())

        val downloadUri = Uri.parse(downloadUrl)
        require(downloadUri.host != null && downloadUri.scheme in setOf("http", "https"))

        val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECTION_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
        }
        val cancellationHandle = currentCoroutineContext().job.invokeOnCompletion(onCancelling = true) { cause ->
            if (cause != null) connection.disconnect()
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Firmware download returned HTTP $responseCode")
            }

            connection.inputStream.use { input ->
                saveToStorageRoot(fileName, expectedSizeBytes, expectedSha256, input, onProgress, onVerifying)
            }
        } finally {
            cancellationHandle.dispose()
            connection.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun saveToStorageRoot(
        fileName: String,
        expectedSizeBytes: Long,
        expectedSha256: String,
        input: InputStream,
        onProgress: suspend (Long) -> Unit,
        onVerifying: suspend (Long) -> Unit,
    ) {
        val directory = Environment.getExternalStorageDirectory()
        val destination = File(directory, fileName)
        val temporary = File.createTempFile("firmware", ".tmp", directory)
        try {
            val result = FileOutputStream(temporary).use { copyDownload(input, it, onProgress) }
            onVerifying(result.sizeBytes)
            verifyDownload(result, expectedSizeBytes, expectedSha256)
            currentCoroutineContext().ensureActive()
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }

    private suspend fun copyDownload(
        input: InputStream,
        output: OutputStream,
        onProgress: suspend (Long) -> Unit,
    ): DownloadResult {
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        val digest = MessageDigest.getInstance("SHA-256")
        val coroutineContext = currentCoroutineContext()
        var downloadedBytes = 0L
        var lastReportTime = SystemClock.elapsedRealtime()
        while (true) {
            coroutineContext.ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            digest.update(buffer, 0, read)
            downloadedBytes += read
            val now = SystemClock.elapsedRealtime()
            if (now - lastReportTime >= PROGRESS_UPDATE_INTERVAL_MILLIS) {
                onProgress(downloadedBytes)
                lastReportTime = now
            }
        }
        output.flush()
        onProgress(downloadedBytes)
        return DownloadResult(downloadedBytes, digest.digest().toHexString())
    }

    private fun verifyDownload(result: DownloadResult, expectedSizeBytes: Long, expectedSha256: String) {
        if (result.sizeBytes != expectedSizeBytes) {
            throw IOException("Firmware size does not match the service response")
        }
        if (!result.sha256.equals(expectedSha256, ignoreCase = true)) {
            throw IOException("Firmware SHA-256 does not match the service response")
        }
    }

    private data class DownloadResult(val sizeBytes: Long, val sha256: String)

    private companion object {
        const val CONNECTION_TIMEOUT_MILLIS = 30_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val COPY_BUFFER_SIZE = 128 * 1024
        const val PROGRESS_UPDATE_INTERVAL_MILLIS = 500L
    }
}
