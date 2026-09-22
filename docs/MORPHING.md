# Morphing

Status: **future research/implementation**.

The eventual path is to interpolate rect position, size, radius, tint and optical parameters, then use SDF smooth-union only for a bounded cluster of analytic shapes. A bitmap mask union is intentionally not the initial path because it compromises normal quality and repeated capture cost.

Materialization should animate lensing, highlight and distortion with opacity as a supporting channel, not merely cross-fade a blurred card. This is a visual principle from Apple's public presentation; an Android behavior has not yet been prototyped.
