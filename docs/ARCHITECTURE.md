# Architecture

Status: **partially implemented**. The P0 module and supplied-image scene are implemented; live Compose backdrop recording remains deferred to Phase 4.

Update: `LibreGlassLiveScene` now records its backdrop subtree once into a retained `GraphicsLayer`. Glass surfaces in its sibling overlay project that layer from root coordinates into their local layers. The supplied-image `LibreGlassScene` remains available for deterministic testing/fallback use.

## Module boundary

Introduce `:LibreGlass` as an Android library module under `LibreGlass/`, namespace `dev.libreglass`. It will contain Compose rendering and pure Kotlin math in distinct packages initially, without an `app` dependency. A future extraction can split math/core from Android rendering only when P0 demonstrates a real need; premature multi-module splitting would make the first prototype harder to validate.

## Scene model

`LibreGlassScene` owns one recorded backdrop layer and coordinate mapping. Backdrop content is rendered below it; glass surfaces are sibling overlays that request only their inflated local sampling region. A scene must not include its own consumers in its recording, preventing recursive/self sampling.

`LibreGlassSurface` is deliberately not finalized as public API until P0 validates the provider/consumer lifecycle. Internally the design separates:

- scene: recording, invalidation, coordinates, capability selection;
- optics: SDF/mask, blur, refraction, dispersion and composition;
- appearance: tint, highlight, rim, shadow and readable-content policy;
- interaction: state and spring values only; it supplies uniforms but does not capture/draw the scene;
- morphology: shape/parameter interpolation and future SDF unions.

## P0 render order to validate

1. Record the shared backdrop once.
2. Project the local, inflated region into a surface layer.
3. Apply platform blur and color adaptation.
4. In AGSL, derive SDF edge distance and normal, then refract/disperse the sampled content.
5. Apply adaptive tint, subtle rim/highlight and mask in a premultiplied-alpha-safe composition.
6. Draw surface content above the material.

This is a hypothesis, not a final pipeline. P0 will compare alternatives and document results.

## Initial module layout

```text
LibreGlass/
  build.gradle
  src/main/java/dev/libreglass/{scene,optics,appearance,interaction,morph}/
  src/test/java/dev/libreglass/
docs/
```

A standalone demo module is deferred until the renderer compiles; it will depend on `:LibreGlass`, never on `:app`.
