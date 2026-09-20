package com.android.libredialer.view.newui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Unified reactive state model tracking touch, fluid dynamics, and spring restoration.
 * Designed to be read inside `graphicsLayer` and `drawWithCache` lambdas to achieve zero recompositions.
 */
@Stable
class LensInteractionSource(
    private val coroutineScope: CoroutineScope
) {
    /** Whether the user's finger is currently in contact with the surface */
    var isPressed: Boolean by mutableStateOf(false)
        private set

    /** Current pointer contact position in local coordinates */
    var pointerPosition: Offset by mutableStateOf(Offset.Unspecified)
        private set

    /** Current interactive drag displacement vector */
    var dragOffset: Offset by mutableStateOf(Offset.Zero)
        private set

    /** Captured pointer release velocity in px/s */
    var velocity: Offset by mutableStateOf(Offset.Zero)
        private set

    /** Dynamic physical scale factor driven by spring physics */
    var scale: Float by mutableFloatStateOf(1f)
        private set

    /** Current analytical water wave packet (null when settled/idle) */
    var waterRippleState: WaterWavePacket? by mutableStateOf(null)
        private set

    /** Active status of fluid ripple propagation */
    val isWaterActive: Boolean
        get() = waterRippleState != null

    /** Active status of spring displacement or oscillation */
    val isSpringActive: Boolean
        get() = scale != 1f || dragOffset != Offset.Zero

    /** Complete dormancy indicator: true when no interaction or animation is executing */
    val isIdle: Boolean
        get() = !isPressed && !isWaterActive && !isSpringActive

    private var rippleJob: Job? = null
    private var springJob: Job? = null

    private val scaleAnimatable = Animatable(1f)
    private val dragXAnimatable = Animatable(0f)
    private val dragYAnimatable = Animatable(0f)

    /**
     * Called when a pointer contacts the surface.
     */
    fun onPressDown(
        position: Offset,
        springConfig: SpringPhysicsConfig = SpringPhysicsCatalog.ButtonSnap,
        targetScale: Float = 0.94f,
        maxRippleRadius: Float = 300f
    ) {
        isPressed = true
        pointerPosition = position

        // 1. Trigger snappy spring compression
        springJob?.cancel()
        springJob = coroutineScope.launch {
            scaleAnimatable.animateTo(
                targetValue = targetScale,
                animationSpec = springConfig.toSpringSpec()
            ) {
                scale = this.value
            }
        }

        // 2. Trigger localized analytical water ripple
        triggerWaterRipple(position, maxRadius = maxRippleRadius)
    }

    /**
     * Called when a pointer moves with displacement.
     */
    fun onDragUpdate(
        displacement: Offset,
        currentPosition: Offset
    ) {
        if (!isPressed) return
        pointerPosition = currentPosition
        dragOffset = displacement
    }

    /**
     * Called when a pointer is lifted, inheriting release velocity into restorative springs.
     */
    fun onRelease(
        releaseVelocity: Offset,
        springConfig: SpringPhysicsConfig = SpringPhysicsCatalog.ButtonSnap,
        targetScale: Float = 1f
    ) {
        isPressed = false
        velocity = releaseVelocity

        springJob?.cancel()
        springJob = coroutineScope.launch {
            // Settle scale back to targetScale inheriting kinetic momentum
            val scaleVelocity = (releaseVelocity.getDistance() / 800f).coerceIn(0f, 3.5f)
            launch {
                scaleAnimatable.animateTo(
                    targetValue = targetScale,
                    animationSpec = springConfig.toSpringSpec(),
                    initialVelocity = scaleVelocity
                ) {
                    scale = this.value
                }
            }

            // Settle drag offset back to Zero with release velocity inheritance
            if (dragOffset != Offset.Zero) {
                launch {
                    dragXAnimatable.snapTo(dragOffset.x)
                    dragXAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = springConfig.toSpringSpec(),
                        initialVelocity = releaseVelocity.x
                    ) {
                        dragOffset = dragOffset.copy(x = this.value)
                    }
                }
                launch {
                    dragYAnimatable.snapTo(dragOffset.y)
                    dragYAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = springConfig.toSpringSpec(),
                        initialVelocity = releaseVelocity.y
                    ) {
                        dragOffset = dragOffset.copy(y = this.value)
                    }
                }
            }
        }
    }

    /**
     * Called when an ongoing gesture is cancelled.
     */
    fun onCancel(
        springConfig: SpringPhysicsConfig = SpringPhysicsCatalog.ButtonSnap
    ) {
        onRelease(Offset.Zero, springConfig)
    }

    /**
     * Triggers an analytical water ripple expanding from [center].
     * Runs strictly for [durationMs] and terminates cleanly to guarantee 0% idle GPU usage.
     */
    fun triggerWaterRipple(
        center: Offset,
        maxRadius: Float = 280f,
        durationMs: Int = 380,
        damping: Float = 3.2f
    ) {
        rippleJob?.cancel()
        rippleJob = coroutineScope.launch {
            val startTimeNanos = withFrameNanos { it }
            val totalDurationNanos = durationMs * 1_000_000L

            while (true) {
                val currentNanos = withFrameNanos { it }
                val elapsedNanos = currentNanos - startTimeNanos
                val progress = (elapsedNanos.toFloat() / totalDurationNanos).coerceIn(0f, 1f)

                waterRippleState = WaterWavePacket(
                    center = center,
                    progress = progress,
                    maxRadius = maxRadius,
                    damping = damping
                )

                if (progress >= 1f) break
            }
            // Transition immediately to null to stop drawing ripple
            waterRippleState = null
        }
    }

    /**
     * Resets all internal physical state to baseline rest.
     */
    fun reset() {
        rippleJob?.cancel()
        springJob?.cancel()
        isPressed = false
        pointerPosition = Offset.Unspecified
        dragOffset = Offset.Zero
        velocity = Offset.Zero
        scale = 1f
        waterRippleState = null
    }
}

/**
 * Creates and remembers a [LensInteractionSource] tied to the local composition lifecycle.
 */
@Composable
fun rememberLensInteractionSource(): LensInteractionSource {
    val scope = rememberCoroutineScope()
    return remember(scope) { LensInteractionSource(scope) }
}
