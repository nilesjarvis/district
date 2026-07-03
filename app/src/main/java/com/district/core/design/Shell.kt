package com.district.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ShellMetrics {
    val HeaderHeight: Dp = 46.dp
    val ContextBarHeight: Dp = 52.dp
    val NowPlayingHeight: Dp = 56.dp
    val ControlZoneHeight: Dp = 248.dp
    val MinTouchTarget: Dp = 44.dp
}

@Composable
fun MonoShell(
    header: @Composable () -> Unit,
    contextualBar: (@Composable () -> Unit)?,
    scrollRegion: @Composable () -> Unit,
    nowPlaying: @Composable () -> Unit,
    controlZone: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    controlZoneHeight: Dp = ShellMetrics.ControlZoneHeight,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MonoTokens.Bg)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ShellMetrics.HeaderHeight)
                .background(MonoTokens.Bg)
                .border(1.dp, MonoTokens.Line),
        ) {
            header()
        }
        if (contextualBar != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ShellMetrics.ContextBarHeight)
                    .background(MonoTokens.Panel)
                    .border(1.dp, MonoTokens.Line),
            ) {
                contextualBar()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MonoTokens.Bg),
        ) {
            scrollRegion()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ShellMetrics.NowPlayingHeight)
                .border(1.dp, MonoTokens.Line),
        ) {
            nowPlaying()
        }
        // The bottom bar extends its own color behind the system gesture bar so there is no
        // color seam at the very bottom edge. navigationBarsPadding keeps the interactive
        // content above the pill while the background fills all the way down.
        if (controlZoneHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MonoTokens.Panel)
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(controlZoneHeight)
                        .defaultMinSize(minHeight = ShellMetrics.MinTouchTarget)
                        .background(MonoTokens.Panel),
                ) {
                    controlZone()
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MonoTokens.Bg)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
fun UpperLabel(
    text: String,
    color: androidx.compose.ui.graphics.Color = MonoTokens.Mut,
    fontSize: TextUnit = 9.sp,
    fontWeight: FontWeight = FontWeight.Medium,
) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = 1.1.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun MonoButton(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .defaultMinSize(minHeight = ShellMetrics.MinTouchTarget)
            .background(if (active) MonoTokens.Ink else MonoTokens.Bg)
            .border(1.dp, MonoTokens.Line)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        UpperLabel(
            text = text,
            color = if (active) MonoTokens.Bg else MonoTokens.Ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
