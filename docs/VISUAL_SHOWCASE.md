# LibreGlass Visual Showcase

Status: **build-tested, unit-tested, lint-tested; not physical-device-tested**.

Run `:LibreGlassShowcase:assembleDebug` and install the resulting independent app. It is not linked to LibreDialer.

The showcase presents a scrollable Field Notes document as a `LibreGlassLiveScene` backdrop. Its floating navigation capsule, search field, text toolbar, circular tools, comparison samples, material control panel, and bottom navigation are sibling `LibreGlassSurface` instances. This placement prevents a glass surface from recording itself.

## Live tuning

The Material lab updates all surfaces immediately. It includes labeled percentage sliders for transparency, refraction, horizontal edge width, vertical edge width, and blur, plus horizontal/vertical curvature and dispersion. Reset, Clear Glass, Soft Glass, Strong Lens, and Frosted presets modify the same `LibreGlassOptics` value; the demo does not use per-component shaders.

## Interaction

Clickable surfaces use a shared press implementation: a small GPU scale compression, boosted rim/highlight, and Compose spring release. The floating document tools demonstrate it.

## Reading the material

`transparency = 1` removes only the tint wash. It does not disable the sampled scene, boundary refraction, RGB split, rim, or highlight. Refraction is strongest near the rounded boundary and is driven by the SDF normal, directional edge widths, and directional curvature. The centre remains mostly flat.

Device validation is still required for actual scrolling, animation smoothness, AGSL compilation, and visual correctness on API 33+. API 26–32 use a coordinated source layer with a tint fallback rather than the runtime shader.
