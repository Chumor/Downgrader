package com.chumor.downgrader.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
internal data class CardItem(
    val key: String,
    val contentType: String = "infoRow",
    val content: @Composable ColumnScope.() -> Unit,
)

internal fun LazyListScope.groupedCardItems(
    keyPrefix: String,
    items: List<CardItem>,
) {
    items.forEachIndexed { index, cardItem ->
        val position = when {
            items.size == 1 -> CardSegmentPosition.Only
            index == 0 -> CardSegmentPosition.First
            index == items.lastIndex -> CardSegmentPosition.Last
            else -> CardSegmentPosition.Middle
        }
        item(key = keyPrefix + "." + cardItem.key, contentType = cardItem.contentType) {
            CardSegment(
                position = position,
                content = cardItem.content,
            )
        }
    }
}

private enum class CardSegmentPosition {
    Only,
    First,
    Middle,
    Last,
}

@Composable
private fun CardSegment(
    position: CardSegmentPosition,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surfaceContainer
    val isLast = position == CardSegmentPosition.Only || position == CardSegmentPosition.Last
    val shapeModifier = when (position) {
        CardSegmentPosition.Only -> Modifier.squircleSurface(
            color = surfaceColor,
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp,
        )

        CardSegmentPosition.First -> Modifier.squircleSurface(
            color = surfaceColor,
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 0.dp,
            bottomStart = 0.dp,
        )

        CardSegmentPosition.Middle -> Modifier.background(surfaceColor)

        CardSegmentPosition.Last -> Modifier.squircleSurface(
            color = surfaceColor,
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = if (isLast) 12.dp else 0.dp)
            .then(shapeModifier),
    ) {
        content()
    }
}
