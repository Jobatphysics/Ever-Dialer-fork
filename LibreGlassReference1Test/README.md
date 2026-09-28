# LibreGlass Reference #1 — Kyant0 / Backdrop

An isolated Android study app using only Kyant0's `io.github.kyant0:backdrop:2.0.1` API for its glass rendering. It does not depend on or modify `:LibreGlass` and is not integrated into `:app`.

## Reference snapshot

- Repository: [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)
- Branch inspected: `kmp`
- Branch head inspected: [`65ab177`](https://github.com/Kyant0/AndroidLiquidGlass/commit/65ab177) (dependency update, Aug 26 2026)
- Artifact: `io.github.kyant0:backdrop:2.0.1`
- Documentation: [Backdrop](https://kyant.gitbook.io/backdrop)
- License: Apache-2.0; this app consumes the published library and does not copy its source.

## Rendering architecture

A `rememberLayerBackdrop()` creates a shared retained Compose `GraphicsLayer`. `Modifier.layerBackdrop(backdrop)` draws the document and records that content into the layer. Each `drawBackdrop` consumer creates its own local offscreen graphics layer to apply effects and clip to its shape, while sampling the shared recorded backdrop. `LayerBackdrop` maps consumer coordinates into provider coordinates with `LayoutCoordinates.localPositionOf`; a window-position fallback is used if that mapping throws. The source is not captured as a bitmap or independently snapshotted for every surface.

Each consumer applies a Compose `RenderEffect` chain to its backdrop copy. The demo follows the reference catalog order: optional vibrancy, blur, then lens. `onDrawSurface` overlays tint, `Highlight` supplies the library's highlight decoration, and child content is drawn above the effect. The provider is drawn behind the surfaces, so the scene is scrollable and several consumers share the same source layer.

## Lens/refraction mechanism

The reference has no refractive-index parameter and does not implement Snell's law. The `lens` effect is an Android AGSL `RuntimeShader` guarded by Android API 33. Its rounded-rectangle SDF calculates signed distance inside the surface; an arc-like `circleMap` converts distance within `refractionHeight` into a displacement. The shader evaluates the recorded input at the displaced coordinate along the rounded-rectangle SDF gradient. `depthEffect` adds a normalized radial position bias to that gradient. Optional chromatic aberration performs seven backdrop evaluations at nearby coordinates and combines color channels. The amount is screen-space displacement, not IOR.

The shader accepts four corner radii and the effect accepts rounded-rectangle/corner-based shapes. A capsule and circle are represented with rounded corners; unsupported shape types throw from the lens effect. Its SDF and gradient are analytic and continuous over rounded corners, but the reference is not a physical thickness or Snell model.

## Blur, edge and appearance

Blur is Android Compose `BlurEffect`/platform `RenderEffect` (API 31+) with configurable pixel radius, and the backdrop effect scope tracks padding so the effect surface is not clipped. Lens distortion is boundary-local because the shader returns the original sample once the point is farther inside than `refractionHeight`. Highlight is a separate shape-clipped decoration. Tint is drawn by the consumer in `onDrawSurface` in the catalog example; opacity/color controls are effect-chain operations. There is no separate edge-width or IOR control.

## Controls in this test app

- Backdrop blur radius
- Lens refraction height and amount
- Backdrop opacity
- Highlight alpha
- Tint color and alpha
- Lens depth bias
- Chromatic aberration
- Vibrancy
- Show/hide glass and reset

No per-shape shader or extra optical model is used. The app deliberately does not invent an IOR slider. Default reset values are reproduction-scene starting values, not claimed to be library-wide defaults.

## Interaction, coordinates and limits

The library's generic `drawBackdrop` modifier does not itself implement touch deformation. The repository's demo components add that behavior at component level using press/drag state, damped animation, and `GraphicsLayerScope` translation/scale. This test focuses on optical behavior and uses the library modifier directly; slider input is live, but glass does not deform on touch.

`LayerBackdrop` requires layout coordinates and is intended for content in the same Compose coordinate space. The source comments acknowledge that outer transforms can produce an incorrect fallback mapping. Nested/exported/combined backdrops are supported by the library; independent windows and arbitrary transformed layouts are not guaranteed by this reproduction.

## API levels and performance notes

- Library Android min SDK: 21.
- Basic `RenderEffect` blur: API 31+; silently skipped below support.
- AGSL runtime-shader lens: API 33+; silently skipped below support.
- Test app min SDK: 26 (matching the surrounding project), compile SDK 36.
- GPU work includes a per-surface effect layer; blur is a platform render effect. Enabling chromatic aberration causes seven shader content samples instead of one. This app does not claim a frame rate; no profiler measurement has been made.

## Known reproduction differences

- Uses the published 2.0.1 library artifact rather than copying its source or the full catalog app.
- This is a single Android test scene rather than the reference's multiplatform catalog.
- Scene-specific tint, default values, and panel layout are ours; the rendering/effects come from the reference library.
- No visual screenshot validation was performed as part of this study setup.
