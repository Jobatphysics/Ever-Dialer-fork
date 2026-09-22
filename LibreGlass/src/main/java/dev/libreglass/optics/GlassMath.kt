package dev.libreglass.optics

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Pure Kotlin optical helpers mirrored by the P0 AGSL program. */
public object GlassMath {
    public data class Vec2(val x: Float, val y: Float) {
        public operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
        public operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
        public operator fun times(value: Float): Vec2 = Vec2(x * value, y * value)
        public fun length(): Float = sqrt(x * x + y * y)
        public fun normalized(): Vec2 {
            val length = length()
            return if (length <= EPSILON) Zero else this * (1f / length)
        }

        public companion object { public val Zero: Vec2 = Vec2(0f, 0f) }
    }

    /** Signed distance to a rounded rectangle centred at the origin. */
    public fun roundedRectSdf(point: Vec2, halfSize: Vec2, radius: Float): Float {
        val clampedRadius = radius.coerceIn(0f, min(halfSize.x, halfSize.y))
        val q = Vec2(kotlin.math.abs(point.x), kotlin.math.abs(point.y)) -
            Vec2(halfSize.x - clampedRadius, halfSize.y - clampedRadius)
        val outside = Vec2(max(q.x, 0f), max(q.y, 0f)).length()
        return outside + min(max(q.x, q.y), 0f) - clampedRadius
    }

    /** 0 in the flat centre and 1 at the outer edge of a lens band. */
    public fun rimProgress(signedDistance: Float, width: Float): Float {
        if (width <= EPSILON) return 0f
        return ((width + signedDistance) / width).coerceIn(0f, 1f)
    }

    /** Convex lens profile with zero displacement at the inner band edge. */
    public fun lensProfile(progress: Float): Float {
        val x = progress.coerceIn(0f, 1f)
        return 1f - sqrt(max(0f, 1f - x * x))
    }

    public fun dispersionOffsets(direction: Vec2, amount: Float): Triple<Vec2, Vec2, Vec2> {
        val offset = direction.normalized() * amount
        return Triple(offset, Vec2.Zero, offset * -1f)
    }

    private const val EPSILON: Float = 0.0001f
}
