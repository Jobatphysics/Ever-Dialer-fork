package com.android.libredialer.debug

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.example.liquidglass.GlassMaterial
import com.example.liquidglass.LiquidGlassView
import kotlin.math.min

class LiquidGlassProofOfConceptActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density + 0.5f).toInt()

        val stage = FrameLayout(this).apply {
            setWillNotDraw(false)
            setBackgroundColor(Color.BLACK)
        }

        // Match the demo's native sibling order: real background first, glass above it.
        stage.addView(
            PatternedBackdropView(this),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val label = TextView(this).apply {
            text = "Liquid Glass"
            textSize = 21f
            typeface = android.graphics.Typeface.create(
                "sans-serif-medium",
                android.graphics.Typeface.NORMAL
            )
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setShadowLayer(10f, 0f, 2f, 0x66000000)
        }

        val glass = LiquidGlassView(this).apply {
            enableDynamicBackground = true
            useShaderPipeline = true
            material = GlassMaterial.REGULAR
            cornerRadius = 999f
            refractionHeight = 200f
            bevelWidth = 56f
            dispersionStrength = 0.16f
            blurAmount = 0.055f
            saturation = 150f
            enableEdgeHighlight = true
            enableSensorHighlight = true
            enableAdaptiveTint = true
            glassAppearanceListener = { overLight ->
                label.setTextColor(if (overLight) 0xDE000000.toInt() else Color.WHITE)
            }
            setOnTouchListener(GlassDragHandler())
            addView(
                label,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        // No backdropSource override: LiquidGlassView samples its direct FrameLayout parent,
        // just like the demo, and the library excludes this glass sibling during capture.
        stage.addView(
            glass,
            FrameLayout.LayoutParams(dp(300), dp(112), Gravity.CENTER)
        )
        setContentView(stage)
    }

    private inner class GlassDragHandler : View.OnTouchListener {
        private var offsetX = 0f
        private var offsetY = 0f

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    offsetX = view.x - event.rawX
                    offsetY = view.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    view.x = event.rawX + offsetX
                    view.y = event.rawY + offsetY
                }
            }
            return false
        }
    }

    private class PatternedBackdropView(activity: Activity) : View(activity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val density = resources.displayMetrics.density
        private val colors = intArrayOf(
            Color.rgb(27, 15, 75),
            Color.rgb(15, 112, 148),
            Color.rgb(188, 48, 115)
        )

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            paint.shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                colors,
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null

            val cell = 44f * density
            val radius = 12f * density
            val rows = (height / cell).toInt() + 2
            val columns = (width / cell).toInt() + 2
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    val left = column * cell + if (row % 2 == 0) 0f else cell / 2f
                    val top = row * cell
                    val color = colors[(row + column) % colors.size]
                    paint.color = color
                    canvas.drawRoundRect(
                        left + 3f * density,
                        top + 3f * density,
                        left + cell - 3f * density,
                        top + cell - 3f * density,
                        radius,
                        radius,
                        paint
                    )
                    paint.color = Color.WHITE
                    paint.alpha = 210
                    val dotRadius = min(cell * 0.12f, 7f * density)
                    canvas.drawCircle(left + cell / 2f, top + cell / 2f, dotRadius, paint)
                    paint.alpha = 255
                }
            }
        }
    }
}
