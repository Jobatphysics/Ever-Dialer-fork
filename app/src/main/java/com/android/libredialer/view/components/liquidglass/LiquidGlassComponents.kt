package com.android.libredialer.view.components.liquidglass

import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.android.libredialer.controller.util.PreferenceManager
import com.example.liquidglass.GlassMaterial
import com.example.liquidglass.LiquidGlassView
import org.koin.compose.koinInject

/**
 * Neutral grey tint with equal red, green, and blue components.
 * Red = 128, Green = 128, Blue = 128, Alpha = 255.
 */
val LIQUID_GLASS_NEUTRAL_GREY: Int = (0xFF shl 24) or (128 shl 16) or (128 shl 8) or 128

const val DEFAULT_LIQUID_GLASS_TINT_ALPHA = 0.10f

val LocalLiquidGlassBackdropSource = staticCompositionLocalOf<View?> { null }

private val LocalGlassOverLightBackdrop =
    androidx.compose.runtime.compositionLocalOf<Boolean?> { null }

@Composable
fun rememberLiquidGlassTint(prefs: PreferenceManager = koinInject()): Float {
    val settingsVersion by prefs.settingsChanged.collectAsState()
    return remember(settingsVersion) {
        prefs.getLiquidGlassTint().coerceIn(0f, 0.25f)
    }
}

private data class LiquidGlassConfiguration(
    val source: View,
    val cornerRadiusPx: Float,
    val tintAlpha: Float,
    val dynamicBackground: Boolean
)

private class LiquidGlassConfigurationCache {
    private var configuredView: LiquidGlassView? = null
    private var configuration: LiquidGlassConfiguration? = null

    fun apply(
        view: LiquidGlassView,
        source: View,
        cornerRadiusPx: Float,
        tintAlpha: Float,
        dynamicBackground: Boolean = false
    ) {
        val requested = LiquidGlassConfiguration(source, cornerRadiusPx, tintAlpha, dynamicBackground)
        if (configuredView === view && configuration == requested) return

        view.configureLiquidGlass(source, cornerRadiusPx, tintAlpha, dynamicBackground)
        configuredView = view
        configuration = requested
    }
}

/** True when the library's luminance meter classifies the captured backdrop as light. */
val glassOverLightBackdrop: Boolean?
    @Composable get() = LocalGlassOverLightBackdrop.current

/**
 * Draws existing Compose button content above the library's View-based glass renderer.
 * Keeping content outside the glass View prevents its redraws from invalidating backdrop capture.
 */
@Composable
fun LiquidGlassActionSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp,
    backdropSource: View? = LocalLiquidGlassBackdropSource.current,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    tintAlpha: Float = rememberLiquidGlassTint(),
    content: @Composable () -> Unit
) {
    val source = backdropSource ?: LocalView.current
    val overLight = remember { mutableStateOf<Boolean?>(null) }
    val configurationCache = remember { LiquidGlassConfigurationCache() }
    val appearanceListener = remember(overLight) {
        { isOverLight: Boolean -> overLight.value = isOverLight }
    }

    if (!supportsLiquidGlass(source)) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(fallbackColor),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
        return
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { context ->
                val radiusPx = with(context.resources.displayMetrics) {
                    cornerRadius.value * density
                }
                LiquidGlassView(context).apply {
                    configurationCache.apply(this, source, radiusPx, tintAlpha)
                    glassAppearanceListener = appearanceListener
                }
            },
            update = { glass ->
                val radiusPx = with(glass.resources.displayMetrics) {
                    cornerRadius.value * density
                }
                configurationCache.apply(glass, source, radiusPx, tintAlpha)
            }
        )
        CompositionLocalProvider(LocalGlassOverLightBackdrop provides overLight.value) {
            content()
        }
    }
}

/**
 * Hosts header heading content in a compact original Liquid Glass surface.
 */
