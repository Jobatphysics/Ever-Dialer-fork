# Compatibility

Status: **researched/proposed**.

| Capability | Initial proposed behavior |
| --- | --- |
| API 33+ | AGSL lensing/refraction, optional dispersion, blur/tint/rim |
| API 31–32 | RenderEffect blur/tint/rim with geometric clipping; no AGSL optics |
| API 26–30 | translucent/tinted material and drawn rim; no live blur requirement |

LibreGlass's host project min SDK is 26, so the first library will not claim API 21 support. A caller or accessibility policy may lower the tier, never raise it above device capability. Cross-window/dialog/popup sampling and non-translation transforms are known areas requiring explicit P0 tests before support is claimed.
