package com.novabrowse.browser.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novabrowse.browser.ui.theme.FrostWhite
import com.novabrowse.browser.ui.theme.LocalNovaStyle
import com.novabrowse.browser.ui.theme.NovaCyan
import com.novabrowse.browser.ui.theme.NovaViolet
import com.novabrowse.browser.ui.theme.SoftLavender

/**
 * The Liquid Glass surface: layered translucency, a light-catching top edge, a soft
 * specular sweep and a real drop shadow. Opacity is clamped so text stays readable
 * on top of arbitrary web content.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    intensity: Float = LocalNovaStyle.current.glassIntensity,
    strong: Boolean = false,
    elevation: Dp = 16.dp,
    tint: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val style = LocalNovaStyle.current
    val dark = style.dark
    val topBase = tint ?: if (dark) Color(0xFF1B2554) else Color.White
    val bottomBase = tint ?: if (dark) Color(0xFF0A1129) else Color(0xFFE7EDFB)
    val topAlpha = ((if (dark) (if (strong) 0.78f else 0.58f) else 0.92f) * intensity).coerceIn(0.12f, 0.98f)
    val bottomAlpha = ((if (dark) (if (strong) 0.6f else 0.42f) else 0.72f) * intensity).coerceIn(0.1f, 0.98f)
    val borderTop = Color.White.copy(alpha = if (dark) 0.34f else 0.95f)
    val borderBottom = Color.White.copy(alpha = if (dark) 0.05f else 0.35f)

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(topBase.copy(alpha = topAlpha), bottomBase.copy(alpha = bottomAlpha)))
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = if (dark) 0.10f else 0.55f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 900f
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(borderTop, borderBottom, Color.Transparent)),
                shape
            )
    ) {
        content()
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    enabled: Boolean = true,
    tint: Color = LocalNovaStyle.current.accent,
    content: @Composable BoxScope.() -> Unit
) {
    GlassPanel(
        modifier = modifier
            .size(size)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        elevation = 6.dp,
        intensity = 0.95f
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides
                    if (enabled) tint else tint.copy(alpha = 0.35f)
            ) { content() }
        }
    }
}

@Composable
fun GradientText(
    text: String,
    style: TextStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp),
    modifier: Modifier = Modifier,
    brush: Brush = Brush.linearGradient(listOf(NovaCyan, Color(0xFF6FA8FF), NovaViolet)),
    textAlign: TextAlign? = null
) {
    Text(text = text, modifier = modifier, style = style.copy(brush = brush), textAlign = textAlign)
}

@Composable
fun NovaWordmark(
    modifier: Modifier = Modifier,
    size: Int = 34,
    showTagline: Boolean = false,
    light: Boolean = !LocalNovaStyle.current.dark
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Nova",
                style = TextStyle(
                    fontSize = size.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp, color = if (light) Color(0xFF101528) else FrostWhite
                )
            )
            GradientText(
                "Browse",
                style = TextStyle(fontSize = size.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
            )
        }
        if (showTagline) {
            Text(
                "BROWSE BEYOND.",
                style = TextStyle(
                    fontSize = 9.sp, letterSpacing = 3.4.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (light) Color(0xFF5A6380) else SoftLavender.copy(alpha = 0.75f)
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Slow specular highlight used sparingly on hero elements. */
@Composable
fun sheenAlpha(enabled: Boolean = true): Float {
    if (!enabled) return 0f
    val transition = rememberInfiniteTransition(label = "sheen")
    val value by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheenValue"
    )
    return 0.04f + value * 0.05f
}

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    GlassPanel(
        modifier = modifier,
        shape = CircleShape,
        elevation = 4.dp,
        intensity = 0.85f
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) { content() }
    }
}

@Composable
fun GlowDot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .shadow(6.dp, CircleShape)
    )
}

@Composable
fun NovaBrandTile(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    GlassPanel(modifier = modifier.size(size), shape = RoundedCornerShape(size * 0.28f), elevation = 10.dp) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CompassMark(Modifier.size(size * 0.66f))
        }
    }
}

/** Vector compass mark drawn from the brand identity (no bitmap dependency). */
@Composable
fun CompassMark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) / 2f * 0.92f
        drawCircle(
            brush = Brush.linearGradient(
                listOf(NovaCyan.copy(alpha = 0.22f), Color.Transparent),
                start = Offset(0f, 0f), end = Offset(w, h)
            ),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = r,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.035f)
        )
        // Compass needle: cyan north, violet south.
        val north = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - r * 0.82f)
            lineTo(cx + r * 0.34f, cy)
            lineTo(cx - r * 0.34f, cy)
            close()
        }
        val south = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy + r * 0.82f)
            lineTo(cx + r * 0.34f, cy)
            lineTo(cx - r * 0.34f, cy)
            close()
        }
        drawPath(
            path = north,
            brush = Brush.linearGradient(
                listOf(NovaCyan, Color(0xFF3C7BFF)),
                start = Offset(cx, cy - r), end = Offset(cx + r, cy)
            )
        )
        drawPath(
            path = south,
            brush = Brush.linearGradient(
                listOf(NovaViolet, Color(0xFF6E3BD6)),
                start = Offset(cx, cy + r), end = Offset(cx - r, cy)
            )
        )
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = r * 0.09f, center = Offset(cx, cy))
        drawCircle(
            color = Color.White.copy(alpha = 0.5f), radius = r * 0.02f,
            center = Offset(cx - r * 0.25f, cy - r * 0.3f)
        )
    }
}
