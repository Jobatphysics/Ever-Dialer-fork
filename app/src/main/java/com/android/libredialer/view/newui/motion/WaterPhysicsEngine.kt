package com.android.libredialer.view.newui.motion

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Closed-form analytical wave packet modeling localized water ripple propagation.
 * Evaluates without numerical grid integration, ensuring zero memory allocations
 * and guaranteed 60/120 Hz rendering performance.
 */
@Immutable
data class WaterWavePacket(
    val center: Offset,
    val progress: Float, // Normalized lifecycle progress: 0f (impact) to 1f (settled)
    val maxRadius: Float, // Maximum wave expansion distance
    val damping: Float = 3.2f, // Viscous temporal damping factor
    val wavelength: Float = 42f, // Spatial width of the wave crest (px)
    val peakAmplitude: Float = 0.30f // Peak optical highlight intensity
) {
    /**
     * Leading wave crest radius at current progress.
     */
    val currentRadius: Float
        get() = progress * maxRadius

    /**
     * Alpha opacity for rendering single-pass circular wave highlights.
     * Dampens exponentially as the wave expands outward to the perimeter.
     */
    val ringAlpha: Float
        get() = (peakAmplitude * (1f - progress) * exp(-damping * progress)).coerceIn(0f, 1f)

    /**
     * Analytical wave amplitude at an arbitrary radial distance [distance] from [center].
     * Uses a Gaussian wave-packet envelope:
     * A(r) = A_0 * exp(-damping * progress) * exp(-(r - r_c)^2 / (2 * sigma^2))
     */
    fun amplitudeAt(distance: Float): Float {
        if (progress <= 0f || progress >= 1f) return 0f
        val dr = distance - currentRadius
        val sigma = wavelength * 0.85f
        val spatialSpread = exp(-(dr * dr) / (2f * sigma * sigma))
        val temporalDecay = (1f - progress) * exp(-damping * progress)
        return (peakAmplitude * temporalDecay * spatialSpread).coerceIn(0f, 1f)
    }
}

/**
 * Fluid shear and boundary deformation calculator for touch drag interactions.
 * Models volume-preserving fluid stretch along the drag vector and viscous drag resistance.
 */
@Immutable
data class FluidShearDeformation(
    val dragOffset: Offset,
    val dragVelocity: Offset = Offset.Zero,
    val resistance: Float = 0.40f // Stokes fluid drag resistance factor
) {
    /**
     * Effective physical displacement after fluid resistance.
     */
    val effectiveDisplacement: Offset
        get() = dragOffset * (1f - resistance)

    /**
     * Flow angle in radians along the drag displacement vector.
     */
    val flowAngleRad: Float
        get() = atan2(dragOffset.y, dragOffset.x)

    /**
     * Distance of the drag displacement in pixels.
     */
    val distance: Float
        get() = dragOffset.getDistance()

    /**
     * Stretch factor parallel to the flow direction (volume-preserving elongation).
     * Clamped to subtle physical deformation to prevent visual distortion.
     */
    val stretchParallel: Float
        get() {
            val strain = (distance / 280f).coerceIn(0f, 0.12f)
            return 1f + strain
        }

    /**
     * Compression factor perpendicular to the flow direction.
     * Ensures volume preservation: lambda_perp = 1 / sqrt(lambda_parallel).
     */
    val squashPerpendicular: Float
        get() = 1f / sqrt(stretchParallel)
}

/**
 * Utility functions for water dynamics calculations.
 */
object WaterPhysicsMath {
    /**
     * Computes maximum propagation radius based on surface bounding box dimensions.
     */
    fun computeMaxRadius(width: Float, height: Float): Float {
        val maxDim = maxOf(width, height)
        return maxDim * 1.15f
    }

    /**
     * Calculates optical refraction displacement vector at a given distance from ripple center.
     */
    fun refractionOffset(
        point: Offset,
        ripple: WaterWavePacket
    ): Offset {
        val delta = point - ripple.center
        val dist = delta.getDistance()
        if (dist <= 0.001f) return Offset.Zero
        val amp = ripple.amplitudeAt(dist)
        val normal = delta / dist
        // Optical gradient displacement normal to the wavefront
        return normal * (amp * 6f)
    }
}
