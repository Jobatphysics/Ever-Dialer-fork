package dev.libreglass.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.libreglass.LibreGlassScene
import dev.libreglass.LibreGlassSurface

/** Standalone P0 showcase for an independent demo module or sample host. */
@Composable
public fun LibreGlassPrototype() {
    val backdrop = remember { prototypeBackdrop() }
    LibreGlassScene(backdrop, Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            LibreGlassSurface(Modifier.padding(32.dp)) {
                BasicText("LibreGlass P0", Modifier.padding(24.dp), style = TextStyle(color = Color.White))
            }
        }
    }
}

private fun prototypeBackdrop() = Bitmap.createBitmap(1080, 720, Bitmap.Config.ARGB_8888).apply {
    Canvas(this).drawPaint(Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), intArrayOf(0xff1e2a78.toInt(), 0xffc13c81.toInt(), 0xfff5bb4b.toInt()), null, Shader.TileMode.CLAMP)
    })
}.asImageBitmap()
