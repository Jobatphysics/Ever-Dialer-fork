# Research record

Status: **researched**. No LibreGlass rendering behavior has been prototyped, tested, or benchmarked yet.

## Android references

Kyant0's [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) is an Apache-2.0 Compose Multiplatform effect library. It exposes a low-level backdrop facility rather than prescribing high-level controls. Its modular `backdrop` directory and examples support separating backdrop capture from component design. LibreGlass will preserve that separation but will not copy its source or API.

Abdullajon1881's [LiquidGlass](https://github.com/Abdullajon1881/LiquidGlass) is Apache-2.0 and documents a provider that records a `GraphicsLayer` once per frame, consumer layers that sample it by reference, platform blur/saturation before an AGSL shader, SDF normals, edge refraction, subtle RGB offsets, and tiered fallbacks. Its documented limitations—no cross-window sampling and restricted transformed coordinate spaces—are relevant risks to validate independently. Its useful architectural lesson is the shared provider, not a source-level dependency.

## Apple visual reference

Apple describes Liquid Glass as a dynamic digital material that bends and shapes light, adapts to context, and treats motion and visuals as one system in [Meet Liquid Glass](https://developer.apple.com/videos/play/wwdc2025/219/). The initial design reference therefore prioritizes: visible layering without obscuring content, restrained lensing at edges, contextual tint/contrast, rounded touch-friendly forms, and materialization by increasing optical behavior rather than only fading alpha. Apple's [announcement](https://www.apple.com/in/newsroom/2025/06/apple-introduces-a-delightful-and-elegant-new-software-design/) additionally identifies specular highlights, layered materials, and light/dark/tinted adaptation as visual cues.

Apple material is proprietary and is not an implementation source. LibreGlass will independently implement only general visual/interaction principles.

## Initial conclusions

- Shared backdrop recording is required; per-surface full-screen capture is rejected.
- API 33+ is the practical first target for AGSL `RuntimeShader`; API 31–32 can use blur/tint; lower levels need a readable static material fallback.
- An analytic rounded-rectangle/capsule SDF is a suitable P0 geometry because it gives stable mask, edge distance, normal and interpolation inputs without bitmap masks.
- Blur alone is insufficient: P0 must demonstrate localized edge displacement, controlled tint, rim/highlight, and legible foreground content.
- Interaction and merging remain later phases, separate from the P0 optical renderer.
