# Shaders

Status: **planned**, no shader source exists.

Android 13/API 33 is the first advanced tier because `RuntimeShader`/AGSL is available there. A single compiled AGSL program per renderer should receive changing uniforms rather than source recompilation per frame. Platform `RenderEffect` blur is the first candidate for prefiltering because it avoids CPU pixel work.

The first shader must contain only independently authored SDF, normal estimation, sampling and composition code. CPU math mirrors will be created for every nontrivial formula and unit tested before visual tuning. Shader structural tests will validate uniform names and bounded loops. No reference-project source, shader text, or Apple source will be copied.
