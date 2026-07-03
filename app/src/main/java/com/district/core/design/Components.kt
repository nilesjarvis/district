package com.district.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonoPanel(
    modifier: Modifier = Modifier,
    color: Color = MonoTokens.Panel,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(color)
            .border(1.dp, MonoTokens.Line),
    ) {
        content()
    }
}

@Composable
fun MonoPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ShellMetrics.MinTouchTarget)
            .background(if (enabled) MonoTokens.Ink else MonoTokens.Panel2)
            .border(1.dp, if (enabled) MonoTokens.Ink else MonoTokens.Line2)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        UpperLabel(
            text = text,
            color = if (enabled) MonoTokens.Bg else MonoTokens.Mut2,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun MonoLedgerRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(MonoTokens.Panel)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UpperLabel(label, color = MonoTokens.Mut2, fontSize = 8.sp)
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = MonoTokens.Ink,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MonoStatusDot(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MonoTokens.Ok,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color),
        )
        UpperLabel(text, color = color, fontSize = 8.sp)
    }
}

@Composable
fun MonoAlbumTile(
    code: String,
    title: String,
    artist: String,
    coverColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .background(MonoTokens.Panel)
            .clipToBounds()
            .clickable(onClick = onClick),
    ) {
        CoverPlaceholder(
            color = coverColor,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            UpperLabel(code, color = MonoTokens.Mut2, fontSize = 8.sp)
            Text(
                text = title,
                color = MonoTokens.Ink,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
            )
            Text(
                text = subtitle ?: artist,
                color = MonoTokens.Mut,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
fun MonoTrackRow(
    number: String,
    title: String,
    duration: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    tintColor: Color = MonoTokens.Tint,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ShellMetrics.MinTouchTarget)
            .background(if (isPlaying) coverTint(tintColor, alpha = 0.16f) else MonoTokens.Bg)
            .border(1.dp, MonoTokens.Line)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UpperLabel(number, color = if (isPlaying) MonoTokens.Accent else MonoTokens.Mut2, fontSize = 8.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MonoTokens.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp,
            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
        )
        UpperLabel(duration, color = MonoTokens.Mut, fontSize = 8.sp)
    }
}

@Composable
fun MonoNowPlayingBar(
    title: String,
    artist: String,
    code: String,
    elapsed: String,
    duration: String,
    coverColor: Color,
    tintColor: Color = coverColor,
    isPlaying: Boolean = false,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
    cover: (@Composable () -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(coverTint(tintColor, alpha = 0.22f))
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (onTitleClick != null) {
                        Modifier
                            .testTag("now-playing-album-link")
                            .clickable(onClick = onTitleClick)
                    } else {
                        Modifier
                    },
                )
                .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(coverColor)
                    .border(1.dp, MonoTokens.Line2),
            ) {
                cover?.invoke()
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                UpperLabel("$code - NOW", color = if (isError) MonoTokens.Accent else MonoTokens.Mut2, fontSize = 8.sp)
                Text(
                    text = title,
                    color = MonoTokens.Ink,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                )
                Text(
                    text = artist,
                    color = if (isError) MonoTokens.Accent else MonoTokens.Mut,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            UpperLabel(elapsed, color = if (isError) MonoTokens.Accent else MonoTokens.Mut, fontSize = 8.sp)
            UpperLabel(duration, color = if (isError) MonoTokens.Accent else MonoTokens.Mut2, fontSize = 8.sp)
        }
        if (onActionClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .defaultMinSize(minWidth = ShellMetrics.MinTouchTarget)
                    .testTag("now-playing-toggle")
                    .clickable(onClick = onActionClick),
                contentAlignment = Alignment.Center,
            ) {
                PlayPauseGlyph(isPlaying = isPlaying, color = MonoTokens.Ink)
            }
        } else {
            Spacer(Modifier.width(10.dp))
        }
    }
}

@Composable
private fun PlayPauseGlyph(isPlaying: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(15.dp)) {
        if (isPlaying) {
            val barWidth = size.width * 0.28f
            drawRect(color = color, topLeft = Offset(size.width * 0.16f, 0f), size = Size(barWidth, size.height))
            drawRect(color = color, topLeft = Offset(size.width * 0.56f, 0f), size = Size(barWidth, size.height))
        } else {
            val path = Path().apply {
                moveTo(size.width * 0.2f, 0f)
                lineTo(size.width * 0.2f, size.height)
                lineTo(size.width * 0.92f, size.height / 2f)
                close()
            }
            drawPath(path, color)
        }
    }
}

@Composable
fun MonoScrubPreview(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .defaultMinSize(minHeight = ShellMetrics.MinTouchTarget)
            .background(MonoTokens.Line)
            .padding(1.dp),
    ) {
        Row(Modifier.fillMaxSize()) {
            repeat(24) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (index % 4 == 0) MonoTokens.Line2 else MonoTokens.Panel2),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(maxWidth * progress.coerceIn(0f, 1f))
                .background(MonoTokens.Accent.copy(alpha = 0.22f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = (maxWidth * progress.coerceIn(0f, 1f) - 1.dp).coerceAtLeast(0.dp))
                .width(2.dp)
                .fillMaxHeight()
                .background(MonoTokens.Ink),
        )
    }
}

@Composable
fun MonoVolumeBar(
    volume: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ShellMetrics.MinTouchTarget)
            .background(MonoTokens.Panel)
            .padding(vertical = 18.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MonoTokens.Line),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(volume.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MonoTokens.Ink),
        )
    }
}

@Composable
fun CoverPlaceholder(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(MonoTokens.Panel)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(color = color)
            val step = 14f
            var x = -size.height
            while (x < size.width) {
                drawLine(
                    color = MonoTokens.Line.copy(alpha = 0.45f),
                    start = androidx.compose.ui.geometry.Offset(x, size.height),
                    end = androidx.compose.ui.geometry.Offset(x + size.height, 0f),
                    strokeWidth = 1f,
                )
                x += step
            }
        }
    }
}

fun coverTint(color: Color, alpha: Float = 0.14f): Color =
    color.copy(alpha = alpha)
