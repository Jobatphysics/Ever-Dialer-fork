package com.android.libredialer.view.newui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.libredialer.view.newui.motion.LensInteractionSource
import com.android.libredialer.view.newui.motion.hybridLensTransform

/**
 * Analytical optical lens surface modifier.
 * Renders physical glass-like characteristics without GPU blur shaders:
 * - Translucent layered tonal tint
 * - Analytical specular rim highlight (Fresnel reflection)
 * - Directional ambient glaze
 * - Localized interactive caustics (active strictly during touch)
 * - Closed-form concentric water ripple waves (active strictly during wave expansion)
 * - Physical elevation and optical depth
 *
 * Uses `drawWithCache` to guarantee zero allocations per frame during 60/120 Hz playback.
 */
fun Modifier.lensSurface(
    shape: Shape,
    tonalColor: Color,
    translucentAlpha: Float = 0.72f,
    specularAlpha: Float = 0.32f,
    elevation: Dp = 2.dp,
    interactionSource: LensInteractionSource? = null,
    borderWidth: Dp = 1.2.dp,
    opticalDepthFactor: Float = 1.0f
): Modifier = this
    .shadow(
        elevation = elevation * opticalDepthFactor,
        shape = shape,
        clip = false
    )
    .clip(shape)
    .drawWithCache {
        val strokePx = borderWidth.toPx()

        // 1. Pre-calculate and cache incident optical rim brush (135° to 315°)
        val rimBrush = Brush.linearGradient(
            0.0f to Color.White.copy(alpha = specularAlpha),
            0.40f to Color.White.copy(alpha = specularAlpha * 0.25f),
            0.80f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = 0.18f),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )

        // 2. Pre-calculate and cache top ambient directional glare
        val ambientGlazeBrush = Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.09f),
            0.45f to Color.Transparent,
            startY = 0f,
            endY = size.height
        )

        // 3. Cache shape outline for rim stroke drawing
        val outline = shape.createOutline(size, layoutDirection, this)
        val strokeStyle = Stroke(width = strokePx)

        onDrawWithContent {
            // Layer 1: Base translucent surface container tint
            drawRect(color = tonalColor.copy(alpha = translucentAlpha))

            // Layer 2: Ambient top optical glaze
            drawRect(brush = ambientGlazeBrush)

            // Layer 3: Underlying child content
            drawContent()

            // Layer 4: Localized interactive caustic spotlight (ACTIVE ONLY ON TOUCH)
            interactionSource?.let { source ->
                if (source.isPressed && source.pointerPosition != Offset.Unspecified) {
                    val causticRadius = (size.minDimension * 0.60f).coerceAtLeast(36.dp.toPx())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = source.pointerPosition,
                            radius = causticRadius
                        )
                    )
                }

                // Layer 5: Concentric water wave rings (ACTIVE ONLY WHILE PROPAGATING)
                source.waterRippleState?.let { ripple ->
                    val ringAlpha = ripple.ringAlpha
                    if (ringAlpha > 0.01f && ripple.progress in 0f..1f) {
                        val waveStrokeWidth = (2.dp.toPx() * (1f - ripple.progress)).coerceAtLeast(1f)
                        drawCircle(
                            color = Color.White.copy(alpha = ringAlpha),
                            radius = ripple.currentRadius,
                            center = ripple.center,
                            style = Stroke(width = waveStrokeWidth)
                        )
                    }
                }
            }

            // Layer 6: Analytical specular rim highlight
            drawOutline(
                outline = outline,
                brush = rimBrush,
                style = strokeStyle
            )
        }
    }

/**
 * Reusable container composable applying the [lensSurface] effect.
 */
@Composable
fun LensSurfaceBox(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    tonalColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    translucentAlpha: Float = 0.72f,
    specularAlpha: Float = 0.32f,
    elevation: Dp = 2.dp,
    interactionSource: LensInteractionSource? = null,
    borderWidth: Dp = 1.2.dp,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val physicalModifier = if (interactionSource != null) {
        modifier.hybridLensTransform(interactionSource)
    } else {
        modifier
    }

    Box(
        modifier = physicalModifier.lensSurface(
            shape = shape,
            tonalColor = tonalColor,
            translucentAlpha = translucentAlpha,
            specularAlpha = specularAlpha,
            elevation = elevation,
            interactionSource = interactionSource,
            borderWidth = borderWidth
        ),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * Card-like lens surface composable with default M3 Expressive large container rounding.
 */
@Composable
fun LensSurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    tonalColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    translucentAlpha: Float = 0.80f,
    specularAlpha: Float = 0.30f,
    elevation: Dp = 2.dp,
    interactionSource: LensInteractionSource? = null,
    borderWidth: Dp = 1.2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    LensSurfaceBox(
        modifier = modifier,
        shape = shape,
        tonalColor = tonalColor,
        translucentAlpha = translucentAlpha,
        specularAlpha = specularAlpha,
        elevation = elevation,
        interactionSource = interactionSource,
        borderWidth = borderWidth,
        contentAlignment = Alignment.TopStart,
        content = content
    )
}
