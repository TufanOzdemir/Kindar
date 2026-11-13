package com.tufanozdemir.kindar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tufanozdemir.kindar.domain.Tone

private val Green = Color(0xFF1F5C4A)
private val GreenLight = Color(0xFF8FD3B8)
private val Amber = Color(0xFFE8A33D)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9EBDD),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF8A5A12),
    secondaryContainer = Color(0xFFFFDDB3),
    tertiary = Amber,
    background = Color(0xFFFBFAF6),
    surface = Color(0xFFFBFAF6),
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF0E4636),
    onPrimaryContainer = Color(0xFFC9EBDD),
    secondary = Color(0xFFFFB955),
    secondaryContainer = Color(0xFF5A3D00),
    tertiary = Amber,
)

/** Alacak (pozitif) ve borç (negatif) renkleri. */
object LedgerColors {
    val credit = Color(0xFF2E7D5B)
    val debt = Color(0xFFC0503A)
    val neutral = Color(0xFF7A7A7A)

    fun forNet(net: Int): Color = when {
        net > 0 -> credit
        net < 0 -> debt
        else -> neutral
    }

    fun forTone(tone: Tone): Color = when (tone) {
        Tone.POSITIVE -> credit
        Tone.NEUTRAL -> Color(0xFF5F6F8A)
        Tone.ACTION -> Color(0xFFB7791F)
        Tone.WARNING -> debt
    }
}

@Composable
fun KindarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
