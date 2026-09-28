package dev.libreglass.reference1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.opacity
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ReferenceLab()
            }
        }
    }
}

private data class LabSettings(
    val blur: Float = 2f,
    val refractionHeight: Float = 20f,
    val refractionAmount: Float = 26f,
    val opacity: Float = 1f,
    val highlight: Float = 0.48f,
    val tintAmount: Float = 0.10f,
    val tint: Color = Color(0xFFE7F2FF),
    val depthEffect: Boolean = false,
    val chromaticAberration: Boolean = false,
    val vibrancy: Boolean = false,
    val showGlass: Boolean = true
)

private val PresetColors = listOf(
    Color(0xFFE7F2FF), Color(0xFFFFE7D6), Color(0xFFDFF6EA), Color(0xFFE8E0FF)
)

@Composable
private fun ReferenceLab() {
    var settings by remember { mutableStateOf(LabSettings()) }
    val backdrop = rememberLayerBackdrop()
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF11151A)).verticalScroll(rememberScrollState())
    ) {
        Header()
        Box(
            modifier = Modifier.fillMaxWidth().height(590.dp).padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            DocumentBackdrop(backdrop)
            if (settings.showGlass) {
                GlassScene(backdrop, settings)
            }
            Text(
                text = if (settings.showGlass) "GLASS ON · scroll the page behind fixed surfaces" else "REFERENCE A · unfiltered backdrop",
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    .background(Color(0xCC11151A), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp),
                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
            )
        }
        Comparison(backdrop, settings)
        MaterialLab(settings = settings, onChange = { settings = it })
        ReferenceInfo(settings)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Header() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("REFERENCE STUDY 01", color = Color(0xFF9BC8FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Backdrop / Kyant0", color = Color(0xFFF5F7FA), fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("A controlled document scene for inspecting the library’s own blur, lens, and backdrop pipeline.", color = Color(0xFFADB6C0), fontSize = 13.sp)
    }
}

@Composable
private fun DocumentBackdrop(backdrop: LayerBackdrop) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7F5EF)).verticalScroll(rememberScrollState())
            .layerBackdrop(backdrop).padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("FIELD NOTES", fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = Color(0xFF56616A))
            Text("VOL. 08  /  2026", fontSize = 9.sp, color = Color(0xFF747C82))
        }
        Spacer(Modifier.height(12.dp))
        Text("The shape of\nsmall things", fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D282E))
        Text("OBSERVATIONS ON LIGHT, PAPER & EVERYDAY MATERIALS", modifier = Modifier.padding(top = 8.dp), fontSize = 8.sp, letterSpacing = 1.sp, color = Color(0xFF67747A))
        Rule(Modifier.padding(vertical = 16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEDB24F)), contentAlignment = Alignment.Center) {
                Text("08", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF352A18))
            }
            Column(Modifier.padding(start = 10.dp)) {
                Text("LIGHT STUDY  /  NORTH WINDOW", fontSize = 8.sp, letterSpacing = 1.sp, color = Color(0xFF6F797E))
                Text("A clear morning, recorded slowly", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF243038))
            }
        }
        Paragraph("Morning light moves across the worktable in a narrow band. It finds the glass first, then the grain beneath it. A fine line appears where the surface bends, though the room itself has not changed.")
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A6870)))
            Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE4B25E)))
            Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFBD6F59)))
            Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF82949A)))
        }
        Text("FIG. 08   /   FOUR PIGMENTS ON UNCOATED STOCK", fontSize = 8.sp, letterSpacing = 0.8.sp, color = Color(0xFF657178))
        Rule(Modifier.padding(vertical = 14.dp))
        Text("01   Transmission", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26333A))
        Paragraph("Place a transparent pane above printed matter and the letters remain legible. Increase the curvature and their edges shift. The effect is easiest to see where a rule crosses the boundary at a shallow angle.")
        Hairlines()
        Text("02   Edges and scale", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26333A))
        Paragraph("A broad shape gives the eye room to compare its center with its perimeter. A small round sample makes the corner treatment easier to isolate. Neither object changes the source page; both read from the same recorded layer.")
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(44.dp).background(Color(0xFFBA6550)))
            Text("A high contrast stroke reveals displacement more clearly than a soft photographic background.", fontSize = 10.sp, lineHeight = 14.sp, color = Color(0xFF49565D))
        }
        Rule(Modifier.padding(vertical = 14.dp))
        Text("03   Fine print", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26333A))
        repeat(5) { index ->
            Text(
                "${index + 1}. Optical behavior is read from the relation between a surface and the content behind it. The letterforms here provide repeated edges, spacing, and contrast for comparison.",
                modifier = Modifier.padding(top = 7.dp), fontSize = 9.sp, lineHeight = 13.sp, color = Color(0xFF445159)
            )
        }
        Spacer(Modifier.height(16.dp))
        Rule()
        Text("FIELD LOG  /  END OF EXTRACT", modifier = Modifier.padding(top = 12.dp, bottom = 35.dp), fontSize = 8.sp, letterSpacing = 1.sp, color = Color(0xFF6F797E))
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(text, modifier = Modifier.padding(top = 10.dp), fontSize = 10.sp, lineHeight = 15.sp, color = Color(0xFF3D4B52))
}

