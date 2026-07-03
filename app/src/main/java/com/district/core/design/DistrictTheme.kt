package com.district.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val DistrictColorScheme = darkColorScheme(
    background = MonoTokens.Bg,
    surface = MonoTokens.Panel,
    surfaceVariant = MonoTokens.Panel2,
    primary = MonoTokens.Ink,
    secondary = MonoTokens.Mut,
    tertiary = MonoTokens.Accent,
    outline = MonoTokens.Line,
    onBackground = MonoTokens.Ink,
    onSurface = MonoTokens.Ink,
    onPrimary = MonoTokens.Bg,
)

@Composable
fun DistrictTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DistrictColorScheme,
        typography = MaterialTheme.typography.copy(
            bodyLarge = monoStyle(14),
            bodyMedium = monoStyle(12),
            bodySmall = monoStyle(10),
            labelLarge = monoStyle(12),
            labelMedium = monoStyle(10),
            labelSmall = monoStyle(9),
            titleLarge = monoStyle(22),
            titleMedium = monoStyle(18),
            titleSmall = monoStyle(14),
        ),
        content = content,
    )
}

private fun monoStyle(sizeSp: Int): TextStyle =
    TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = sizeSp.sp,
    )
