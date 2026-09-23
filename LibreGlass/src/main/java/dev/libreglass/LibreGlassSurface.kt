package dev.libreglass

import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A rounded P0 glass surface. Children are drawn above the filtered material layer. */
@Composable
public fun LibreGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    optics: LibreGlassOptics = LibreGlassOptics(),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val scene = currentLibreGlassScene
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "LibreGlassPressScale")
    val normalized = optics.normalized().let { style -> if (pressed) style.copy(highlightAlpha = (style.highlightAlpha + .22f).coerceAtMost(1f), rimAlpha = (style.rimAlpha + .12f).coerceAtMost(1f)) else style }
    Box(modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.then(if (onClick == null) Modifier else Modifier.clickable(interactionSource, indication = null, onClick = onClick))) {
        if (scene?.backdrop != null) {
            GlassMaterial(
                modifier = Modifier.matchParentSize(),
                scene = scene,
                cornerRadius = cornerRadius,
                optics = normalized,
            )
        } else if (scene?.liveLayer != null) {
            LiveGlassMaterial(
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
    val backdrop = requireNotNull(scene.backdrop)
    val bitmapShader = BitmapShader(backdrop.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    shader.setInputShader("backdrop", bitmapShader)
    shader.setFloatUniform("resolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
    shader.setFloatUniform("sceneOrigin", sceneOrigin.x.toFloat(), sceneOrigin.y.toFloat())
    shader.setFloatUniform("sceneSize", backdrop.width.toFloat(), backdrop.height.toFloat())
    shader.setFloatUniform("cornerRadius", cornerRadius)
    shader.setFloatUniform("blurRadius", optics.blurRadius)
    shader.setFloatUniform("horizontalEdgeWidth", optics.horizontalEdgeWidth * size.width.coerceAtLeast(1f) * 0.5f)
    shader.setFloatUniform("verticalEdgeWidth", optics.verticalEdgeWidth * size.height.coerceAtLeast(1f) * 0.5f)
    shader.setFloatUniform("horizontalCurvature", optics.horizontalCurvature)
    shader.setFloatUniform("verticalCurvature", optics.verticalCurvature)
    shader.setFloatUniform("lensStrength", optics.refraction * 24f + optics.lensStrength * 0.15f)
    shader.setFloatUniform("refractionWidth", optics.refractionWidth)
    shader.setFloatUniform("distortion", optics.distortion)
    shader.setFloatUniform("dispersion", optics.chromaticDispersion)
    shader.setFloatUniform("saturation", optics.saturation)
    shader.setFloatUniform("brightness", optics.brightness)
    val tint = optics.tint
    shader.setFloatUniform("tint", tint.red, tint.green, tint.blue, tint.alpha * (1f - optics.transparency))
    shader.setFloatUniform("highlightAlpha", optics.highlightAlpha)
    shader.setFloatUniform("rimAlpha", optics.rimAlpha)
    shader.setFloatUniform("shadowAlpha", optics.shadowAlpha)
    return RenderEffect.createRuntimeShaderEffect(shader, "content")
}

@Composable
private fun LiveGlassMaterial(
    modifier: Modifier,
    scene: LibreGlassSceneState,
    cornerRadius: Dp,
    optics: LibreGlassOptics,
) {
    val sourceLayer = requireNotNull(scene.liveLayer)
    val materialLayer = rememberGraphicsLayer()
    val shader = remember { if (android.os.Build.VERSION.SDK_INT >= 33) RuntimeShader(LIBRE_GLASS_LIVE_SHADER) else null }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    var size by remember { mutableStateOf(Size.Zero) }
    var surfaceRootOffset by remember { mutableStateOf(IntOffset.Zero) }
    Box(
        modifier
            .clip(shape)
            .onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }
            .onGloballyPositioned { surfaceRootOffset = it.positionInRoot().let { point -> IntOffset(point.x.toInt(), point.y.toInt()) } }
            .drawWithContent {
                val relative = surfaceRootOffset - scene.backdropRootOffset
                materialLayer.record(this@drawWithContent, layoutDirection, size.toIntSize()) {
                    withTransform({ translate(-relative.x.toFloat(), -relative.y.toFloat()) }) { drawLayer(sourceLayer) }
                }
                if (android.os.Build.VERSION.SDK_INT >= 33 && shader != null) {
                    materialLayer.renderEffect = createLiveEffect(shader, size, cornerRadius.value, optics).asComposeRenderEffect()
                }
                drawLayer(materialLayer)
                if (android.os.Build.VERSION.SDK_INT < 33) {
                    drawRect(optics.tint.copy(alpha = optics.tint.alpha * (1f - optics.transparency)))
                }
            },
    )
}

@RequiresApi(33)
private fun createLiveEffect(
    shader: RuntimeShader,
    size: Size,
    cornerRadius: Float,
    optics: LibreGlassOptics,
): RenderEffect {
    shader.setFloatUniform("resolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
    shader.setFloatUniform("sceneOrigin", 0f, 0f)
    shader.setFloatUniform("sceneSize", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
    shader.setFloatUniform("cornerRadius", cornerRadius)
    shader.setFloatUniform("blurRadius", optics.blurRadius)
    shader.setFloatUniform("horizontalEdgeWidth", optics.horizontalEdgeWidth * size.width.coerceAtLeast(1f) * 0.5f)
    shader.setFloatUniform("verticalEdgeWidth", optics.verticalEdgeWidth * size.height.coerceAtLeast(1f) * 0.5f)
    shader.setFloatUniform("horizontalCurvature", optics.horizontalCurvature)
    shader.setFloatUniform("verticalCurvature", optics.verticalCurvature)
    shader.setFloatUniform("lensStrength", optics.refraction * 24f + optics.lensStrength * 0.15f)
    shader.setFloatUniform("refractionWidth", optics.refractionWidth)
    shader.setFloatUniform("distortion", optics.distortion)
    shader.setFloatUniform("dispersion", optics.chromaticDispersion)
    shader.setFloatUniform("saturation", optics.saturation)
    shader.setFloatUniform("brightness", optics.brightness)
    shader.setFloatUniform("tint", optics.tint.red, optics.tint.green, optics.tint.blue, optics.tint.alpha * (1f - optics.transparency))
    shader.setFloatUniform("highlightAlpha", optics.highlightAlpha)
    shader.setFloatUniform("rimAlpha", optics.rimAlpha)
    shader.setFloatUniform("shadowAlpha", optics.shadowAlpha)
    return RenderEffect.createRuntimeShaderEffect(shader, "content")
}
