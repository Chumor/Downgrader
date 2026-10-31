package com.chumor.downgrader.presentation.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.chumor.downgrader.R
import com.chumor.downgrader.domain.model.DeviceInfo
import com.chumor.downgrader.domain.model.FirmwareDownloadStatus
import com.chumor.downgrader.domain.model.FirmwarePackage
import com.chumor.downgrader.domain.model.downloadFileName
import com.chumor.downgrader.domain.model.isSha256Digest
import com.chumor.downgrader.presentation.DowngraderUiState
import com.chumor.downgrader.presentation.ui.component.ActionInfoRow
import com.chumor.downgrader.presentation.ui.component.CardItem
import com.chumor.downgrader.presentation.ui.component.FirmwareDownloadRow
import com.chumor.downgrader.presentation.ui.component.InfoRow
import com.chumor.downgrader.presentation.ui.component.formatFileSize
import com.chumor.downgrader.presentation.ui.component.groupedCardItems
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Download

internal fun LazyListScope.firmwareSection(
    state: DowngraderUiState,
    onSaveCertificate: () -> Unit,
    onDownloadFirmware: () -> Unit,
    onCancelFirmwareDownload: () -> Unit,
    onCopyFirmwareUrl: (String) -> Unit,
    onCopyCertificate: (String) -> Unit,
) {
    state.firmware?.let { firmware ->
        val actionRows = buildList {
            if (firmware.downloadUrl.isNotBlank()) {
                add(
                    CardItem("downloadUrl", contentType = "downloadRow") {
                        FirmwareDownloadRow(
                            title = androidx.compose.ui.res.stringResource(R.string.firmware_download_url),
                            status = if (firmware.sha256.equals(state.firmwareDownload.sha256, ignoreCase = true)) {
                                state.firmwareDownload.status
                            } else {
                                FirmwareDownloadStatus.Idle
                            },
                            downloadedBytes = state.firmwareDownload.downloadedBytes,
                            totalBytes = firmware.fileSizeBytes,
                            downloadEnabled = state.canDownloadFirmware,
                            onCopy = { onCopyFirmwareUrl(firmware.downloadUrl) },
                            onDownload = onDownloadFirmware,
                            onCancel = onCancelFirmwareDownload,
                        )
                    },
                )
            }
            if (
                firmware.downloadUrl.isNotBlank() &&
                (firmware.fileSizeBytes <= 0L || !firmware.sha256.isSha256Digest())
            ) {
                add(
                    CardItem("downloadIntegrityUnavailable", contentType = "messageRow") {
                        BasicComponent(
                            title = androidx.compose.ui.res.stringResource(R.string.download_firmware),
                            summary = androidx.compose.ui.res.stringResource(
                                R.string.firmware_download_integrity_required,
                            ),
                        )
                    },
                )
            }
            add(
                CardItem("certificate", contentType = "actionRow") {
                    ActionInfoRow(
                        title = androidx.compose.ui.res.stringResource(R.string.certificate_section_title),
                        firstIcon = MiuixIcons.Copy,
                        firstContentDescription = androidx.compose.ui.res.stringResource(
                            R.string.certificate_copy,
                        ),
                        firstEnabled = firmware.certificate.isNotEmpty(),
                        onFirstClick = { onCopyCertificate(firmware.certificate) },
                        secondIcon = MiuixIcons.Download,
                        secondContentDescription = androidx.compose.ui.res.stringResource(
                            if (state.isCertificateSaved) R.string.certificate_save_done else R.string.certificate_save,
                        ),
                        secondEnabled = state.canSaveCertificate,
                        onSecondClick = onSaveCertificate,
                        secondCompleted = state.isCertificateSaved,
                    )
                },
            )
        }
        firmwareInfoCards(firmware, state.device?.model, actionRows)
    }
}

internal fun LazyListScope.deviceInfoCards(device: DeviceInfo) {
    val rows = buildList<CardItem> {
        device.productName?.takeIf(String::isNotBlank)?.let { productName ->
            add(CardItem("productName") { InfoRow(R.string.device_name, productName) })
        }
        add(CardItem("model") { InfoRow(R.string.device_model, device.model.asDisplayValue()) })
        add(CardItem("productVersion") {
            InfoRow(R.string.device_product_version, device.softwareVersion.asDisplayValue())
        })
        add(CardItem("androidVersion") {
            InfoRow(R.string.device_android_version, device.androidVersion.asDisplayValue())
        })
        add(CardItem("serial") { InfoRow(R.string.device_serial, device.serial.asMaskedSerial()) })
    }
    groupedCardItems(keyPrefix = "device", items = rows)
}

private fun LazyListScope.firmwareInfoCards(
    firmware: FirmwarePackage,
    deviceModel: String?,
    actionRows: List<CardItem>,
) {
    item(key = "firmware.section", contentType = "sectionTitle") {
        SmallTitle(
            text = androidx.compose.ui.res.stringResource(R.string.firmware_section_title),
            modifier = Modifier.semantics { heading() },
        )
    }

    val rows = buildList<CardItem> {
        fun addIfPresent(key: String, @StringRes labelRes: Int, value: String) {
            if (value.isNotBlank()) {
                add(CardItem(key) { InfoRow(labelRes, value) })
            }
        }

        addIfPresent("systemVersion", R.string.firmware_system_version, firmware.systemVersion)
        addIfPresent("androidVersion", R.string.firmware_android_version, firmware.androidVersion)
        deviceModel?.takeIf(String::isNotBlank)?.let { model ->
            if (firmware.systemVersion.isNotBlank()) {
                add(
                    CardItem("downloadFileName") {
                        InfoRow(R.string.firmware_file_name, firmware.downloadFileName(model))
                    },
                )
            }
        }
        addIfPresent("romVersion", R.string.firmware_rom_version, firmware.romVersion)
        addIfPresent("sha256", R.string.firmware_sha256, firmware.sha256)
        if (firmware.fileSizeBytes > 0) {
            add(
                CardItem("fileSize") {
                    InfoRow(R.string.firmware_file_size, formatFileSize(firmware.fileSizeBytes))
                },
            )
        }
        addIfPresent("releaseTime", R.string.firmware_release_time, firmware.releaseTime)
        addAll(actionRows)
    }
    groupedCardItems(keyPrefix = "firmware", items = rows)
}

@Composable
private fun String?.asDisplayValue(): String =
    this?.takeIf(String::isNotBlank)
        ?: androidx.compose.ui.res.stringResource(R.string.value_unknown)

@Composable
private fun String?.asMaskedSerial(): String {
    val serial = this?.takeIf(String::isNotBlank)
        ?: return androidx.compose.ui.res.stringResource(R.string.value_unknown)
    return if (serial.length <= 4) {
        "••••"
    } else {
        "•".repeat(serial.length - 4) + serial.takeLast(4)
    }
}
