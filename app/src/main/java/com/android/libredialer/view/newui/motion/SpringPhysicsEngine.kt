package com.android.libredialer.view.newui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Expressive spring physics configuration modeling physical mass, stiffness, and damping.
 * Designed for 60/120 Hz interaction-driven motion with velocity inheritance.
 */
@Immutable
data class SpringPhysicsConfig(
    val stiffness: Float,
    val dampingRatio: Float,
    val mass: Float = 1f,
    val name: String = "Spring"
) {
    /**
     * Effective stiffness normalized by mass (omega_n^2 = k / m).
     */
    val effectiveStiffness: Float
        get() = stiffness / mass.coerceAtLeast(0.01f)

    /**
     * Natural angular frequency omega_n in rad/s.
     */
    val naturalFrequency: Float
        get() = sqrt(effectiveStiffness)

    /**
     * Converts to a Jetpack Compose [SpringSpec].
     */
    fun <T> toSpringSpec(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = dampingRatio,
            stiffness = effectiveStiffness,
            visibilityThreshold = visibilityThreshold
        )

    /**
     * Closed-form analytical evaluation of 1D spring displacement at elapsed time [tSeconds].
     * Models the exact damped harmonic oscillator without numerical integration:
     * x(t) = exp(-zeta * omega_n * t) * (C1 * cos(omega_d * t) + C2 * sin(omega_d * t))
     */
    fun evaluateDisplacement(
        initialDisplacement: Float,
        initialVelocity: Float,
        tSeconds: Float
    ): Float {
        if (tSeconds <= 0f) return initialDisplacement
        val omegaN = naturalFrequency
        val zeta = dampingRatio

        return if (zeta < 1f) {
            // Underdamped oscillation
            val omegaD = omegaN * sqrt(1f - zeta * zeta)
            val decay = exp(-zeta * omegaN * tSeconds)
            val c1 = initialDisplacement
            val c2 = (initialVelocity + zeta * omegaN * initialDisplacement) / omegaD
            decay * (c1 * cos(omegaD * tSeconds) + c2 * sin(omegaD * tSeconds))
        } else {
            // Critically damped or overdamped
            val decay = exp(-omegaN * tSeconds)
            val c1 = initialDisplacement
            val c2 = initialVelocity + omegaN * initialDisplacement
            decay * (c1 + c2 * tSeconds)
        }
    }
}

/**
 * Standard spring catalog tailored for LibreDialer UI interactions.
 */
object SpringPhysicsCatalog {
    /**
     * Tactile button press & snap (88dp keypad circles, call triggers).
     * High stiffness and light mass for instant haptic visual feedback,
     * with controlled underdamped micro-rebound (zeta = 0.72).
     */
    val ButtonSnap = SpringPhysicsConfig(
        stiffness = 2200f,
        dampingRatio = 0.72f,
        mass = 0.75f,
        name = "ButtonSnap"
    )

    /**
     * Rapid keypad DTMF entry.
     * Critically damped to eliminate visual lag during high-frequency typing.
     */
    val KeypadRapid = SpringPhysicsConfig(
        stiffness = 2800f,
        dampingRatio = 0.88f,
        mass = 0.60f,
        name = "KeypadRapid"
    )

    /**
     * Navigation transitions & fluid indicator pill gliding.
     * Medium stiffness with organic glide and soft settling.
     */
    val NavigationGlide = SpringPhysicsConfig(
        stiffness = 650f,
        dampingRatio = 0.80f,
        mass = 1.0f,
        name = "NavigationGlide"
    )

    /**
     * Grouped card surfaces and contact list tiles.
     * Balanced mass and damping for weighty physical depth.
     */
    val CardSurface = SpringPhysicsConfig(
        stiffness = 450f,
        dampingRatio = 0.85f,
        mass = 1.2f,
        name = "CardSurface"
    )

    /**
     * Drag-and-release settling for gesture swipes, sheets, and overscroll.
     * Velvety deceleration absorbing release flings without harsh snapping.
     */
    val DragSettling = SpringPhysicsConfig(
        stiffness = 320f,
        dampingRatio = 0.86f,
        mass = 1.4f,
        name = "DragSettling"
    )
}

/**
 * Settles a 1D [Animatable] to [targetValue] inheriting pointer release [initialVelocity].
 */
suspend fun Animatable<Float, AnimationVector1D>.settleWithVelocity(
    targetValue: Float,
    initialVelocity: Float = 0f,
    config: SpringPhysicsConfig = SpringPhysicsCatalog.ButtonSnap
) {
    animateTo(
        targetValue = targetValue,
        animationSpec = config.toSpringSpec(),
        initialVelocity = initialVelocity
    )
}

/**
 * Settles a 2D [Animatable] offset to [targetValue] inheriting pointer release [initialVelocity].
 */
suspend fun Animatable<Offset, AnimationVector2D>.settleOffsetWithVelocity(
    targetValue: Offset,
    initialVelocity: Offset = Offset.Zero,
    config: SpringPhysicsConfig = SpringPhysicsCatalog.DragSettling
) {
    animateTo(
        targetValue = targetValue,
        animationSpec = config.toSpringSpec(),
        initialVelocity = initialVelocity
    )
}
