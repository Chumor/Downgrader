package com.chumor.downgrader.data.storage

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.chumor.downgrader.MainActivity
import com.chumor.downgrader.R
import kotlinx.coroutines.CancellationException

internal const val FIRMWARE_DOWNLOAD_WORK_NAME = "firmware-download"
internal const val FIRMWARE_DOWNLOAD_PROGRESS_BYTES = "progress_bytes"
internal const val FIRMWARE_DOWNLOAD_EXPECTED_SIZE_BYTES = "expected_size_bytes"
internal const val FIRMWARE_DOWNLOAD_VERIFYING = "verifying"
internal const val FIRMWARE_DOWNLOAD_SHA256_TAG_PREFIX = "firmware-sha256:"
private const val KEY_DOWNLOAD_URL = "download_url"
private const val KEY_FILE_NAME = "file_name"
private const val KEY_EXPECTED_SHA256 = "expected_sha256"

internal fun firmwareDownloadInputData(
    downloadUrl: String,
    fileName: String,
    expectedSizeBytes: Long,
    expectedSha256: String,
) = workDataOf(
    KEY_DOWNLOAD_URL to downloadUrl,
    KEY_FILE_NAME to fileName,
    FIRMWARE_DOWNLOAD_EXPECTED_SIZE_BYTES to expectedSizeBytes,
    KEY_EXPECTED_SHA256 to expectedSha256,
)

class FirmwareDownloadWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: return Result.failure()
        val expectedSizeBytes = inputData.getLong(FIRMWARE_DOWNLOAD_EXPECTED_SIZE_BYTES, 0L)
        val expectedSha256 = inputData.getString(KEY_EXPECTED_SHA256) ?: return Result.failure()

        return try {
            val downloader = FirmwareDownloader()
            createNotificationChannel()
            updateProgress(0L, expectedSizeBytes, verifying = false)
            downloader.download(
                downloadUrl = downloadUrl,
                fileName = fileName,
                expectedSizeBytes = expectedSizeBytes,
                expectedSha256 = expectedSha256,
                onProgress = { downloadedBytes ->
                    updateProgress(downloadedBytes, expectedSizeBytes, verifying = false)
                },
                onVerifying = { downloadedBytes ->
                    updateProgress(downloadedBytes, expectedSizeBytes, verifying = true)
                },
            )
            showCompletedNotification()
            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure()
        }
    }

    private suspend fun updateProgress(downloadedBytes: Long, expectedSizeBytes: Long, verifying: Boolean) {
        setProgress(
            workDataOf(
                FIRMWARE_DOWNLOAD_PROGRESS_BYTES to downloadedBytes,
                FIRMWARE_DOWNLOAD_EXPECTED_SIZE_BYTES to expectedSizeBytes,
                FIRMWARE_DOWNLOAD_VERIFYING to verifying,
            ),
        )
        setForeground(createForegroundInfo(downloadedBytes, expectedSizeBytes, verifying))
    }

    private fun showCompletedNotification() {
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(applicationContext.getString(R.string.firmware_download_completed))
            .setContentIntent(createContentIntent())
            .setAutoCancel(true)
            .build()
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun createForegroundInfo(downloadedBytes: Long, expectedSizeBytes: Long, verifying: Boolean): ForegroundInfo {
        val percentage =
            ((downloadedBytes.toDouble() / expectedSizeBytes) * 100).toInt().coerceIn(0, 100)
        val contentText = applicationContext.getString(
            R.string.firmware_download_notification_progress,
            percentage,
        )
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle(
                applicationContext.getString(
                    if (verifying) R.string.firmware_download_verifying else R.string.firmware_downloading,
                ),
            )
            .setContentText(contentText)
            .setContentIntent(createContentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percentage, verifying)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                applicationContext.getString(R.string.download_cancel),
                cancelIntent,
            )
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createContentIntent(): PendingIntent = PendingIntent.getActivity(
        applicationContext,
        0,
        Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createNotificationChannel() {
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                applicationContext.getString(R.string.firmware_download_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private companion object {
        const val NOTIFICATION_CHANNEL_ID = "firmware_download"
        const val NOTIFICATION_ID = 4101
        const val COMPLETION_NOTIFICATION_ID = 4102
    }
}
