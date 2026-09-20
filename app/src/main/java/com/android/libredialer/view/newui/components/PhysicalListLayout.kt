package com.android.libredialer.view.newui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import com.android.libredialer.view.newui.motion.SpringPhysicsCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Coordinates physical list interactions across a LazyColumn.
 * When an item is pressed, it depresses slightly and transfers subtle compressive
 * displacement to its immediate neighbors (i-1 and i+1), springing back on release.
 *
 * All transformations are evaluated inside `graphicsLayer` lambdas to ensure zero
 * recomposition overhead during 60/120 Hz interaction.
 */
@Stable
class PhysicalListState(
    private val coroutineScope: CoroutineScope
) {
    /** Currently pressed item index (-1 when idle) */
    var pressedIndex: Int by mutableIntStateOf(-1)
        private set

    /** Touch coordinate on the currently pressed item */
    var pointerOffset: Offset by mutableStateOf(Offset.Unspecified)
        private set

    /** Physical transition progress: 0f (resting) to 1f (fully depressed) */
    var fraction: Float by mutableFloatStateOf(0f)
        private set

    /** True when no item is pressed and all springs have settled to rest */
    val isIdle: Boolean
        get() = pressedIndex == -1 && fraction == 0f

    private var springJob: Job? = null
    private val fractionAnimatable = Animatable(0f)

    /**
     * Called when an item at [index] is touched.
     */
    fun onItemPressed(index: Int, offset: Offset) {
        pressedIndex = index
        pointerOffset = offset

        springJob?.cancel()
        springJob = coroutineScope.launch {
            fractionAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = SpringPhysicsCatalog.ButtonSnap.toSpringSpec()
            ) {
                fraction = this.value
            }
        }
    }

    /**
     * Called when the touch on [index] is released.
     */
    fun onItemReleased(velocity: Offset = Offset.Zero) {
        springJob?.cancel()
        springJob = coroutineScope.launch {
            val flingSpeed = (velocity.getDistance() / 1000f).coerceIn(0f, 3f)
            fractionAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = SpringPhysicsCatalog.CardSurface.toSpringSpec(),
                initialVelocity = flingSpeed
            ) {
                fraction = this.value
            }
            pressedIndex = -1
            pointerOffset = Offset.Unspecified
        }
    }

    /**
     * Called when the touch gesture is cancelled.
     */
    fun onItemCancelled() {
        onItemReleased(Offset.Zero)
    }
}

/**
 * Creates and remembers a [PhysicalListState] tied to the composition.
 */
@Composable
fun rememberPhysicalListState(): PhysicalListState {
    val scope = rememberCoroutineScope()
    return remember(scope) { PhysicalListState(scope) }
}

/**
 * Modifier applied to each item in the physical list.
 * Evaluates primary displacement for index == pressedIndex,
 * and subtle compressive displacement for immediate neighbors (|index - pressedIndex| == 1).
 *
 * Parameters are strictly bounded to maintain subtle, premium movement:
 * - Primary displacement: ~1.5 dp (well within 1–3 dp limit)
 * - Neighboring response: ~0.8 dp (well within 0.5–1 dp limit)
 * - Press scale: 0.98f (within 0.97–0.99 limit)
 * - Rotation: max 0.5 degrees (under 1 degree limit)
 * - Inactive/dormant when idle: zero matrix updates
 */
fun Modifier.physicalListItem(
    state: PhysicalListState,
    index: Int
): Modifier = this.graphicsLayer {
    val currentPressed = state.pressedIndex
    val f = state.fraction

    if (currentPressed == -1 || f == 0f) {
        // Complete dormancy at rest
        scaleX = 1f
        scaleY = 1f
        translationY = 0f
        rotationZ = 0f
        return@graphicsLayer
    }

    val distance = abs(index - currentPressed)

    when (distance) {
        0 -> {
            // DIRECTLY PRESSED ITEM
            // Scale: 1.0f -> 0.98f
            val currentScale = 1f - (0.02f * f)
            scaleX = currentScale
            scaleY = currentScale

            // Subtle vertical displacement: 1.5 dp
            translationY = 1.5.dp.toPx() * f

            // Subtle perspective tilt based on touch X position (< 0.5 degree)
            val pos = state.pointerOffset
            if (pos != Offset.Unspecified && size.width > 0f) {
                val normX = ((pos.x - size.width / 2f) / (size.width / 2f)).coerceIn(-1f, 1f)
                rotationZ = normX * 0.45f * f
            } else {
                rotationZ = 0f
            }
        }
        1 -> {
            // IMMEDIATE NEIGHBOR (i-1 or i+1)
            // Subtle compressive response: 1.0f -> 0.994f
            val neighborScale = 1f - (0.006f * f)
            scaleX = neighborScale
            scaleY = neighborScale

            // Subtle impulse away from pressed center: 0.8 dp
            val direction = if (index < currentPressed) -1f else 1f
            translationY = direction * 0.8.dp.toPx() * f
            rotationZ = 0f
        }
        else -> {
            // Non-neighboring elements remain completely stationary
            scaleX = 1f
            scaleY = 1f
            translationY = 0f
            rotationZ = 0f
        }
    }

    cameraDistance = 12f * density
}

/**
 * Handles pointer touch input for a physical list item without blocking scroll gestures.
 */
fun Modifier.physicalItemInput(
    state: PhysicalListState,
    index: Int,
    onClick: () -> Unit
): Modifier = this.pointerInput(state, index) {
    coroutineScope {
        awaitEachGesture {
            val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
            val downTime = System.currentTimeMillis()
            val velocityTracker = VelocityTracker()
            velocityTracker.resetTracking()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            state.onItemPressed(index, down.position)

            var currentPointer = down
            var accumulatedDistance = 0f

            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                    if (change.pressed) {
                        currentPointer = change
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        accumulatedDistance += (change.position - down.position).getDistance()

                        // If user moved significantly, this is a scroll gesture - release physics cleanly
                        if (accumulatedDistance > 18f) {
                            state.onItemCancelled()
                            break
                        }
                    } else {
                        currentPointer = change
                        break
                    }
                }
            } catch (_: Throwable) {
                state.onItemCancelled()
            }

            val velocity = velocityTracker.calculateVelocity()
            val releaseVelocity = Offset(velocity.x, velocity.y)

            if (currentPointer.pressed) {
                state.onItemCancelled()
            } else {
                state.onItemReleased(releaseVelocity)

                // Trigger onClick if tap was stationary and quick
                val duration = System.currentTimeMillis() - downTime
                if (duration < 500L && accumulatedDistance < 18f) {
                    onClick.invoke()
                }
            }
        }
    }
}