@Composable
private fun Rule(modifier: Modifier = Modifier) {
    Spacer(modifier.fillMaxWidth().height(1.dp).background(Color(0xFFBFC3C0)))
}

@Composable
private fun Hairlines() {
    Column(Modifier.padding(top = 9.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF737F83))) }
    }
}

@Composable
private fun GlassScene(backdrop: LayerBackdrop, settings: LabSettings) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        GlassSurface(backdrop, settings, RoundedCornerShape(24.dp), Modifier.offset(x = 18.dp, y = 145.dp).size(width = w * 0.61f, height = 126.dp), "ROUNDED RECT")
        GlassSurface(backdrop, settings, RoundedCornerShape(16.dp), Modifier.offset(x = w - 112.dp, y = 105.dp).size(width = 94.dp, height = 72.dp), "SMALL")
        GlassSurface(backdrop, settings, RoundedCornerShape(50), Modifier.offset(x = 23.dp, y = 322.dp).size(width = 154.dp, height = 56.dp), "CAPSULE")
        GlassSurface(backdrop, settings, CircleShape, Modifier.offset(x = w - 119.dp, y = 315.dp).size(92.dp), "CIRCLE")
        GlassSurface(backdrop, settings, RoundedCornerShape(28.dp), Modifier.offset(x = w - 110.dp, y = 415.dp).size(width = 90.dp, height = 154.dp), "TALL PANEL")
    }
}

@Composable
private fun GlassSurface(backdrop: com.kyant.backdrop.Backdrop, settings: LabSettings, shape: Shape, modifier: Modifier, label: String) {
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (settings.opacity < 1f) opacity(settings.opacity)
                if (settings.vibrancy) vibrancy()
                blur(settings.blur)
                lens(
                    refractionHeight = settings.refractionHeight,
                    refractionAmount = settings.refractionAmount,
                    depthEffect = settings.depthEffect,
                    chromaticAberration = settings.chromaticAberration
                )
            },
            highlight = { Highlight(width = 7.dp, blurRadius = 4.dp, alpha = settings.highlight) },
            onDrawSurface = {
                if (settings.tintAmount > 0f) drawRect(settings.tint.copy(alpha = settings.tintAmount))
            }
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, modifier = Modifier.padding(5.dp), color = Color(0xDD17232A), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun Comparison(backdrop: LayerBackdrop, settings: LabSettings) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text("A / B COMPARISON", color = Color(0xFFEAF0F5), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text("Both samples draw from the same document backdrop.", color = Color(0xFF9DA9B3), fontSize = 11.sp)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text("A · SOURCE", color = Color(0xFFAEB9C4), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                ComparisonPaper()
            }
            Column(Modifier.weight(1f)) {
                Text("B · EFFECT", color = Color(0xFFAEB9C4), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.fillMaxWidth().height(92.dp)) {
                    ComparisonPaper()
                    GlassSurface(backdrop, settings, RoundedCornerShape(16.dp), Modifier.fillMaxSize(), "BACKDROP + EFFECTS")
                }
            }
        }
    }
}

