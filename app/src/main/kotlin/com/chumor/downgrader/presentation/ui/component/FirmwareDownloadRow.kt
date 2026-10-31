package com.chumor.downgrader.presentation.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chumor.downgrader.R
import com.chumor.downgrader.domain.model.FirmwareDownloadStatus
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FirmwareDownloadRow(
    title: String,
    status: FirmwareDownloadStatus,
    downloadedBytes: Long,
    totalBytes: Long,
    downloadEnabled: Boolean,
    onCopy: (() -> Unit)?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (status == FirmwareDownloadStatus.Downloading && totalBytes > 0L) {
        (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    } else {
        null
    }
    val labelRes = when (status) {
        FirmwareDownloadStatus.Idle, FirmwareDownloadStatus.Cancelled -> R.string.download_firmware
        FirmwareDownloadStatus.Preparing -> R.string.firmware_download_preparing
        FirmwareDownloadStatus.Downloading -> R.string.firmware_download_running
        FirmwareDownloadStatus.Verifying -> R.string.firmware_download_verifying
        FirmwareDownloadStatus.Completed -> R.string.firmware_download_done
        FirmwareDownloadStatus.Failed -> R.string.download_retry
    }
    val summary = when (status) {
        FirmwareDownloadStatus.Downloading -> progress?.let {
            androidx.compose.ui.res.stringResource(
                R.string.firmware_download_size,
                formatFileSize(downloadedBytes),
                formatFileSize(totalBytes),
            )
        }
        FirmwareDownloadStatus.Failed -> androidx.compose.ui.res.stringResource(R.string.firmware_download_failed)
        FirmwareDownloadStatus.Cancelled -> androidx.compose.ui.res.stringResource(R.string.firmware_download_cancelled)
        FirmwareDownloadStatus.Idle, FirmwareDownloadStatus.Preparing,
        FirmwareDownloadStatus.Verifying, FirmwareDownloadStatus.Completed ->
            totalBytes.takeIf { it > 0L }?.let { formatFileSize(it) }
    }
    BasicComponent(
        modifier = modifier,
        title = title,
        summary = summary,
        endActions = {
            if (onCopy != null) {
                ActionIconButton(
                    imageVector = MiuixIcons.Copy,
                    contentDescription = androidx.compose.ui.res.stringResource(R.string.firmware_url_copy),
                    onClick = onCopy,
                )
                Spacer(Modifier.width(12.dp))
            }
            if (status == FirmwareDownloadStatus.Completed) {
                CompletedActionIcon(
                    contentDescription = androidx.compose.ui.res.stringResource(labelRes),
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            } else {
                ActionIconButton(
                    imageVector = if (status.isActive) MiuixIcons.Close else MiuixIcons.Download,
                    contentDescription = androidx.compose.ui.res.stringResource(
                        if (status.isActive) R.string.download_cancel else labelRes,
                    ),
                    onClick = if (status.isActive) onCancel else onDownload,
                    enabled = status.isActive || downloadEnabled,
                )
            }
        },
        bottomAction = if (status.isActive) {
            {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(labelRes),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSecondaryVariant,
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        progress = progress,
                    )
                    if (progress != null) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                R.string.firmware_download_percentage,
                                (progress * 100).toInt(),
                            ),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                }
            }
        } else {
            null
        },
    )
}
