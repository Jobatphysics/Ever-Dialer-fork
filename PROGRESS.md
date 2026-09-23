# LibreGlass progress

## Completed
- Phase 0: repository and Gradle inspection.
- Phase 1: initial research of the two named Android projects and Apple public material.
- Phase 2: initial architecture decision and documentation.
- Phase 3: P0 implementation — independent library module, shared supplied-image scene, rounded SDF math, API 33+ AGSL lensing/prefilter, fallback material, and JVM math tests.
- LibreGlass Visual Milestone: retained live Compose backdrop path, directional optical style controls, press interaction, and independent document showcase app.

## In Progress
- Device visual validation, backdrop-scroll correctness validation, and GPU profiling.

## Next
- Instrument and validate the Visual Showcase on API 33+ hardware/emulator; then optimize backdrop inflation and effect bounds.

## Remaining
- Phases 3–20: prototype, shared backdrop, optics, interaction, morphing, accessibility, performance/device validation, and release work.

## Research
- Recorded in `docs/RESEARCH.md`; source-specific licensing is in `docs/LICENSING.md`.
- Phase 4 feasibility findings are recorded in `docs/BACKDROP.md`.

## Architecture
- A shared scene/backdrop provider with sibling glass consumers is selected for the prototype. See `docs/ARCHITECTURE.md`.

## Implementation
- P0 source is present in `LibreGlass/`. Nothing in `app/` depends on LibreGlass.

## Build
- `:LibreGlass:assembleDebug`, `:LibreGlass:testDebugUnitTest`, `:LibreGlass:lintDebug`, and `:LibreGlassShowcase:assembleDebug` passed for the Visual Milestone; physical-device testing has not occurred.

## Performance
- Not benchmarked. P0's five-tap shader prefilter is intentionally bounded but requires GPU profiling before it is retained.

## Known Limitations
- Android runtime-shader capability is API-dependent; live backdrop sampling has coordinate/window constraints. See `docs/COMPATIBILITY.md` and `docs/LIMITATIONS.md`.

## Git
- Active branch: `libreglass-development`.
- Existing uncommitted LibreDialer UI work predates this work and is deliberately untouched.
