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

// Brand palette ---------------------------------------------------------------
val Midnight = Color(0xFF05081A)
val DeepNavy = Color(0xFF0A112E)
val Navy = Color(0xFF111A3F)
val NavySoft = Color(0xFF1A2450)
val ElectricBlue = Color(0xFF2F6BFF)
val NovaCyan = Color(0xFF45E0FF)
val NovaViolet = Color(0xFF9B5CFF)
val SoftLavender = Color(0xFFC9BDFF)
val FrostWhite = Color(0xFFF4F7FF)

fun accentColor(name: String): Color = when (name) {
    "cyan" -> NovaCyan
    "violet" -> NovaViolet
    "lavender" -> SoftLavender
    else -> ElectricBlue
}

val BrandBrush = Brush.linearGradient(listOf(NovaCyan, ElectricBlue, NovaViolet))
val AuroraBrush = Brush.linearGradient(listOf(ElectricBlue, NovaViolet, NovaCyan))

data class NovaStyle(
    val dark: Boolean,
    val accent: Color,
    val glassIntensity: Float,
    val highlights: Boolean
)

val LocalNovaStyle = staticCompositionLocalOf {
    NovaStyle(dark = true, accent = ElectricBlue, glassIntensity = 1f, highlights = true)
}

val NovaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp, letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp, lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, letterSpacing = 1.6.sp
    )
)

private val DarkScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    secondary = NovaCyan,
    tertiary = NovaViolet,
    background = Midnight,
    onBackground = FrostWhite,
    surface = DeepNavy,
    onSurface = FrostWhite,
    surfaceVariant = Navy,
    onSurfaceVariant = SoftLavender,
    outline = Color.White.copy(alpha = 0.18f),
    error = Color(0xFFFF6B8B)
)

private val LightScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    secondary = Color(0xFF0096C7),
    tertiary = Color(0xFF7B3FE4),
    background = Color(0xFFF1F4FC),
    onBackground = Color(0xFF101528),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101528),
    surfaceVariant = Color(0xFFE6ECFA),
    onSurfaceVariant = Color(0xFF4B5470),
    outline = Color(0xFF1B2440).copy(alpha = 0.12f),
    error = Color(0xFFCC2E4E)
)

data class WallpaperPreset(val id: String, val name: String, val colors: List<Color>)

object Wallpapers {
    val presets = listOf(
        WallpaperPreset("aurora", "Aurora", listOf(Color(0xFF05081A), Color(0xFF13245C), Color(0xFF3A2A72), Color(0xFF0A2C4A))),
        WallpaperPreset("midnight", "Midnight", listOf(Color(0xFF03050F), Color(0xFF0A112E), Color(0xFF141D44))),
        WallpaperPreset("nebula", "Nebula", listOf(Color(0xFF08091F), Color(0xFF3A1D63), Color(0xFF1B3E72))),
        WallpaperPreset("glacier", "Glacier", listOf(Color(0xFF04101F), Color(0xFF0E3C5A), Color(0xFF2B7A8C))),
        WallpaperPreset("violet", "Violet Dusk", listOf(Color(0xFF0B0620), Color(0xFF2A1155), Color(0xFF5B2A8C))),
        WallpaperPreset("graphite", "Graphite", listOf(Color(0xFF080A12), Color(0xFF1B2030), Color(0xFF2C3348))),
        WallpaperPreset("dawn", "Dawn", listOf(Color(0xFFF4F7FF), Color(0xFFDCE6FF), Color(0xFFEADFFF))),
        WallpaperPreset("frost", "Frost", listOf(Color(0xFFEFF4FF), Color(0xFFD7E7FF), Color(0xFFC4F0FF)))
    )

    fun preset(id: String): WallpaperPreset = presets.firstOrNull { it.id == id } ?: presets.first()

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
    val accent = accentColor(settings.accent)
    val scheme = (if (dark) DarkScheme else LightScheme).copy(primary = accent)
    val style = NovaStyle(
        dark = dark,
        accent = accent,
        glassIntensity = settings.glassIntensity.coerceIn(0.4f, 1.4f),
        highlights = settings.glassHighlights
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

object NovaTokens {
    val cornerSmall = 14.dp
    val corner = 22.dp
    val cornerLarge = 30.dp
    val gutter = 16.dp
}
