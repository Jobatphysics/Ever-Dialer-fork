package dev.libreglass.showcase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.libreglass.LibreGlassLiveScene
import dev.libreglass.LibreGlassOptics
import dev.libreglass.LibreGlassSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        setContent { MaterialTheme(colorScheme = lightColorScheme()) { LibreGlassVisualShowcase() } }
    }
}

@Composable
private fun LibreGlassVisualShowcase() {
    var optics by remember { mutableStateOf(LibreGlassOptics()) }
    LibreGlassLiveScene(
        modifier = Modifier.fillMaxSize(),
        backdrop = { DocumentBackdrop() },
        overlay = {
            Box(Modifier.fillMaxSize()) {
                TopCapsule(optics)
                FloatingTools(optics)
                SearchAndToolbar(optics)
                ComparisonStrip(optics)
                ControlPanel(optics, onChange = { optics = it }, onReset = { optics = LibreGlassOptics() })
                BottomNavigation(optics)
            }
        },
    )
}

@Composable private fun DocumentBackdrop() {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().background(Color(0xfff5f1e8)).verticalScroll(scroll)
            .padding(horizontal = 28.dp, vertical = 126.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Field Notes", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color(0xff20263d))
        Text("A study of transparent interfaces · 23 September 2026", style = MaterialTheme.typography.labelLarge, color = Color(0xff5e6580))
        Divider(color = Color(0xffccced9))
        DocumentHeading("Material, light, and attention")
        Paragraph("A useful interface material does not merely sit on top of information. It frames nearby content, lets context remain visible, and gives a person a quiet sense of place while moving through a document.")
        Highlight("Working note", "The curved boundary is intentionally concentrated at the perimeter; the center should remain calm enough to read through.")
        Paragraph("This page contains dense type, high-contrast separators, colored annotations, and changing rhythm so that every floating layer has a real visual environment to respond to.")
        DocumentHeading("Observations")
        repeat(3) { index ->
            Paragraph("${index + 1}. When a surface moves over a heading, a colored note, or a rule, the glass should retain their relationship. Blur may soften detail, but refraction belongs near the edge rather than across all of the material.")
        }
        Highlight("Reference 04", "A material with full transparency can still read as glass if it bends the boundary light, carries a rim, and responds subtly to touch.")
        DocumentHeading("Editorial marks")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { i -> Box(Modifier.weight(1f).height(78.dp).background(listOf(Color(0xffd7e7ff), Color(0xffffd7dd), Color(0xffd7f3df))[i], RoundedCornerShape(16.dp))) }
        }
        repeat(8) { Paragraph("The document continues beneath the floating controls. Scroll this content to evaluate whether the sampled type, highlight colors, and visual edges follow the live backdrop without stale frames.") }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable private fun DocumentHeading(value: String) = Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = Color(0xff29375b))
@Composable private fun Paragraph(value: String) = Text(value, style = MaterialTheme.typography.bodyLarge, color = Color(0xff3d4254), lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25f)
@Composable private fun Highlight(label: String, value: String) = Column(Modifier.fillMaxWidth().background(Color(0xffffe6a6), RoundedCornerShape(14.dp)).padding(16.dp)) { Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Text(value, style = MaterialTheme.typography.bodyLarge) }

@Composable private fun BoxScope.TopCapsule(o: LibreGlassOptics) = LibreGlassSurface(Modifier.align(Alignment.TopCenter).padding(top = 34.dp).height(54.dp).fillMaxWidth(0.88f), 28.dp, o) {
    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.width(16.dp)); Text("›", style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.width(16.dp)); Text("Field Notes", Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text("•••")
    }
}

@Composable private fun BoxScope.FloatingTools(o: LibreGlassOptics) = Column(Modifier.align(Alignment.CenterEnd).padding(end = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { listOf("⌕", "↗", "✎", "+", "⚙").forEach { symbol -> LibreGlassSurface(Modifier.size(48.dp), 24.dp, o, onClick = {}) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(symbol, style = MaterialTheme.typography.titleLarge) } } } }

