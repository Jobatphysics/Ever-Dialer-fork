package dev.libreglass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** P0 tuning controls. Values are normalized defensively before reaching AGSL. */
@Immutable
public data class LibreGlassOptics(
    /** 0 is opaque material tint; 1 keeps sampled content fully visible while retaining optics. */
    val transparency: Float = 0.72f,
    /** Normalized edge-lens strength, independent from [transparency]. */
    val refraction: Float = 0.48f,
    val horizontalEdgeWidth: Float = 0.18f,
    val verticalEdgeWidth: Float = 0.18f,
    val horizontalCurvature: Float = 0.48f,
    val verticalCurvature: Float = 0.48f,
    val blurRadius: Float = 14f,
    val lensStrength: Float = 10f,
    val refractionWidth: Float = 20f,
    val distortion: Float = 1.25f,
    val chromaticDispersion: Float = 0.65f,
    val saturation: Float = 1.08f,
    val brightness: Float = 0.02f,
    val tint: Color = Color.White.copy(alpha = 0.16f),
    val highlightAlpha: Float = 0.26f,
    val rimAlpha: Float = 0.34f,
    val shadowAlpha: Float = 0.16f,
) {
    internal fun normalized(): LibreGlassOptics = copy(
        transparency = transparency.coerceIn(0f, 1f),
        refraction = refraction.coerceIn(0f, 1f),
        horizontalEdgeWidth = horizontalEdgeWidth.coerceIn(0f, 1f),
        verticalEdgeWidth = verticalEdgeWidth.coerceIn(0f, 1f),
        horizontalCurvature = horizontalCurvature.coerceIn(0f, 1f),
        verticalCurvature = verticalCurvature.coerceIn(0f, 1f),
        blurRadius = blurRadius.coerceIn(0f, 40f),
        lensStrength = lensStrength.coerceIn(0f, 32f),
        refractionWidth = refractionWidth.coerceIn(1f, 96f),
        distortion = distortion.coerceIn(0f, 8f),
        chromaticDispersion = chromaticDispersion.coerceIn(0f, 4f),
        saturation = saturation.coerceIn(0f, 2f),
        brightness = brightness.coerceIn(-0.25f, 0.25f),
        highlightAlpha = highlightAlpha.coerceIn(0f, 1f),
        rimAlpha = rimAlpha.coerceIn(0f, 1f),
        shadowAlpha = shadowAlpha.coerceIn(0f, 1f),
    )
}
