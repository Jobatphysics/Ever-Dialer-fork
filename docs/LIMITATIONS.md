# Limitations

Status: **known risks, not all tested**.

- A scene can only sample draw content in its own coordinated window/layer; popups and separate windows may need separate scenes.
- Transforms such as rotation and scale between backdrop and consumer complicate coordinate projection.
- AGSL source compiles at runtime and must be validated on actual API 33+ devices/emulators.
- Local backdrop luminance analysis can itself be expensive; P0 will use conservative global/context inputs until a measured local approach is justified.
- Glass does not guarantee text contrast. The public API must expose readable fallback appearance and allow application content semantics to remain native.
