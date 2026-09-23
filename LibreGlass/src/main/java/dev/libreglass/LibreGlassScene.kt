package dev.libreglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toIntSize
import androidx.compose.foundation.layout.Box

/** Shared, immutable sampling source for sibling [LibreGlassSurface] instances. */
@Immutable
public class LibreGlassSceneState internal constructor(
    internal val backdrop: ImageBitmap? = null,
    internal val liveLayer: GraphicsLayer? = null,
) {
    internal var backdropRootOffset: IntOffset = IntOffset.Zero
}

private val LocalLibreGlassScene = compositionLocalOf<LibreGlassSceneState?> { null }

/**
 * Establishes a shared P0 backdrop. Supply an image representing content below the glass.
 * Live Compose backdrop recording is intentionally deferred to Phase 4.
 */
@Composable
public fun LibreGlassScene(
    backdrop: ImageBitmap,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = remember(backdrop) { LibreGlassSceneState(backdrop = backdrop) }
    androidx.compose.runtime.CompositionLocalProvider(LocalLibreGlassScene provides state) {
        Box(modifier = modifier) { content() }
    }
}

/**
 * Records the backdrop composable once into a retained Compose [GraphicsLayer]. Consumers must be
 * placed in [overlay], which is drawn above the provider and therefore cannot sample itself.
 */
@Composable
public fun LibreGlassLiveScene(
    modifier: Modifier = Modifier,
    backdrop: @Composable () -> Unit,
    overlay: @Composable () -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val state = remember(layer) { LibreGlassSceneState(liveLayer = layer) }
    androidx.compose.runtime.CompositionLocalProvider(LocalLibreGlassScene provides state) {
        Box(modifier) {
            Box(
                Modifier
                    .onGloballyPositioned { state.backdropRootOffset = it.positionInRoot().let { point -> IntOffset(point.x.toInt(), point.y.toInt()) } }
                    .drawWithContent {
                        layer.record(this@drawWithContent, layoutDirection, size.toIntSize()) {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(layer)
                    },
            ) { backdrop() }
            overlay()
        }
    }
}

internal val currentLibreGlassScene: LibreGlassSceneState?
    @Composable get() = LocalLibreGlassScene.current
