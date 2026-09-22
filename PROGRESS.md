# LibreGlass progress

## Completed
- Phase 0: repository and Gradle inspection.
- Phase 1: initial research of the two named Android projects and Apple public material.
- Phase 2: initial architecture decision and documentation.

## In Progress
- No rendering code is in progress. P0 scope and acceptance criteria are documented.

## Next
- Add the independent `:LibreGlass` library skeleton and a P0 demo only after the module plan is approved through this research stage.

## Remaining
- Phases 3–20: prototype, shared backdrop, optics, interaction, morphing, accessibility, performance/device validation, and release work.

## Research
- Recorded in `docs/RESEARCH.md`; source-specific licensing is in `docs/LICENSING.md`.

## Architecture
- A shared scene/backdrop provider with sibling glass consumers is selected for the prototype. See `docs/ARCHITECTURE.md`.

## Implementation
- Not started. Nothing in `app/` depends on LibreGlass.

## Build
- Not run for LibreGlass; no module exists yet.

## Performance
- Not benchmarked. Performance constraints and measurements-to-add are documented.

## Known Limitations
- Android runtime-shader capability is API-dependent; live backdrop sampling has coordinate/window constraints. See `docs/COMPATIBILITY.md` and `docs/LIMITATIONS.md`.

## Git
- Active branch: `libreglass-development`.
- Existing uncommitted LibreDialer UI work predates this work and is deliberately untouched.
