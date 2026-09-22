package dev.libreglass

import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A rounded P0 glass surface. Children are drawn above the filtered material layer. */
@Composable
public fun LibreGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    optics: LibreGlassOptics = LibreGlassOptics(),
    content: @Composable BoxScope.() -> Unit,
) {
    val scene = currentLibreGlassScene
    val normalized = optics.normalized()
    Box(modifier = modifier) {
        if (scene != null) {
            GlassMaterial(
                modifier = Modifier.matchParentSize(),
                scene = scene,
                cornerRadius = cornerRadius,
                optics = normalized,
            )
        } else {
            // A legible, deterministic fallback when used outside LibreGlassScene.
            Box(
                Modifier.matchParentSize()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
                    .background(normalized.tint.copy(alpha = maxOf(0.28f, normalized.tint.alpha))),
            )
        }
        content()
    }
}

@Composable
private fun GlassMaterial(
    modifier: Modifier,
    scene: LibreGlassSceneState,
    cornerRadius: Dp,
    optics: LibreGlassOptics,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    val shader = remember(scene.backdrop) { if (android.os.Build.VERSION.SDK_INT >= 33) RuntimeShader(LIBRE_GLASS_SHADER) else null }
    var size by remember { mutableStateOf(Size.Zero) }
    var sceneOrigin by remember { mutableStateOf(IntOffset.Zero) }
    Box(
        modifier
            .clip(shape)
            .onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }
            .onGloballyPositioned { sceneOrigin = it.positionInRoot().let { point -> IntOffset(point.x.toInt(), point.y.toInt()) } }
            .then(
                if (android.os.Build.VERSION.SDK_INT >= 33 && shader != null) {
                    Modifier.graphicsLayer {
                        renderEffect = createP0Effect(shader, scene, size, sceneOrigin, cornerRadius.value, optics).asComposeRenderEffect()
                    }.background(Color.White)
                } else {
                    Modifier.background(optics.tint.copy(alpha = maxOf(0.20f, optics.tint.alpha)))
                },
            ),
    )
}

@RequiresApi(33)
private fun createP0Effect(
    shader: RuntimeShader,
    scene: LibreGlassSceneState,
    size: Size,
    sceneOrigin: IntOffset,
    cornerRadius: Float,
    optics: LibreGlassOptics,
): RenderEffect {
    val bitmapShader = BitmapShader(scene.backdrop.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    shader.setInputShader("backdrop", bitmapShader)
    shader.setFloatUniform("resolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
    shader.setFloatUniform("sceneOrigin", sceneOrigin.x.toFloat(), sceneOrigin.y.toFloat())
    shader.setFloatUniform("sceneSize", scene.backdrop.width.toFloat(), scene.backdrop.height.toFloat())
    shader.setFloatUniform("cornerRadius", cornerRadius)
    shader.setFloatUniform("blurRadius", optics.blurRadius)
    shader.setFloatUniform("lensStrength", optics.lensStrength)
    shader.setFloatUniform("refractionWidth", optics.refractionWidth)
    shader.setFloatUniform("distortion", optics.distortion)
    shader.setFloatUniform("dispersion", optics.chromaticDispersion)
    shader.setFloatUniform("saturation", optics.saturation)
    shader.setFloatUniform("brightness", optics.brightness)
    val tint = optics.tint
    shader.setFloatUniform("tint", tint.red, tint.green, tint.blue, tint.alpha)
    shader.setFloatUniform("highlightAlpha", optics.highlightAlpha)
    shader.setFloatUniform("rimAlpha", optics.rimAlpha)
    shader.setFloatUniform("shadowAlpha", optics.shadowAlpha)
    return RenderEffect.createRuntimeShaderEffect(shader, "content")
}
