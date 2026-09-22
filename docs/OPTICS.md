# Optics

Status: **researched/proposed**, not prototyped.

P0 will use a rounded-rectangle signed distance field (SDF). Its signed edge distance creates an anti-aliased mask; finite-difference SDF gradients provide a screen-space normal. The normal and a rim-distance falloff will displace backdrop sampling near the boundary, approximating a thick convex glass edge while leaving the centre mostly stable. This is perceptual lensing, not physical ray tracing.

Chromatic dispersion, if enabled, will sample R/G/B at tiny different offsets along the refraction vector. It must decay away from the rim and be disabled or clamped for accessibility/readability. Tint, saturation, brightness and local luminance response are subsequent appearance controls—not substitutes for refraction.

P0 controls: blur, lens strength, refraction width, distortion, dispersion, saturation, brightness, tint, highlight, rim and shadow. Parameter ranges, normalization, and expected units remain **incomplete** until visual tests are run.
