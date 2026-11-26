package com.novabrowse.browser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.novabrowse.browser.data.AppSettings

// Palette --------------------------------------------------------------------
// Chrome's material colour system: neutral grey surfaces, one blue accent and
// a secondary set of Google tonal colours for destructive / informational states.
val Midnight = Color(0xFF1F2023)
val DeepNavy = Color(0xFF282A2D)
val Navy = Color(0xFF35373B)
val NavySoft = Color(0xFF3F4246)
val ElectricBlue = Color(0xFF1A73E8)
val ChromeBlueDark = Color(0xFF8AB4F8)
val NovaCyan = Color(0xFF7FD3F7)
val NovaViolet = Color(0xFFC58AF9)
val SoftLavender = Color(0xFFD7BCF7)
val FrostWhite = Color(0xFFE8EAED)
val ChromeGrey = Color(0xFF5F6368)

fun accentColor(name: String): Color = when (name) {
    "cyan" -> NovaCyan
    "violet" -> NovaViolet
    "lavender" -> SoftLavender
    else -> ElectricBlue
}

val BrandBrush = Brush.linearGradient(listOf(NovaCyan, ElectricBlue, NovaViolet))
val AuroraBrush = Brush.linearGradient(listOf(ElectricBlue, NovaCyan))

data class NovaStyle(
    val dark: Boolean,
    val accent: Color,
    val glassIntensity: Float,
    val highlights: Boolean
)

val LocalNovaStyle = staticCompositionLocalOf {
    NovaStyle(dark = true, accent = ChromeBlueDark, glassIntensity = 1f, highlights = false)
}

/** Roboto-like sizing: compact, left aligned and high contrast. */
val NovaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 32.sp, letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 24.sp, letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 20.sp, letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, letterSpacing = 0.4.sp
    )
)

private val DarkScheme = darkColorScheme(
    primary = ChromeBlueDark,
    onPrimary = Color(0xFF202124),
    secondary = NovaCyan,
    tertiary = NovaViolet,
    background = Midnight,
    onBackground = FrostWhite,
    surface = DeepNavy,
    onSurface = FrostWhite,
    surfaceVariant = Navy,
    onSurfaceVariant = Color(0xFF9AA0A6),
    outline = ChromeGrey,
    outlineVariant = Color(0xFF3F4246),
    error = Color(0xFFF28B82)
)

private val LightScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    secondary = Color(0xFF1A73E8),
    tertiary = Color(0xFF8430CE),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF202124),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = ChromeGrey,
    outline = Color(0xFFC4C7C5),
    outlineVariant = Color(0xFFE3E3E3),
    error = Color(0xFFD93025)
)

data class WallpaperPreset(val id: String, val name: String, val colors: List<Color>)

object Wallpapers {
    /** "plain" follows the theme instead of drawing a picture. */
    val plain = WallpaperPreset("plain", "Plain", listOf(Midnight, Midnight))

    val presets = listOf(
        plain,
        WallpaperPreset("graphite", "Graphite", listOf(Color(0xFF1F2023), Color(0xFF2B2D31))),
        WallpaperPreset("midnight", "Midnight", listOf(Color(0xFF101114), Color(0xFF23262B))),
        WallpaperPreset("aurora", "Aurora", listOf(Color(0xFF0B1B2B), Color(0xFF123A56), Color(0xFF241E46))),
        WallpaperPreset("nebula", "Nebula", listOf(Color(0xFF14121F), Color(0xFF32204D))),
        WallpaperPreset("glacier", "Glacier", listOf(Color(0xFF0B1A22), Color(0xFF173F4C))),
        WallpaperPreset("violet", "Violet Dusk", listOf(Color(0xFF17111F), Color(0xFF3A2358))),
        WallpaperPreset("dawn", "Dawn", listOf(Color(0xFFF8F9FA), Color(0xFFE8F0FE))),
        WallpaperPreset("frost", "Frost", listOf(Color(0xFFF1F3F4), Color(0xFFE6F4F9)))
    )

    fun preset(id: String): WallpaperPreset = presets.firstOrNull { it.id == id } ?: plain

    fun brush(id: String): Brush {
        val preset = preset(id)
        return Brush.linearGradient(preset.colors)
    }

    fun isLight(id: String): Boolean = id == "dawn" || id == "frost"
}

@Composable
fun NovaTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) {
        DarkScheme.copy(primary = if (settings.accent == "electric") ChromeBlueDark else accentColor(settings.accent))
    } else {
        LightScheme.copy(primary = accentColor(settings.accent))
    }
    val style = NovaStyle(
        dark = dark,
        accent = scheme.primary,
        glassIntensity = settings.glassIntensity.coerceIn(0.4f, 1.4f),
        highlights = false
    )
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalNovaStyle provides style) {
        MaterialTheme(colorScheme = scheme, typography = NovaTypography, content = content)
    }
}

/** Chrome's small-radius material tokens. */
object NovaTokens {
    val cornerSmall = 8.dp
    val corner = 12.dp
    val cornerLarge = 16.dp
    val gutter = 16.dp
    val row = 56.dp
}
