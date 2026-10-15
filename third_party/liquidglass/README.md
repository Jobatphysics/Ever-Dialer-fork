# LibreDialer LiquidGlass source

This module vendors the `liquidglass` library from QWEA0/Liquid-Glass-Android
v2.0.11 under its MIT license. The only rendering change is a 60-degree
clockwise rotation of the shader's specular light direction in
`GlassLensRenderer`; the original native libraries from the v2.0.11 AAR are
retained.
