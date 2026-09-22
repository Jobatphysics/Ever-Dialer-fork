package dev.libreglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.layout.Box

/** Shared, immutable sampling source for sibling [LibreGlassSurface] instances. */
@Immutable
public class LibreGlassSceneState internal constructor(internal val backdrop: ImageBitmap)

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
    val state = remember(backdrop) { LibreGlassSceneState(backdrop) }
    androidx.compose.runtime.CompositionLocalProvider(LocalLibreGlassScene provides state) {
        Box(modifier = modifier) { content() }
    }
}

internal val currentLibreGlassScene: LibreGlassSceneState?
    @Composable get() = LocalLibreGlassScene.current
