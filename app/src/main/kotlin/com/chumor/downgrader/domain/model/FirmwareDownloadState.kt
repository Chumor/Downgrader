package com.chumor.downgrader.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class FirmwareDownloadState(
    val status: FirmwareDownloadStatus = FirmwareDownloadStatus.Idle,
    val sha256: String? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
)

enum class FirmwareDownloadStatus {
    Idle,
    Preparing,
    Downloading,
    Verifying,
    Completed,
    Failed,
    Cancelled;

    val isActive: Boolean
        get() = this == Preparing || this == Downloading || this == Verifying
}
