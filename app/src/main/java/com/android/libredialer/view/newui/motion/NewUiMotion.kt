package com.android.libredialer.view.newui.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Immutable
data class NewUiSpringSpec(
    val stiffness: Float = Spring.StiffnessMedium,
    val dampingRatio: Float = Spring.DampingRatioNoBouncy,
    val mass: Float = 1f,
    val initialVelocity: Float = 0f
) {
    fun <T> animationSpec(): AnimationSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness / mass.coerceAtLeast(0.01f)
    )
}

object NewUiMotion {
    val Interaction = NewUiSpringSpec(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioNoBouncy
    )
    val Expressive = NewUiSpringSpec(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioMediumBouncy
    )
    val Navigation = NewUiSpringSpec(
        stiffness = Spring.StiffnessLow,
        dampingRatio = Spring.DampingRatioNoBouncy
    )
}

fun Modifier.newUiTransform(
    scale: Float = 1f,
    translationX: Float = 0f,
    translationY: Float = 0f,
    alpha: Float = 1f,
    rotationZ: Float = 0f,
    depth: Float = 0f
): Modifier = graphicsLayer {
    scaleX = scale
    scaleY = scale
    this.translationX = translationX
    this.translationY = translationY
    this.alpha = alpha
    this.rotationZ = rotationZ
    cameraDistance = 8f * density
    shadowElevation = depth
}