@Composable private fun BoxScope.SearchAndToolbar(o: LibreGlassOptics) = Column(Modifier.align(Alignment.TopCenter).padding(top = 104.dp).fillMaxWidth(0.78f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    LibreGlassSurface(Modifier.height(46.dp).fillMaxWidth(), 23.dp, o) { Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) { Text("⌕   Search this document", color = Color(0xff31384c)) } }
    LibreGlassSurface(Modifier.height(42.dp).fillMaxWidth(), 20.dp, o.copy(transparency = (o.transparency + .1f).coerceAtMost(1f))) { Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { listOf("B", "I", "U", "☰", "●", "•••").forEach { Text(it, fontWeight = FontWeight.Bold) } } }
}

@Composable private fun BoxScope.ComparisonStrip(o: LibreGlassOptics) = Row(Modifier.align(Alignment.CenterStart).padding(start = 18.dp).fillMaxWidth(.72f), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Clear" to o.copy(transparency=.95f, refraction=.20f), "Soft" to o.copy(blurRadius=22f, refraction=.35f), "Lens" to o.copy(refraction=.9f, horizontalCurvature=.8f, verticalCurvature=.8f)).forEach { (name, style) -> LibreGlassSurface(Modifier.weight(1f).height(62.dp), 18.dp, style) { Column(Modifier.padding(9.dp)) { Text(name, fontWeight=FontWeight.Bold); Text("R ${(style.refraction*100).toInt()}%", style=MaterialTheme.typography.labelSmall) } } } }

@Composable private fun BoxScope.ControlPanel(o: LibreGlassOptics, onChange: (LibreGlassOptics) -> Unit, onReset: () -> Unit) = LibreGlassSurface(Modifier.align(Alignment.BottomStart).padding(18.dp).width(300.dp), 24.dp, o.copy(transparency=.32f)) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Material lab", fontWeight=FontWeight.Bold)
        SliderRow("Transparency", o.transparency) { onChange(o.copy(transparency=it)) }
        SliderRow("Refraction", o.refraction) { onChange(o.copy(refraction=it)) }
        SliderRow("Horizontal edge", o.horizontalEdgeWidth) { onChange(o.copy(horizontalEdgeWidth=it)) }
        SliderRow("Vertical edge", o.verticalEdgeWidth) { onChange(o.copy(verticalEdgeWidth=it)) }
        SliderRow("Blur", o.blurRadius / 40f) { onChange(o.copy(blurRadius=it*40f)) }
        SliderRow("Horizontal curve", o.horizontalCurvature) { onChange(o.copy(horizontalCurvature=it)) }
        SliderRow("Vertical curve", o.verticalCurvature) { onChange(o.copy(verticalCurvature=it)) }
        SliderRow("Dispersion", o.chromaticDispersion / 4f) { onChange(o.copy(chromaticDispersion=it*4f)) }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { TextButton(onClick=onReset) { Text("Reset") }; TextButton(onClick={ onChange(o.copy(transparency=.92f, refraction=.15f, blurRadius=4f)) }) { Text("Clear") }; TextButton(onClick={ onChange(o.copy(blurRadius=22f, refraction=.35f)) }) { Text("Soft") }; TextButton(onClick={ onChange(o.copy(refraction=.9f, horizontalCurvature=.9f, verticalCurvature=.9f)) }) { Text("Lens") }; TextButton(onClick={ onChange(o.copy(transparency=.30f, blurRadius=32f, refraction=.12f)) }) { Text("Frosted") } }
    }
}

@Composable private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) { Column { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text(label, style=MaterialTheme.typography.labelSmall); Text("${(value*100).toInt()}%", style=MaterialTheme.typography.labelSmall) }; Slider(value=value, onValueChange=onChange, modifier=Modifier.height(22.dp)) } }

@Composable private fun BoxScope.BottomNavigation(o: LibreGlassOptics) = LibreGlassSurface(Modifier.align(Alignment.BottomCenter).padding(bottom=20.dp).height(54.dp).fillMaxWidth(.62f), 27.dp, o) { Row(Modifier.fillMaxSize(), horizontalArrangement=Arrangement.SpaceEvenly, verticalAlignment=Alignment.CenterVertically) { listOf("⌂", "▤", "★", "◉").forEach { Text(it, style=MaterialTheme.typography.titleLarge) } } }
