# Backdrop recording

Status: **researched; implementation pending**.

P0 deliberately accepts a caller-owned `ImageBitmap` as its shared `LibreGlassScene` source. It proves coordinated sampling, surface-to-scene coordinate projection, shader parameterization, and a readable fallback without capturing arbitrary Compose content.

## Phase 4 finding

The Compose UI version resolved by this project exposes public retained-layer primitives: `rememberGraphicsLayer`, `GraphicsLayer.record`, and `DrawScope.drawLayer`. They can support the intended live design:

1. A backdrop provider records only its content into one retained `GraphicsLayer` during drawing.
2. Each surface records an inflated local layer that translates and draws that provider layer by reference.
3. The surface applies the API-appropriate blur/runtime effect to its local layer and draws its foreground content afterwards.

This preserves the no-self-sampling invariant: consumers must be siblings above, never descendants of, the provider recording.

## Decision

Do not replace the verified supplied-image P0 path until the retained-layer path has an instrumentation/demo host and is tested on API 33+ hardware or emulator. The next implementation should introduce `LibreGlassBackdrop` beside—not in place of—the P0 `ImageBitmap` scene source, then compare the two paths for coordinate correctness, scroll invalidation, allocation behavior, and effect bounds.
