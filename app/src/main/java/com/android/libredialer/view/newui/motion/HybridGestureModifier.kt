package com.android.libredialer.view.newui.motion

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Executes the complete unified physical interaction pipeline:
 * touch -> displacement -> velocity -> water response -> spring recovery.
 *
 * All state is routed into [interactionSource] so rendering nodes can read it
 * inside drawing and transform lambdas without triggering UI recomposition.
 */
fun Modifier.hybridLensInteraction(
    interactionSource: LensInteractionSource,
    springConfig: SpringPhysicsConfig = SpringPhysicsCatalog.ButtonSnap,
    pressedScaleTarget: Float = 0.94f,
    allowDrag: Boolean = false,
    dragResistance: Float = 0.40f,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
): Modifier = this.pointerInput(interactionSource, springConfig, allowDrag) {
    coroutineScope {
        awaitEachGesture {
            // 1. TOUCH CONTACT PHASE
            val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
            val downTime = System.currentTimeMillis()
            val velocityTracker = VelocityTracker()
            velocityTracker.resetTracking()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            val maxRippleRadius = WaterPhysicsMath.computeMaxRadius(size.width.toFloat(), size.height.toFloat())
            interactionSource.onPressDown(
                position = down.position,
                springConfig = springConfig,
                targetScale = pressedScaleTarget,
                maxRippleRadius = maxRippleRadius
            )

            var isLongClickTriggered = false
            var currentPointer = down
            var accumulatedDisplacement = Offset.Zero

            // Optional long-press trigger running concurrently in the coroutine scope
            val longClickJob = onLongClick?.let { action ->
                launch {
                    delay(480L)
                    if (interactionSource.isPressed && accumulatedDisplacement.getDistance() < 24f) {
                        isLongClickTriggered = true
                        action.invoke()
                    }
                }
            }

            try {
                // 2. DISPLACEMENT & DRAG PHASE
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                    if (change.pressed) {
                        currentPointer = change
                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                        if (allowDrag) {
                            val delta = change.positionChange()
                            accumulatedDisplacement += delta
                            val dampedDisplacement = accumulatedDisplacement * (1f - dragResistance)
                            interactionSource.onDragUpdate(
                                displacement = dampedDisplacement,
                                currentPosition = change.position
                            )
                            change.consume()
                        }
                    } else {
                        // Pointer was lifted
                        currentPointer = change
                        break
                    }
                }
            } finally {
                longClickJob?.cancel()
            }

            // 3. VELOCITY TRANSFER & 4. WATER/SPRING RESTORATION PHASE
            val velocity = velocityTracker.calculateVelocity()
            val releaseVelocity = Offset(velocity.x, velocity.y)

            if (currentPointer.pressed) {
                // Cancelled mid-gesture
                interactionSource.onCancel(springConfig)
            } else {
                // Normal release
                interactionSource.onRelease(
                    releaseVelocity = releaseVelocity,
                    springConfig = springConfig,
                    targetScale = 1f
                )

                // Fire click if tap remained within threshold and was not consumed as a long click
                val duration = System.currentTimeMillis() - downTime
                val totalDistance = accumulatedDisplacement.getDistance()
                if (!isLongClickTriggered && duration < 600L && totalDistance < 32f) {
                    onClick?.invoke()
                }
            }
        }
    }
}

/**
 * Applies physical transforms (scale, displacement, pseudo-3D tilt) to the element.
 * Reads all state directly inside the [graphicsLayer] lambda block, bypassing recomposition entirely.
 */
fun Modifier.hybridLensTransform(
    interactionSource: LensInteractionSource,
    tiltDegrees: Float = 3.5f
): Modifier = this.graphicsLayer {
    // Read properties inside lambda for zero-recomposition GPU updates
    val currentScale = interactionSource.scale
    scaleX = currentScale
    scaleY = currentScale

    val currentDrag = interactionSource.dragOffset
    translationX = currentDrag.x
    translationY = currentDrag.y

    // Pseudo-3D optical perspective tilt
    if (interactionSource.isPressed && size.width > 0f && size.height > 0f) {
        val pos = interactionSource.pointerPosition
        if (pos != Offset.Unspecified) {
            val normalizedX = ((pos.x - size.width / 2f) / (size.width / 2f)).coerceIn(-1f, 1f)
            val normalizedY = ((pos.y - size.height / 2f) / (size.height / 2f)).coerceIn(-1f, 1f)
            rotationX = -normalizedY * tiltDegrees
            rotationY = normalizedX * tiltDegrees
        }
    } else {
        rotationX = 0f
        rotationY = 0f
    }

    cameraDistance = 12f * density
}
