package dev.infinitetokens.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Ink = Color(0xFF101010)
internal val SurfaceInk = Color(0xFF191919)
internal val RaisedInk = Color(0xFF242424)
internal val Warm = Color(0xFFE68568)
internal val Paper = Color(0xFFF4F1EE)
internal val Muted = Color(0xFFA09C99)
internal val BlueCheck = Color(0xFF8CB7E8)

@Composable
fun InfiniteTokensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Warm,
            onPrimary = Ink,
            secondary = BlueCheck,
            background = Ink,
            onBackground = Paper,
            surface = SurfaceInk,
            onSurface = Paper,
            surfaceVariant = RaisedInk,
            onSurfaceVariant = Muted,
            outline = Color(0xFF41403E),
        ),
        content = content,
    )
}
