package com.novabrowse.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novabrowse.browser.ui.theme.LocalNovaStyle
import com.novabrowse.browser.ui.theme.NovaCyan
import com.novabrowse.browser.ui.theme.NovaViolet

/**
 * Chrome's card surface. This keeps the original call sites (`GlassPanel`) but draws
 * the flat, bordered material container Chrome uses everywhere: one neutral fill, a
 * hairline outline and only the faintest elevation for floating layers.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    intensity: Float = 1f,
    strong: Boolean = false,
    elevation: Dp = 0.dp,
    tint: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val fill = tint ?: scheme.surface
    val outline = scheme.outlineVariant.copy(alpha = 0.7f)
    // Only genuinely floating surfaces (menus, dialogs, sheets) get a shadow.
    val float = elevation >= 16.dp
    Box(
        modifier = modifier
            .then(
                if (float) Modifier.shadow(
                    elevation = 8.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.28f),
                    spotColor = Color.Black.copy(alpha = 0.28f)
                ) else Modifier
            )
            .clip(shape)
            .background(fill)
            .border(1.dp, outline, shape)
    ) {
        content()
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) tint else tint.copy(alpha = 0.38f)
        ) { content() }
    }
}

@Composable
fun GradientText(
    text: String,
    style: TextStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp),
    modifier: Modifier = Modifier,
    brush: Brush = Brush.linearGradient(listOf(NovaCyan, Color(0xFF6FA8FF), NovaViolet)),
    textAlign: TextAlign? = null
) {
    Text(text = text, modifier = modifier, style = style.copy(brush = brush), textAlign = textAlign)
}

/** Chrome-style wordmark: flat, two-tone, no glass furniture. */
@Composable
fun NovaWordmark(
    modifier: Modifier = Modifier,
    size: Int = 22,
    showTagline: Boolean = false,
    light: Boolean = !LocalNovaStyle.current.dark
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Row {
            Text(
                "Nova",
                style = TextStyle(
                    fontSize = size.sp, fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.2).sp, color = scheme.onSurface
                )
            )
            Text(
                "Browse",
                style = TextStyle(
                    fontSize = size.sp, fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.2).sp, color = scheme.primary
                )
            )
        }
        if (showTagline) {
            Text(
                "BROWSE BEYOND.",
                style = TextStyle(
                    fontSize = 9.sp, letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Retained for callers; Chrome surfaces have no animated sheen. */
@Composable
fun sheenAlpha(enabled: Boolean = true): Float = 0f

@Composable
fun GlassPill(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { content() }
    }
}

@Composable
fun GlowDot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun NovaBrandTile(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    GlassPanel(modifier = modifier.size(size), shape = RoundedCornerShape(size * 0.24f)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CompassMark(Modifier.size(size * 0.62f))
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
            color = Color(0xFF8AB4F8).copy(alpha = 0.18f),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFF8AB4F8).copy(alpha = 0.75f),
            radius = r,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.045f)
        )
        val north = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - r * 0.8f)
            lineTo(cx + r * 0.32f, cy)
            lineTo(cx - r * 0.32f, cy)
            close()
        }
        val south = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy + r * 0.8f)
            lineTo(cx + r * 0.32f, cy)
            lineTo(cx - r * 0.32f, cy)
            close()
        }
        drawPath(path = north, color = Color(0xFF1A73E8))
        drawPath(path = south, color = Color(0xFF8AB4F8))
        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = r * 0.08f, center = Offset(cx, cy))
    }
}
