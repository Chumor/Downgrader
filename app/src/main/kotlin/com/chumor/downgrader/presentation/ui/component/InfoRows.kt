package com.chumor.downgrader.presentation.ui.component

import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun InfoRow(@StringRes labelRes: Int, value: String, modifier: Modifier = Modifier) {
    BasicComponent(modifier = modifier) {
        Text(
            text = androidx.compose.ui.res.stringResource(labelRes),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        SelectionContainer {
            Text(
                text = value,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun ActionInfoRow(
    title: String,
    firstIcon: ImageVector,
    firstContentDescription: String,
    firstEnabled: Boolean,
    onFirstClick: () -> Unit,
    secondIcon: ImageVector,
    secondContentDescription: String,
    secondEnabled: Boolean,
    onSecondClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondCompleted: Boolean = false,
) {
    BasicComponent(
        modifier = modifier,
        title = title,
        endActions = {
            ActionIconButton(
                imageVector = firstIcon,
                contentDescription = firstContentDescription,
                enabled = firstEnabled,
                onClick = onFirstClick,
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (secondCompleted) {
                CompletedActionIcon(contentDescription = secondContentDescription)
            } else {
                ActionIconButton(
                    imageVector = secondIcon,
                    contentDescription = secondContentDescription,
                    enabled = secondEnabled,
                    onClick = onSecondClick,
                )
            }
        },
    )
}

@Composable
internal fun CompletedActionIcon(contentDescription: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.defaultMinSize(minWidth = 35.dp, minHeight = 35.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MiuixIcons.Ok,
            contentDescription = contentDescription,
            tint = MiuixTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun ActionIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        modifier = modifier.semantics(mergeDescendants = true) {
            this.contentDescription = contentDescription
            role = Role.Button
            if (!enabled) disabled()
        },
        enabled = enabled,
        onClick = onClick,
        backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
        minHeight = 35.dp,
        minWidth = 35.dp,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = if (enabled) {
                MiuixTheme.colorScheme.onBackground
            } else {
                MiuixTheme.colorScheme.disabledOnSecondaryVariant
            },
        )
    }
}

@Composable
internal fun formatFileSize(bytes: Long): String =
    Formatter.formatShortFileSize(LocalContext.current, bytes)
