package com.android.libredialer.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.libredialer.controller.util.PreferenceManager
import org.koin.compose.koinInject

private val SimCardNotchShape = GenericShape { size, _ ->
    val notch = size.minDimension * 0.42f
    moveTo(notch, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    lineTo(0f, notch)
    close()
}

@Composable
fun SimSlotBadge(
    slot: Int,
    modifier: Modifier = Modifier,
    shape: Shape = SimCardNotchShape,
    overrideColor: Color? = null,
    fontSize: TextUnit? = null
) {
    val color = overrideColor ?: run {
        val prefs = koinInject<PreferenceManager>()
        val settingsVersion by prefs.settingsChanged.collectAsState()
        val sim1Color = remember(settingsVersion) {
            Color(prefs.getInt(PreferenceManager.KEY_SIM1_COLOR, PreferenceManager.DEFAULT_SIM1_COLOR))
        }
        val sim2Color = remember(settingsVersion) {
            Color(prefs.getInt(PreferenceManager.KEY_SIM2_COLOR, PreferenceManager.DEFAULT_SIM2_COLOR))
        }
        if (slot == 0) sim1Color else sim2Color
    }

    if (fontSize != null) {
        Box(
            modifier = modifier.clip(shape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (slot == 0) "1" else "2",
                color = Color.White,
                fontSize = fontSize,
                lineHeight = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
    } else {
        BoxWithConstraints(
            modifier = modifier
                .size(width = 18.dp, height = 21.dp)
                .clip(shape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            val resolvedFontSize = (maxHeight.value * 0.52f).sp
            Text(
                text = if (slot == 0) "1" else "2",
                color = Color.White,
                fontSize = resolvedFontSize,
                lineHeight = resolvedFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
    }
}
