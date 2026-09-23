# Performance

Status: **constraints identified; no measurements yet**.

The non-negotiable design constraint is one backdrop recording per scene per frame, shared by all surfaces. Surfaces should allocate their sampling layer only when size/effect bounds change, update uniforms rather than compile shaders, and inflate local sampling bounds enough for blur/refraction so edge clamping is not visible.

The current live scene records one retained provider layer and each consumer retains a local projected layer. It does not take screenshots or allocate a bitmap per frame. The five-tap shader prefilter and per-surface local projection still require GPU profiling; no 60/120 Hz claim is made.

Future measurement plan: Macrobenchmark scroll and animation scenarios with one and multiple surfaces; record frame timing, jank, allocations, GPU rendering profile, memory, shader warm-up, and battery/thermal behavior. The targets of 60 FPS and 120 Hz where supported are goals, not current benchmark results.