@Composable
private fun ComparisonPaper() {
    Column(Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF7F5EF)).padding(10.dp)) {
        Text("Straight lines & fine print", color = Color(0xFF26333A), fontWeight = FontWeight.Bold, fontSize = 10.sp)
        Spacer(Modifier.height(5.dp))
        repeat(3) { index ->
            Text("${index + 1}  The quick brown fox studies the light through glass.", color = Color(0xFF445159), fontSize = 7.sp, lineHeight = 10.sp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(4) { i -> Box(Modifier.weight(1f).height(4.dp).background(PresetColors[i])) }
        }
    }
}

@Composable
private fun MaterialLab(settings: LabSettings, onChange: (LabSettings) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1C232A))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("MATERIAL / OPTICS LAB", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Text("Live parameters from Backdrop 2.0.1", color = Color(0xFF9DAAB4), fontSize = 10.sp)
                }
                Button(onClick = { onChange(LabSettings()) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334454))) { Text("RESET", fontSize = 10.sp) }
            }
            LabSlider("Backdrop blur", settings.blur, 0f..24f, "%.1f px".format(settings.blur)) { onChange(settings.copy(blur = it)) }
            LabSlider("Lens height", settings.refractionHeight, 0f..50f, "%.1f px".format(settings.refractionHeight)) { onChange(settings.copy(refractionHeight = it)) }
            LabSlider("Refraction amount", settings.refractionAmount, 0f..80f, "%.1f px".format(settings.refractionAmount)) { onChange(settings.copy(refractionAmount = it)) }
            LabSlider("Backdrop opacity", settings.opacity, 0f..1f, "%.2f".format(settings.opacity)) { onChange(settings.copy(opacity = it)) }
            LabSlider("Highlight alpha", settings.highlight, 0f..1f, "%.2f".format(settings.highlight)) { onChange(settings.copy(highlight = it)) }
            LabSlider("Tint strength", settings.tintAmount, 0f..0.45f, "%.2f".format(settings.tintAmount)) { onChange(settings.copy(tintAmount = it)) }
            Text("TINT COLOR", color = Color(0xFFAEB9C4), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetColors.forEach { color ->
                    Surface(
                        onClick = { onChange(settings.copy(tint = color)) },
                        modifier = Modifier.size(30.dp), shape = CircleShape, color = color,
                        border = if (settings.tint == color) BorderStroke(2.dp, Color.White) else null
                    ) { }
                }
            }
            ToggleRow("Depth bias (lens gradient)", settings.depthEffect) { onChange(settings.copy(depthEffect = it)) }
            ToggleRow("Chromatic aberration", settings.chromaticAberration) { onChange(settings.copy(chromaticAberration = it)) }
            ToggleRow("Vibrancy", settings.vibrancy) { onChange(settings.copy(vibrancy = it)) }
            ToggleRow("Show glass surfaces", settings.showGlass) { onChange(settings.copy(showGlass = it)) }
            Text("No IOR control exists in this reference. Lens displacement is set directly by height and amount.", color = Color(0xFF9DAAB4), fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 9.dp))
        }
    }
}

@Composable
private fun LabSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, valueLabel: String, onValue: (Float) -> Unit) {
    Column(Modifier.padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFFE3E9EE), fontSize = 11.sp)
            Text(valueLabel, color = Color(0xFF9BC8FF), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(value = value, onValueChange = onValue, valueRange = range)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFE3E9EE), fontSize = 11.sp)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ReferenceInfo(settings: LabSettings) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("REFERENCE", color = Color(0xFF9BC8FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text("Kyant0 / AndroidLiquidGlass · kmp · Backdrop 2.0.1", color = Color(0xFFE4EAF0), fontSize = 12.sp)
        Text("Rendering: retained Compose GraphicsLayer + RenderEffect chain + AGSL RuntimeShader (API 33+ for lens).", color = Color(0xFF9DA9B3), fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp))
        Text("IOR: not exposed   ·   Blur: ${settings.blur.toInt()} px   ·   Refraction: ${settings.refractionAmount.toInt()} px   ·   Opacity: ${"%.2f".format(settings.opacity)}", color = Color(0xFF9DA9B3), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
