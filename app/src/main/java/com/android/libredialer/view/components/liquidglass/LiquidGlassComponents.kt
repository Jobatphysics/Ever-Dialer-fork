package com.android.libredialer.view.components.liquidglass

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.liquidglass.GlassMaterial
import com.example.liquidglass.LiquidGlassView

private val LocalGlassOverLightBackdrop =
    androidx.compose.runtime.compositionLocalOf<Boolean?> { null }

private data class NavigationContent(
    val content: @Composable RowScope.() -> Unit,
    val colorScheme: ColorScheme,
    val typography: Typography,
    val shapes: Shapes,
    val layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    val itemSpacing: Dp,
    val contentPadding: PaddingValues
)

private data class ActionContent(
    val content: @Composable () -> Unit,
    val colorScheme: ColorScheme,
    val typography: Typography,
    val shapes: Shapes,
    val layoutDirection: LayoutDirection
)

/** True when the library's luminance meter classifies the captured backdrop as light. */
val glassOverLightBackdrop: Boolean?
    @Composable get() = LocalGlassOverLightBackdrop.current

/**
 * Hosts existing Compose button content in the library's View-based glass renderer.
 * The nested ComposeView keeps click semantics, ripple, and the caller's UI content intact.
 */
@Composable
fun LiquidGlassActionSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp,
    fallbackColor: Color,
    content: @Composable () -> Unit
) {
    val backdropSource = LocalView.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    val layoutDirection = LocalLayoutDirection.current
    val contentState = remember {
        mutableStateOf(ActionContent(content, colorScheme, typography, shapes, layoutDirection))
    }
    val overLight = remember { mutableStateOf<Boolean?>(null) }

    SideEffect {
        contentState.value = ActionContent(content, colorScheme, typography, shapes, layoutDirection)
    }

    if (!supportsLiquidGlass(backdropSource)) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(fallbackColor)
        ) {
            content()
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val radiusPx = with(context.resources.displayMetrics) { cornerRadius.value * density }
            val glass = LiquidGlassView(context).apply {
                configureDemoActionGlass(backdropSource, radiusPx)
                glassAppearanceListener = { overLight.value = it }
            }
            val composeContent = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    val actionContent = contentState.value
                    MaterialTheme(
                        colorScheme = actionContent.colorScheme,
                        typography = actionContent.typography,
                        shapes = actionContent.shapes
                    ) {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides actionContent.layoutDirection,
                            LocalGlassOverLightBackdrop provides overLight.value
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                actionContent.content()
                            }
                        }
                    }
                }
            }
            glass.addView(
                composeContent,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            glass
        },
        update = { glass ->
            val radiusPx = with(glass.resources.displayMetrics) { cornerRadius.value * density }
            glass.configureDemoActionGlass(backdropSource, radiusPx)
            glass.glassAppearanceListener = { overLight.value = it }
        }
    )
}

private fun supportsLiquidGlass(backdropSource: View): Boolean =
    Build.SUPPORTED_ABIS.any { it.startsWith("arm") } ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backdropSource.isHardwareAccelerated)

private fun LiquidGlassView.configureDemoActionGlass(source: View, radiusPx: Float) {
    if (backdropSource !== source) backdropSource = source
    enableDynamicBackground = true
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
    saturation = 150f
    enableEdgeHighlight = true
    edgeHighlightBorderWidth = 1.5f
    edgeHighlightOpacity = 100f

    glassTint = android.graphics.Color.TRANSPARENT
    enableAdaptiveTint = true
    enableSensorHighlight = true
    enablePressEffect = true
    pressScale = 0.95f
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
}

/**
 * QWEA0's View renderer hosted as one AndroidView. The nested ComposeView keeps navigation
 * content inside the glass subtree, which the renderer excludes while sampling its ancestor.
 */
@Composable
fun LiquidGlassNavigationSurface(
    modifier: Modifier = Modifier,
    itemSpacing: androidx.compose.ui.unit.Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit
) {
    val backdropSource = LocalView.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    val layoutDirection = LocalLayoutDirection.current
    val contentState = remember {
        mutableStateOf(
            NavigationContent(
                content, colorScheme, typography, shapes, layoutDirection, itemSpacing, contentPadding
            )
        )
    }
    val overLight = remember { mutableStateOf<Boolean?>(null) }

    SideEffect {
        contentState.value = NavigationContent(
            content, colorScheme, typography, shapes, layoutDirection, itemSpacing, contentPadding
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier,
            factory = { context ->
                val glass = LiquidGlassView(context).apply {
                    configureNavigationGlass(backdropSource)
                    glassAppearanceListener = { overLight.value = it }
                }
                val composeContent = ComposeView(context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                    setContent {
                        val navContent = contentState.value
                        MaterialTheme(
                            colorScheme = navContent.colorScheme,
                            typography = navContent.typography,
                            shapes = navContent.shapes
                        ) {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides navContent.layoutDirection,
                                LocalGlassOverLightBackdrop provides overLight.value
                            ) {
                                Row(
                                    modifier = Modifier.padding(navContent.contentPadding),
                                    horizontalArrangement = Arrangement.spacedBy(navContent.itemSpacing),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    navContent.content(this)
                                }
                            }
                        }
                    }
                }
                glass.addView(
                    composeContent,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.Gravity.CENTER
                    )
                )
                glass
            },
            update = { glass ->
                glass.configureNavigationGlass(backdropSource)
                glass.glassAppearanceListener = { overLight.value = it }
            }
        )
    }
}

private fun LiquidGlassView.configureNavigationGlass(source: View) {
    if (backdropSource !== source) backdropSource = source
    enableDynamicBackground = true
    useShaderPipeline = true
    useHardwareBlurWhenPossible = true
    material = GlassMaterial.REGULAR

    cornerRadius = 999f
    adaptiveLensScale = true
    bevelWidth = 48f
    refractionHeight = 160f
    refractionFalloff = 2f
    refractionNoFold = false
    refractionOutward = false
    dispersionStrength = 0.10f

    enableBackdropBlur = true
    blurAmount = 0.060f
    saturation = 140f
    enableEdgeHighlight = true
    edgeHighlightBorderWidth = 1.5f
    edgeHighlightOpacity = 100f

    glassTint = android.graphics.Color.TRANSPARENT
    enableAdaptiveTint = true
    enableSensorHighlight = true
    enablePressEffect = true
    pressScale = 0.95f
    elasticity = 0.15f

    enableChromaticAberration = true
    enableChromaticDispersion = false
    aberrationIntensity = 2f
    displacementScale = 70f
    aberrationRedOffset = 0f
    aberrationGreenOffset = -0.05f
    aberrationBlueOffset = -0.10f

    enableShadow = false
    collectFrameStats = false
}