@Composable
fun LiquidGlassHeadingSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    backdropSource: View? = LocalLiquidGlassBackdropSource.current,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    tintAlpha: Float = rememberLiquidGlassTint(),
    content: @Composable () -> Unit
) {
    val source = backdropSource ?: LocalView.current
    val overLight = remember { mutableStateOf<Boolean?>(null) }
    val configurationCache = remember { LiquidGlassConfigurationCache() }
    val appearanceListener = remember(overLight) {
        { isOverLight: Boolean -> overLight.value = isOverLight }
    }

    if (!supportsLiquidGlass(source)) {
        Box(
            modifier = modifier
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(fallbackColor),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
        return
    }

    Box(
        modifier = modifier.heightIn(min = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { context ->
                val radiusPx = with(context.resources.displayMetrics) {
                    cornerRadius.value * density
                }
                LiquidGlassView(context).apply {
                    configurationCache.apply(this, source, radiusPx, tintAlpha)
                    glassAppearanceListener = appearanceListener
                }
            },
            update = { glass ->
                val radiusPx = with(glass.resources.displayMetrics) {
                    cornerRadius.value * density
                }
                configurationCache.apply(glass, source, radiusPx, tintAlpha)
            }
        )
        CompositionLocalProvider(LocalGlassOverLightBackdrop provides overLight.value) {
            content()
        }
    }
}

/**
 * QWEA0's View renderer hosted as one AndroidView beneath the Compose navigation content.
 */
@Composable
fun LiquidGlassNavigationSurface(
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    backdropSource: View? = LocalLiquidGlassBackdropSource.current,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    dynamicBackground: Boolean = false,
    tintAlpha: Float = rememberLiquidGlassTint(),
    content: @Composable RowScope.() -> Unit
) {
    val source = backdropSource ?: LocalView.current
    val overLight = remember { mutableStateOf<Boolean?>(null) }
    val configurationCache = remember { LiquidGlassConfigurationCache() }
    val appearanceListener = remember(overLight) {
        { isOverLight: Boolean -> overLight.value = isOverLight }
    }

    if (!supportsLiquidGlass(source)) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(999.dp))
                .background(fallbackColor),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                content(this)
            }
        }
        return
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { context ->
                LiquidGlassView(context).apply {
                    configurationCache.apply(this, source, 999f, tintAlpha, dynamicBackground)
                    glassAppearanceListener = appearanceListener
                }
            },
            update = { glass ->
                configurationCache.apply(glass, source, 999f, tintAlpha, dynamicBackground)
                glass.glassAppearanceListener = appearanceListener
            }
        )
        CompositionLocalProvider(
            LocalLiquidGlassBackdropSource provides source,
            LocalGlassOverLightBackdrop provides overLight.value
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                content(this)
            }
        }
    }
}

@Composable
fun LiquidGlassTabSelectionIndicator(
    modifier: Modifier = Modifier,
    backdropSource: View?,
    dynamicBackground: Boolean = false,
    tintAlpha: Float = rememberLiquidGlassTint(),
    fallbackColor: Color = MaterialTheme.colorScheme.secondaryContainer
) {
    if (backdropSource == null || !supportsLiquidGlass(backdropSource)) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(999.dp))
                .background(fallbackColor)
        )
        return
    }

    val overLight = LocalGlassOverLightBackdrop.current ?: false
    val selectedTintAlpha = maxOf(tintAlpha, SELECTED_LIQUID_GLASS_TINT_ALPHA)
    val configurationCache = remember { LiquidGlassConfigurationCache() }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LiquidGlassView(context).apply {
                configurationCache.apply(
                    this,
                    backdropSource,
                    999f,
                    selectedTintAlpha,
                    dynamicBackground
                )
                this.overLight = overLight
            }
        },
        update = { indicator ->
            configurationCache.apply(
                indicator,
                backdropSource,
                999f,
                selectedTintAlpha,
                dynamicBackground
            )
            if (indicator.overLight != overLight) {
                indicator.overLight = overLight
            }
        }
    )
}

private const val SELECTED_LIQUID_GLASS_TINT_ALPHA = 0.18f

fun supportsLiquidGlass(backdropSource: View): Boolean =
    (backdropSource.isHardwareAccelerated || !backdropSource.isAttachedToWindow) &&
        Build.SUPPORTED_ABIS.any { abi ->
            abi == "arm64-v8a" || abi == "armeabi-v7a"
        }

fun LiquidGlassView.configureLiquidGlass(
    source: View,
    radiusPx: Float,
    tintAlpha: Float,
    dynamicBackground: Boolean = false
) {
    if (backdropSource !== source) backdropSource = source
    val wasDynamicBackground = enableDynamicBackground
    enableDynamicBackground = dynamicBackground
    useShaderPipeline = true
    useHardwareBlurWhenPossible = true
    material = GlassMaterial.REGULAR

    cornerRadius = radiusPx
    adaptiveLensScale = true
    bevelWidth = 56f
    refractionHeight = 200f
    refractionFalloff = 2f
    refractionNoFold = false
    refractionOutward = false
    dispersionStrength = 0.16f

    enableBackdropBlur = true
    blurAmount = 0.055f
    saturation = 200f
    enableEdgeHighlight = true
    edgeHighlightBorderWidth = 1.5f
    edgeHighlightOpacity = 100f

    setGlassTint(LIQUID_GLASS_NEUTRAL_GREY, tintAlpha)
    enableAdaptiveTint = false
    enableSensorHighlight = false
    enablePressEffect = false
    pressScale = 1f
    elasticity = 0.15f

    enableChromaticAberration = true
    enableChromaticDispersion = false
    aberrationIntensity = 2f
    displacementScale = 70f
    aberrationRedOffset = 0f
    aberrationGreenOffset = -0.05f
    aberrationBlueOffset = -0.10f

    blurMethod = com.example.liquidglass.BlurMethod.SMART
    enableShadow = false
    collectFrameStats = false

    if (wasDynamicBackground && !dynamicBackground) invalidate()
}
