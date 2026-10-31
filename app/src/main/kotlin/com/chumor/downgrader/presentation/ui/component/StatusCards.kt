package com.chumor.downgrader.presentation.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chumor.downgrader.R
import com.chumor.downgrader.domain.model.DeviceReadError
import com.chumor.downgrader.domain.model.QueryError
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DownloadInstructionCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenSystemUpdater: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.primaryContainer,
        ),
    ) {
        BasicComponent(
            title = androidx.compose.ui.res.stringResource(R.string.download_instructions_title),
            titleColor = BasicComponentDefaults.titleColor(
                color = MiuixTheme.colorScheme.onPrimaryContainer,
            ),
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Info,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onPrimaryContainer,
                )
            },
            endActions = {
                Icon(
                    imageVector = if (expanded) MiuixIcons.ExpandLess else MiuixIcons.ExpandMore,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onPrimaryContainer,
                )
            },
            onClick = onToggle,
            onClickLabel = androidx.compose.ui.res.stringResource(
                if (expanded) R.string.collapse_download_instructions else R.string.expand_download_instructions,
            ),
            role = Role.Button,
        )
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.download_instructions),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onPrimaryContainer,
                )
                if (onOpenSystemUpdater != null) {
                    TextButton(
                        text = androidx.compose.ui.res.stringResource(R.string.open_system_updater),
                        onClick = onOpenSystemUpdater,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun DeviceLoadingCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = 12.dp),
                size = 18.dp,
                strokeWidth = 2.dp,
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.device_reading),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
internal fun CredentialStatusCard(
    credentialAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (credentialAvailable) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        BasicComponent(
            title = if (credentialAvailable) {
                androidx.compose.ui.res.stringResource(R.string.credential_ready)
            } else {
                androidx.compose.ui.res.stringResource(R.string.credential_missing)
            },
            titleColor = BasicComponentDefaults.titleColor(color = color),
            summary = if (credentialAvailable) {
                null
            } else {
                androidx.compose.ui.res.stringResource(R.string.credential_missing_note)
            },
        )
    }
}

@Composable
internal fun ErrorCard(title: String, message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        BasicComponent(
            title = title,
            titleColor = BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.error),
            summary = message,
            summaryColor = BasicComponentDefaults.summaryColor(color = MiuixTheme.colorScheme.error),
        )
    }
}

@Composable
internal fun deviceErrorMessage(error: DeviceReadError): String = when (error) {
    DeviceReadError.Unavailable ->
        androidx.compose.ui.res.stringResource(R.string.device_error_unavailable)

    is DeviceReadError.Failed ->
        androidx.compose.ui.res.stringResource(R.string.device_error_exit, error.exitCode)

    DeviceReadError.Interrupted ->
        androidx.compose.ui.res.stringResource(R.string.device_error_interrupted)
}

@Composable
internal fun queryErrorMessage(error: QueryError): String = when (error) {
    QueryError.Network ->
        androidx.compose.ui.res.stringResource(R.string.query_error_network)

    is QueryError.HttpStatus ->
        androidx.compose.ui.res.stringResource(R.string.query_error_http, error.code)

    is QueryError.ServerRejected ->
        if (error.serverMessage.isBlank()) {
            androidx.compose.ui.res.stringResource(R.string.query_error_rejected, error.code)
        } else {
            androidx.compose.ui.res.stringResource(
                R.string.query_error_rejected_detail,
                error.code,
                error.serverMessage,
            )
        }

    QueryError.InvalidResponse ->
        androidx.compose.ui.res.stringResource(R.string.query_error_invalid_response)

    QueryError.Unexpected ->
        androidx.compose.ui.res.stringResource(R.string.query_error_unexpected)
}
