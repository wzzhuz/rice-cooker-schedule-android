package com.wzzhuz.ricecooker.schedule.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class AppPalette(
    val bg: Color,
    val ink: Color,
    val sub: Color,
    val line: Color,
    val brand: Color,
    val danger: Color,
)

private val Light = AppPalette(
    bg = Color(0xFFFAF7F2),
    ink = Color(0xFF2A2520),
    sub = Color(0xFF9A9088),
    line = Color(0xFFEDE6DC),
    brand = Color(0xFFC4703A),
    danger = Color(0xFFC4553B),
)

private val Dark = AppPalette(
    bg = Color(0xFF171512),
    ink = Color(0xFFF2EBE1),
    sub = Color(0xFF8A8075),
    line = Color(0xFF302B25),
    brand = Color(0xFFE29562),
    danger = Color(0xFFE0705A),
)

fun palette(night: Boolean): AppPalette = if (night) Dark else Light

/** 20:00–07:00 自动深色，不跟随系统，也不提供主题按钮。 */
@Composable
fun RiceCookerTheme(night: Boolean, content: @Composable () -> Unit) {
    val p = palette(night)
    val scheme = if (night) {
        darkColorScheme(
            background = p.bg, surface = p.bg, onBackground = p.ink, onSurface = p.ink,
            primary = p.brand, outline = p.line, onSurfaceVariant = p.sub,
        )
    } else {
        lightColorScheme(
            background = p.bg, surface = p.bg, onBackground = p.ink, onSurface = p.ink,
            primary = p.brand, outline = p.line, onSurfaceVariant = p.sub,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
