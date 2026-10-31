package com.chumor.downgrader.presentation.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chumor.downgrader.R
import com.chumor.downgrader.presentation.DowngraderUiState
import com.chumor.downgrader.presentation.ui.component.AboutAction
import com.chumor.downgrader.presentation.ui.component.AboutDialog
import com.chumor.downgrader.presentation.ui.component.BlurredBar
import com.chumor.downgrader.presentation.ui.component.CardItem
import com.chumor.downgrader.presentation.ui.component.CredentialStatusCard
import com.chumor.downgrader.presentation.ui.component.DeviceLoadingCard
import com.chumor.downgrader.presentation.ui.component.DownloadInstructionCard
import com.chumor.downgrader.presentation.ui.component.ErrorCard
import com.chumor.downgrader.presentation.ui.component.FirmwareDownloadRow
import com.chumor.downgrader.presentation.ui.component.deviceErrorMessage
import com.chumor.downgrader.presentation.ui.component.groupedCardItems
import com.chumor.downgrader.presentation.ui.component.queryErrorMessage
import com.chumor.downgrader.presentation.ui.component.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private val WideScreenMinWidth = 600.dp
private val WideContentMaxWidth = 800.dp

@Composable
fun DowngraderScreen(
    state: DowngraderUiState,
    onQueryPackage: () -> Unit,
    onSaveCertificate: () -> Unit,
    onDownloadFirmware: () -> Unit,
    onCancelFirmwareDownload: () -> Unit,
    onCopyFirmwareUrl: (String) -> Unit,
    onCopyCertificate: (String) -> Unit,
    onOpenSystemUpdater: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var instructionsExpanded by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= WideScreenMinWidth
        val layoutDirection = LocalLayoutDirection.current
        val horizontalInsets = WindowInsets.displayCutout
            .union(WindowInsets.navigationBars)
            .only(WindowInsetsSides.Horizontal)
        val horizontalInsetPadding = horizontalInsets.asPaddingValues()
        val safeWidth = maxWidth - horizontalInsetPadding.calculateStartPadding(layoutDirection) -
            horizontalInsetPadding.calculateEndPadding(layoutDirection)
        val wideContentPadding = ((safeWidth - WideContentMaxWidth) / 2).coerceAtLeast(0.dp)
        val canCollapseTopBar = remember(isWideScreen) { { !isWideScreen } }
        val scrollBehavior = MiuixScrollBehavior(
            state = rememberTopAppBarState(),
            canScroll = canCollapseTopBar,
        )
        val backdrop = rememberBlurBackdrop()
        val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                BlurredBar(backdrop = backdrop) {
                    val title = androidx.compose.ui.res.stringResource(R.string.app_name)
                    val navigationIcon: @Composable () -> Unit = {
                        AboutAction(
                            showDialog = showAboutDialog,
                            onClick = { showAboutDialog = true },
                        )
                    }
                    if (isWideScreen) {
                        SmallTopAppBar(
                            title = title,
                            color = barColor,
                            scrollBehavior = scrollBehavior,
                            navigationIcon = navigationIcon,
                        )
                    } else {
                        TopAppBar(
                            title = title,
                            color = barColor,
                            scrollBehavior = scrollBehavior,
                            navigationIcon = navigationIcon,
                        )
                    }
                }
            },
        ) { contentPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(horizontalInsets)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(
                        if (backdrop != null) {
                            Modifier.layerBackdrop(backdrop)
                        } else {
                            Modifier
                        },
                    ),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    start = wideContentPadding,
                    end = wideContentPadding,
                ),
                overscrollEffect = null,
            ) {
                item(key = "device.section", contentType = "sectionTitle") {
                    SmallTitle(
                        text = androidx.compose.ui.res.stringResource(R.string.device_section_title),
                        modifier = Modifier.semantics { heading() },
                    )
                }

                if (state.isLoadingDevice && state.device == null) {
                    item(key = "device.loading", contentType = "loadingCard") {
                        DeviceLoadingCard()
                    }
                }

                state.device?.let { device ->
                    deviceInfoCards(device)
                }

                state.deviceError?.let { error ->
                    item(key = "device.error", contentType = "errorCard") {
                        ErrorCard(
                            title = androidx.compose.ui.res.stringResource(R.string.device_section_title),
                            message = deviceErrorMessage(error),
                        )
                    }
                }

                if (state.credentialAvailable || (!state.isLoadingDevice && state.deviceError == null)) {
                    item(key = "credential.status", contentType = "credentialCard") {
                        CredentialStatusCard(
                            credentialAvailable = state.credentialAvailable,
                        )
                    }
                }

                item(key = "query.action", contentType = "queryButton") {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        enabled = state.canQueryPackage,
                        onClick = onQueryPackage,
                    ) {
                        if (state.isQuerying) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 10.dp),
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = LocalContentColor.current,
                                ),
                                size = 18.dp,
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                if (state.isQuerying) R.string.query_in_progress else R.string.query_action,
                            ),
                            style = MiuixTheme.textStyles.button,
                        )
                    }
                }

                state.queryError?.let { error ->
                    item(key = "query.error", contentType = "errorCard") {
                        ErrorCard(
                            title = androidx.compose.ui.res.stringResource(R.string.query_error_title),
                            message = queryErrorMessage(error),
                        )
                    }
                }

                firmwareSection(
                    state = state,
                    onSaveCertificate = onSaveCertificate,
                    onDownloadFirmware = onDownloadFirmware,
                    onCancelFirmwareDownload = onCancelFirmwareDownload,
                    onCopyFirmwareUrl = onCopyFirmwareUrl,
                    onCopyCertificate = onCopyCertificate,
                )

                if (state.isDownloadingFirmware && state.firmware == null) {
                    groupedCardItems(
                        keyPrefix = "firmware.download",
                        items = listOf(
                            CardItem("progress", contentType = "downloadRow") {
                                FirmwareDownloadRow(
                                    title = androidx.compose.ui.res.stringResource(R.string.download_firmware),
                                    status = state.firmwareDownload.status,
                                    downloadedBytes = state.firmwareDownload.downloadedBytes,
                                    totalBytes = state.firmwareDownload.totalBytes,
                                    downloadEnabled = false,
                                    onCopy = null,
                                    onDownload = onDownloadFirmware,
                                    onCancel = onCancelFirmwareDownload,
                                )
                            },
                        ),
                    )
                }

                if (state.firmware != null) {
                    item(key = "firmware.download.instructions", contentType = "instructionCard") {
                        DownloadInstructionCard(
                            expanded = instructionsExpanded,
                            onToggle = { instructionsExpanded = !instructionsExpanded },
                            onOpenSystemUpdater = onOpenSystemUpdater,
                        )
                    }
                }

                item(key = "screen.bottom", contentType = "spacer") {
                    Spacer(
                        modifier = Modifier
                            .height(24.dp)
                            .navigationBarsPadding(),
                    )
                }
            }
        }
    }

    AboutDialog(
        show = showAboutDialog,
        onDismissRequest = { showAboutDialog = false },
    )
}
