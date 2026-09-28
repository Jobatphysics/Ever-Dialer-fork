# Reference #2 analysis — Abdullajon1881/LiquidGlass

## Repository snapshot

- Repository: [Abdullajon1881/LiquidGlass](https://github.com/Abdullajon1881/LiquidGlass)
- Branch: `main`
- Inspected commit: [`72ad05c49628ea2270116b2943629cd81c0cc496`](https://github.com/Abdullajon1881/LiquidGlass/commit/72ad05c49628ea2270116b2943629cd81c0cc496), 2026-06-13.
- Main modules: `liquidglass-core` and `liquidglass-compose`. `liquidglass-view`, React Native package, and `sample` were excluded from this Compose experiment.
- Distribution: the README says Maven Central artifacts are configured but not yet published. This experiment contains the unmodified core/Compose source trees under `reference/`, with only standalone build scripts adapted to this repository. The upstream Apache-2.0 license is included at `reference/LICENSE`.

## Pipeline and backdrop architecture

`Modifier.liquidGlassProvider(state)` is a draw modifier node. On each draw, it records the provider content into one retained `GraphicsLayer` and then draws that layer normally. Every independent `liquidGlass` consumer, and each `LiquidGlassContainer`, reads that shared layer by reference and records the relevant provider slice into its own inflated glass `GraphicsLayer`.

Consumer and provider coordinates are mapped at draw time with `provider.localPositionOf(consumer, Offset.Zero)`. The provider increments `positionTick` when globally positioned; consumers read that snapshot value so fixed glass can refresh when its provider moves. The provider is re-recorded from UI content, not captured as a bitmap. It is live for scroll/content invalidation, and multiple consumers share the same recorded source. Both layers must be in the same window.

Glass surfaces must be siblings above the provider, outside its subtree. Otherwise recording the provider could include its own rendered glass and produce recursive/self-sampling. This app follows that rule: document provider first, overlay glass and merge cluster afterward.

Each standalone surface owns its own glass layer. A merge container instead paints one scene layer for all registered member shapes. Child geometry is collected through `onGloballyPositioned` and converted to container-local positions using `LayoutCoordinates.localPositionOf`.

## RenderEffect chain and bounds

The shader tier creates the backdrop prep effect with `RenderEffect.createBlurEffect(radius, radius, CLAMP)` and a saturation `ColorMatrixColorFilter` (`ColorMatrix.setSaturation`). The AGSL `RuntimeShader` effect wraps that prepared content. The chain is therefore blur → saturation → AGSL shader. If blur is zero, the blur stage is omitted; saturation near 1 is omitted.

Before applying effects, `GlassPainter` records the shared provider layer with `inflate - positionInProvider` translation. Its inflation is:

`ceil(2 × blurPx + abs(refractionAmountPx) + 0.5 × mergeSmoothingPx) + 4 px`.

The surface then draws from the offset inflated layer. The shader tier uses the SDF mask as its antialiased boundary. The blur fallback clips the layer to a union `Path`; the scrim tier clips the unfiltered layer.

## SDF, shapes, and merging

The shape model is a rounded rectangle per member. Circles and capsules are represented as rounded rectangles with corner radius equal to half the smaller dimension. `GlassShapePacker` packs each as `(centerX, centerY, halfWidth, halfHeight)` and one radius. Shader arrays have eight slots; unused shapes are parked at `(-1,000,000, -1,000,000)` with tiny extents and cannot affect the scene.

For a point `p`, a shape's rounded-box SDF uses `q = abs(p) - halfSize + radius`, then `min(max(q.x,q.y),0) + length(max(q,0)) - radius`. Distances are negative inside and positive outside.

The union uses polynomial smooth-min with `k = max(spacing, 0.0001)`, `h = clamp(0.5 + 0.5 × (b-a)/k, 0, 1)`, `smin(a,b,k) = mix(b,a,h) - k×h×(1-h)`. The `LiquidGlassContainer.spacing` value is passed as this smoothing `k`, so it controls the blend width; it is not an explicit physical gap threshold. Up to eight shapes are combined into one field and one material mask. Enter/exit shapes morph with an `Animatable` scale (0.4→1) using the library's spring specs.

The shader normal is a central finite difference of the complete scene SDF at a one-pixel step: `g=(sd(p+(1,0))-sd(p-(1,0)), sd(p+(0,1))-sd(p-(0,1)))`, normalized if its length exceeds `1e-4`. This makes normals follow smooth-min necks; it is not an analytic per-shape normal.

## Edge refraction, dispersion, tint, rim, and grain

The mask is `1 - smoothstep(-1, 1, sd)`. Pixels outside it return transparent. `edgeDist=max(-sd,0)` is the inward distance from the SDF boundary. Only pixels with `edgeDist < refractionHeight` are displaced; the center past that band samples the original coordinate.

The lens profile is `x=1-edgeDist/max(height,epsilon)`, `lens=1-sqrt(max(1-x*x,0))`. It is 1 at the boundary and 0 at the inner end of the band. The sample coordinate is `p - normal × amount × lens`, so positive amount bends samples toward the shape center and negative amount bends outward. Distortion is strongest at the edge and fades continuously toward a relatively calm center. There is no fixed screen-space direction.

There is no `IOR`, `refractiveIndex`, Snell/eta ratio, Fresnel, or incident-angle refraction calculation in core/Compose. **Reference #2 uses an SDF-based lens/refraction model rather than an explicit physical IOR.** Refraction is controlled by `GlassRefraction.height` and `.amount`.

If chromatic aberration is above 0.001 after scaling by lens and amount, the shader samples three times: red at `sampleCoord - normal×shift`, green at the center, and blue at `sampleCoord + normal×shift`, where `shift = chromaticAberration × lens × abs(refractionAmount) × 0.12`. Otherwise it samples once. Thus the chromatic separation is edge weighted.

Tint uses a straight-alpha `mix(color.rgb, tint.rgb, tint.a)` before lighting. `tint.alpha` is color-mix strength, not final material alpha. The public `GlassStyle` has no independent surface/final-alpha parameter. The API likewise has no IOR control.

The shader rim is `1 - smoothstep(0, highlightWidth, edgeDist)`. Orientation comes from `pow(abs(dot(normal, lightDir)), 1.5)`. It adds a thin specular contribution with `highlightAlpha`; pressing boosts it by `1 + pressAmount×0.6`. Absolute dot means both facing and opposing contours can light. The default `GlassHighlight` is width 2.5 dp, alpha 0.55, angle 245 degrees. A hash of pixel coordinates creates a single noise value, centered around zero and scaled by `noiseAlpha`, to reduce banding.

## Gel press interaction

`GlassStyle.isInteractive` enables the pointer behavior in the modifier node. On press an `Animatable` moves from 0 to 1 with spring `(dampingRatio=0.9, stiffness=1200)`; release/cancel returns with `(0.45, 280)`. The whole draw scope scales by up to 2.5% (`PRESS_SCALE_MAX=0.025`). The shader bulge uses `toPress=p-pressPoint`, Gaussian `exp(-dot(toPress,toPress)/8000)`, and subtracts `toPress × pressAmount × 0.08 × falloff` from the sample coordinate. The same progress boosts rim brightness. Containers have one shared press point/progress routed from interactive `glassEffect` children.

Only the on/off `isInteractive` flag is public. Press strength, Gaussian width, press-in/release spring parameters, and maximum scale are internal constants, not tunable material parameters.

## Render tiers

`GlassRenderTier.select` chooses the best supported tier, and requested/local settings can only lower it:

- `SHADER`, API 33+: blur + saturation + AGSL lens, dispersion, merge field, press bulge, shader rim and grain.
- `BLUR`, API 31–32: RenderEffect blur + saturation, geometric clip, optional tint wash and drawn rim; no refraction or merging.
- `SCRIM`, API 21+: unblurred recorded backdrop, geometric clip, translucent fallback scrim and drawn rim; no blur, refraction or merging.

The app's min SDK is 21. The connected validation device is API 36, so the shader tier should be available there; the tier picker can force lower tiers for study.

## Parameters exposed in the test app

- `GlassStyle.blurRadius`
- `GlassRefraction.height` and `.amount` (including negative outward amount, which the source permits)
- `GlassStyle.saturation`
- `GlassStyle.chromaticAberration`
- `GlassHighlight.width`, `.alpha`, `.lightAngleDegrees`
- `GlassStyle.tint` via color choices and its alpha/mix strength
- `GlassStyle.noiseAlpha`
- `GlassShape` probe selection and rounded-rectangle corner radius
- `GlassStyle.isInteractive`
- `LiquidGlassContainer.spacing` (smooth-min `k`), plus scene gap presets that move three actual container children
- requested render tier and show/hide switch
- reset to upstream `GlassStyle.Regular`-equivalent constructor values

Explicitly labeled **NOT PRESENT IN REFERENCE**: physical IOR/Snell control and independent final surface opacity. Tint alpha changes the tint mix only. The library offers no separate alpha control that leaves refraction/rim unchanged.

## Performance notes and limitations

- One provider layer recording per draw/frame is shared; every standalone glass owns an inflated layer, while one container uses one scene layer.
- AGSL `RuntimeShader` is initialized once per painter/node and uniforms are refreshed during draws; the effect chain is rebuilt around it.
- Normal calculation performs four SDF evaluations per shaded fragment (central differences). Base shader backdrop sampling is one; chromatic mode is three. Shader loops use eight fixed shape slots and smooth-min operations for active shapes.
- Inflation grows with blur, absolute refraction amount, and merge smoothing. Large/high settings increase the work area.
- This study does not claim measured frame rate. It uses static test text and UI controls; animated backdrop behavior is not performance-profiled.
- No screenshot-based visual assertion is run. Device installation/launch validates packaging/activity startup; parameter effects remain for visual manual inspection on device.
- Shader appearance can differ by GPU/driver. Text composited as child content remains above and untouched by the backdrop RenderEffect.

## What this experiment does not do

It does not alter or depend on the existing LibreGlass module, does not depend on Reference #1, and does not replace any upstream optical equation. It is an Android-only controlled study of the upstream core/Compose source at the stated commit.

## Compose API inventory and showcase wiring audit

The vendored `liquidglass-core/src` and `liquidglass-compose/src` Kotlin source trees were compared with the checked-out upstream snapshot at `72ad05c49628ea2270116b2943629cd81c0cc496`; their source files match. Only standalone module build scripts and package metadata were added for this workspace. The separate `liquidglass-view`, React Native package, and upstream sample app are not included in this Compose study.

Every configurable public Compose material/container option in that source snapshot is now represented in the Reference #2 Material Lab:

| Upstream API | Demo control | Renderer path |
| --- | --- | --- |
| `GlassStyle.Regular`, `Clear`, `prominent(tint)` | Preset chips; choosing a preset loads its actual upstream values | State is rebuilt into the `GlassStyle` supplied to each reference component/modifier |
| `GlassStyle.shape`; `GlassShape.Capsule`, `Circle`, `RoundedRectangle(cornerRadius)` | Shape chips and corner-radius slider | Packed as rounded-rectangle SDF inputs; circles/capsules resolve to half the smaller dimension |
| `GlassStyle.blurRadius` | Blur radius slider | `GlassPainter` → `buildBackdropPrepEffect` → `RenderEffect.createBlurEffect` |
| `GlassRefraction.height`, `.amount` | Independent height and amount sliders | `GlassPainter` uniforms → AGSL edge-distance lens profile and normal displacement |
| `GlassStyle.saturation` | Saturation slider | `RenderEffect` color-matrix pre-pass before shader sampling |
| `GlassStyle.chromaticAberration` | Chromatic aberration slider | AGSL uniform; enables edge-weighted RGB channel samples |
| `GlassHighlight.width`, `.alpha`, `.lightAngleDegrees` | Three independent sliders | Highlight/rim uniforms or fallback rim drawing |
| `GlassStyle.tint` / `tinted(color)` | Color swatches plus tint mix slider | `GlassStyle.tint` color and alpha are passed to the shader or fallback wash |
| `GlassStyle.noiseAlpha` | Dither/noise slider | AGSL noise uniform |
| `GlassStyle.isInteractive` / `interactive(enabled)` | Interactive gel toggle | Compose pointer press/release animation → scale, press-point/bulge and rim boost |
| `GlassStyle.fallbackScrim` | Scrim color swatches | `GlassPainter.drawScrimTier`; only active in SCRIM tier |
| `LiquidGlassContainer.spacing` | Merge-spacing slider, plus container enable switch | Container parameter → smooth-min width `k`; it does not create a second merge renderer |
| `Modifier.glassEffect` child shape/id/interactive options | Two-button, three-button and FAB + two actions layouts; separation/approach/merge/move-apart presets | Registered children are collected by the upstream container and drawn as its union |
| `LiquidGlassProviderState.requestedTier` and `LocalLiquidGlassTier` | AUTO / SHADER / BLUR / SCRIM selector; current device maximum/request/active tier are displayed separately | The demo sets the upstream provider cap; the library still clamps it to device capability |

The showcase also uses the upstream `GlassCard`, `GlassButton`, `GlassIconButton`, `GlassSurface`, and `GlassBottomBar` composables alongside direct `Modifier.liquidGlass` samples and the upstream `LiquidGlassContainer`/`glassEffect` merge path.

### Control-path checks

- Each Lab slider updates `LabSettings`; `toStyle(shape)` constructs a new upstream `GlassStyle` and `GlassRefraction`/`GlassHighlight` value. The library modifier node compares the new style and invalidates its draw.
- Blur, saturation, refraction height/amount, chromatic aberration, tint, noise, and highlight values are read by `GlassPainter` and reach their respective `RenderEffect` input or AGSL uniforms. Height and amount remain separate. Chromatic aberration does not rewrite refraction amount.
- Tint alpha is disabled until a tint color is selected. Selecting a color chooses a visible initial mix if needed. The upstream API has no independent final material opacity.
- Merge spacing is disabled when the container is off; when enabled it is passed directly to `LiquidGlassContainer.spacing`. The container-off path renders individual reference glass children, so the comparison remains meaningful.
- Preset chips load the upstream preset values before the regular sliders are applied. `prominent` receives the selected tint or its default blue accent. Preset state indicates which baseline was selected; subsequent slider edits remain live.
- The moving marker is actual document content recorded by the provider. A separate position slider moves a glass probe through the parent layout to exercise the source's coordinate projection. These are scene diagnostics, not invented material parameters.

### Confirmed options that do not exist

- No explicit IOR, `refractiveIndex`, Snell/eta refraction, Fresnel parameter, or physical transmission coefficient exists in the inspected core/Compose source. The renderer uses SDF lens displacement.
- There is no public final surface alpha/transparency control. Tint alpha is tint mix strength only.
- Gel press spring constants, press scale maximum, and Gaussian bulge radius/strength are implementation constants, not public numeric controls; the demo exposes only `isInteractive`.
- The Compose `GlassShape` sealed interface contains only capsule, circle, and rounded rectangle. There are no independent ellipse, polygon, or arbitrary path shape APIs.

## Resume audit changes and build notes

The audit found that the earlier demo did not expose the upstream material presets or fallback scrim color and did not demonstrate several upstream foundation composables. It also had no container-on/off comparison or moving backdrop/position stress controls. These are now in the isolated showcase. Reset returns to `GlassStyle.Regular` values (including 16 dp container spacing); API 21-compatible Compose/activity versions from the upstream version catalog are used, while the inspected upstream library's tier logic remains API 33+/31–32/21+.

The Compose module's UI smoke test task did not finish during the previous run and is inconclusive. Core JVM tests passed. The Reference #2 app APK built successfully before this audit update; the audited source must be rebuilt and revalidated before claiming completion.
