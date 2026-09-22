# Interaction

Status: **future research/implementation**.

Interaction is a separate input-to-uniform layer. Proposed states are idle, pressed, dragged, released, focused and disabled. A later surface may supply press position, intensity, scale and rim-energy values to the optical renderer; it must not make independent backdrop captures.

P0 has no interactive deformation. Later validation must include touch cancellation, nested scrolling, keyboard/focus operation, reduced motion, and disabled controls.
