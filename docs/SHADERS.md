# Shaders

Status: **partially implemented**. P0 contains an independently authored AGSL program for API 33+; device-level shader validation remains incomplete.

Android 13/API 33 is the first advanced tier because `RuntimeShader`/AGSL is available there. P0 creates a runtime shader per retained surface and updates uniforms as its dimensions/optics change. Its five-tap backdrop prefilter is GPU-side; replacing it with platform blur over the future recorded `GraphicsLayer` remains a Phase 4/5 investigation.

The shader contains independently authored rounded-rect SDF, finite-difference normals, edge sampling, RGB offset, tint, highlight/rim and lower-edge shading. CPU mirrors cover the SDF, lens profile and dispersion symmetry. Shader structural/device tests remain incomplete. No reference-project source, shader text, or Apple source was copied.
