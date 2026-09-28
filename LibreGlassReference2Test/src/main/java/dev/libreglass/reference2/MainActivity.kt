package dev.libreglass.reference2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.liquidglass.compose.GlassHighlight
import dev.liquidglass.compose.GlassRefraction
import dev.liquidglass.compose.GlassShape
import dev.liquidglass.compose.GlassStyle
import dev.liquidglass.compose.LiquidGlassProviderState
import dev.liquidglass.compose.LocalLiquidGlassTier
import dev.liquidglass.compose.container.LiquidGlassContainer
import dev.liquidglass.compose.container.LiquidGlassContainerState
import dev.liquidglass.compose.container.glassEffect
import dev.liquidglass.compose.container.rememberLiquidGlassContainerState
import dev.liquidglass.compose.liquidGlass
import dev.liquidglass.compose.liquidGlassProvider
import dev.liquidglass.compose.rememberLiquidGlassProviderState
import dev.liquidglass.compose.components.GlassBottomBar
import dev.liquidglass.compose.components.GlassButton
import dev.liquidglass.compose.components.GlassCard
import dev.liquidglass.compose.components.GlassIconButton
import dev.liquidglass.compose.components.GlassSurface
import dev.liquidglass.core.GlassRenderTier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Reference2Lab() } }
    }
}

private enum class ProbeShape { RoundedRectangle, Capsule, Circle }
private enum class ClusterPhase(val label: String, val gapDp: Float) {
    Separate("SEPARATE", 48f), Approach("APPROACH", 24f), Merge("MERGE", 2f), Apart("MOVE APART", 68f)
}
private enum class ClusterLayout(val label: String) {
    Two("TWO BUTTONS"), Three("THREE BUTTONS"), FabActions("FAB + 2 ACTIONS")
}
private enum class MaterialPreset(val label: String) { Regular("REGULAR"), Clear("CLEAR"), Prominent("PROMINENT") }
private enum class TierScope(val label: String) { Local("LOCAL"), Provider("PROVIDER") }

private data class LabSettings(
    val blurDp: Float = 20f,
    val refractionHeightDp: Float = 12f,
    val refractionAmountDp: Float = 16f,
    val saturation: Float = 1.5f,
    val chromaticAberration: Float = 0f,
    val highlightWidthDp: Float = 2.5f,
    val highlightAlpha: Float = 0.55f,
    val lightAngleDegrees: Float = 245f,
    val tint: Color = Color.Unspecified,
    val tintAlpha: Float = 0f,
    val fallbackScrim: Color = Color.Unspecified,
    val noiseAlpha: Float = 0.015f,
    val interactive: Boolean = GlassStyle.Regular.isInteractive,
    val probeShape: ProbeShape = ProbeShape.RoundedRectangle,
    val cornerRadiusDp: Float = 24f,
    val mergeSpacingDp: Float = 16f,
    val clusterGapDp: Float = ClusterPhase.Separate.gapDp,
    val clusterLayout: ClusterLayout = ClusterLayout.FabActions,
    val materialPreset: MaterialPreset = MaterialPreset.Regular,
    val tierScope: TierScope = TierScope.Local,
    val animateBackdrop: Boolean = true,
    val surfaceShiftDp: Float = 0f,
    val mergeContainerEnabled: Boolean = true,
    val tier: GlassRenderTier? = null,
    val showGlass: Boolean = true,
)

private val TintChoices = listOf(
    Color.Unspecified, Color(0xFFB8DCFF), Color(0xFFFFD4BD), Color(0xFFBDE8D2), Color(0xFFD8C8FF)
)

private fun LabSettings.withPreset(preset: MaterialPreset): LabSettings {
    val accent = if (tint.isSpecified) tint.copy(alpha = 0.55f) else Color(0xFF91C8FF).copy(alpha = 0.55f)
    val style = when (preset) {
        MaterialPreset.Regular -> GlassStyle.Regular
        MaterialPreset.Clear -> GlassStyle.Clear
        MaterialPreset.Prominent -> GlassStyle.prominent(accent)
    }
    return copy(
        materialPreset = preset,
        blurDp = style.blurRadius.value,
        refractionHeightDp = style.refraction.height.value,
        refractionAmountDp = style.refraction.amount.value,
        saturation = style.saturation,
        chromaticAberration = style.chromaticAberration,
        highlightWidthDp = style.highlight.width.value,
        highlightAlpha = style.highlight.alpha,
        lightAngleDegrees = style.highlight.lightAngleDegrees,
        tint = if (style.tint.isSpecified) style.tint.copy(alpha = 1f) else Color.Unspecified,
        tintAlpha = if (style.tint.isSpecified) style.tint.alpha else 0f,
        noiseAlpha = style.noiseAlpha,
        fallbackScrim = style.fallbackScrim,
        interactive = style.isInteractive,
    )
}

@Composable
private fun Reference2Lab() {
    var settings by remember { mutableStateOf(LabSettings()) }
    val provider = rememberLiquidGlassProviderState()
    val mergeState = rememberLiquidGlassContainerState(provider)
    SideEffect { provider.requestedTier = settings.tier.takeIf { settings.tierScope == TierScope.Provider } }
    val detectedTier = GlassRenderTier.select(android.os.Build.VERSION.SDK_INT, settings.tier)

    Column(Modifier.fillMaxSize().background(Color(0xFF11151A)).verticalScroll(rememberScrollState())) {
        Header(detectedTier)
        Box(
            Modifier.fillMaxWidth().height(790.dp).padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            DocumentBackdrop(provider, settings.animateBackdrop)
            if (settings.showGlass) CompositionLocalProvider(
                LocalLiquidGlassTier provides settings.tier.takeIf { settings.tierScope == TierScope.Local },
            ) { GlassScene(provider, mergeState, settings) }
            Text(
                "FIELD NOTES · scroll the document behind fixed glass",
                Modifier.align(Alignment.BottomStart).padding(12.dp)
                    .background(Color(0xCC11151A), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp),
                color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
            )
        }
        MaterialLab(settings) { settings = it }
        ReferenceInfo(settings, detectedTier)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Header(tier: GlassRenderTier) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("REFERENCE STUDY 02", color = Color(0xFF9BC8FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("LiquidGlass / Abdullajon1881", color = Color(0xFFF5F7FA), fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("AGSL edge lens · gel press · smooth-min shape merging", color = Color(0xFFADB6C0), fontSize = 12.sp)
        Text("ACTIVE TIER: $tier", color = Color(0xFFB8E2C2), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun DocumentBackdrop(provider: LiquidGlassProviderState, animateBackdrop: Boolean) {
    val transition = rememberInfiniteTransition(label = "reference2-backdrop-motion")
    val motion by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), repeatMode = RepeatMode.Reverse),
        label = "reference2-content-position",
    )
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF7F5EF)).verticalScroll(rememberScrollState())
            .liquidGlassProvider(provider).padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("FIELD NOTES", fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = Color(0xFF56616A))
            Text("VOL. 08  /  2026", fontSize = 9.sp, color = Color(0xFF747C82))
        }
        Spacer(Modifier.height(12.dp))
        Text("The shape of\nsmall things", fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D282E))
        Text("OBSERVATIONS ON LIGHT, PAPER & EVERYDAY MATERIALS", Modifier.padding(top = 8.dp), fontSize = 8.sp, letterSpacing = 1.sp, color = Color(0xFF67747A))
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
            listOf(Color(0xFF2A6870), Color(0xFFE4B25E), Color(0xFFBD6F59), Color(0xFF82949A)).forEach { color ->
                Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(4.dp)).background(color))
            }
        }
        Box(Modifier.fillMaxWidth().height(18.dp).padding(vertical = 5.dp).background(Color(0xFFD9E0DE), RoundedCornerShape(4.dp))) {
            Box(
                Modifier.offset(x = if (animateBackdrop) (motion * 190f).dp else 0.dp)
                    .width(32.dp).height(8.dp).align(Alignment.CenterStart)
                    .background(Color(0xFFBD6F59), RoundedCornerShape(4.dp))
            )
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
                Modifier.padding(top = 7.dp), fontSize = 9.sp, lineHeight = 13.sp, color = Color(0xFF445159)
            )
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(10) { index -> Box(Modifier.weight(1f).height(if (index % 2 == 0) 18.dp else 9.dp).background(if (index % 2 == 0) Color(0xFF283A43) else Color(0xFFE0A857))) }
        }
        Rule()
        Text("FIELD LOG  /  END OF EXTRACT", Modifier.padding(top = 12.dp, bottom = 35.dp), fontSize = 8.sp, letterSpacing = 1.sp, color = Color(0xFF6F797E))
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(text, Modifier.padding(top = 10.dp), fontSize = 10.sp, lineHeight = 15.sp, color = Color(0xFF3D4B52))
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
private fun GlassScene(
    provider: LiquidGlassProviderState,
    mergeState: LiquidGlassContainerState,
    settings: LabSettings,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        GlassCard(
            state = provider,
            modifier = Modifier.offset(x = 17.dp, y = 145.dp).size(width = width * 0.60f, height = 118.dp),
            style = settings.toStyle(settings.toShape()),
            contentPadding = PaddingValues(4.dp),
        ) { Text("ROUNDED RECT / PROBE", color = Color(0xE517232A), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.35.sp) }
        GlassPane(provider, settings, GlassShape.RoundedRectangle(16.dp), Modifier.offset(x = width - 111.dp + settings.surfaceShiftDp.dp, y = 102.dp).size(width = 94.dp, height = 74.dp), "SMALL RECT")
        GlassButton(
            onClick = {}, state = provider,
            modifier = Modifier.offset(x = 21.dp, y = 308.dp).size(width = 154.dp, height = 56.dp),
            style = settings.toStyle(GlassShape.Capsule), contentPadding = PaddingValues(4.dp),
        ) { Text("CAPSULE / GlassButton", color = Color(0xE517232A), fontSize = 7.sp, fontWeight = FontWeight.Bold) }
        GlassPane(provider, settings, GlassShape.Circle, Modifier.offset(x = width - 116.dp, y = 300.dp).size(92.dp), "CIRCLE")
        GlassSurface(
            state = provider,
            modifier = Modifier.offset(x = width - 108.dp, y = 405.dp).size(width = 90.dp, height = 152.dp),
            style = settings.toStyle(GlassShape.RoundedRectangle(28.dp)),
        ) { Text("TALL PANEL", color = Color(0xE517232A), fontSize = 7.sp, fontWeight = FontWeight.Bold) }
        GlassIconButton(
            onClick = {}, state = provider,
            modifier = Modifier.offset(x = width - 72.dp, y = 204.dp),
            style = settings.toStyle(GlassShape.Circle),
        ) { Text("⌕", color = Color(0xE517232A), fontSize = 18.sp) }
        MergeDemo(provider, mergeState, settings, Modifier.offset(y = 575.dp).fillMaxWidth().height(148.dp).padding(horizontal = 8.dp))
        GlassBottomBar(
            state = provider,
            modifier = Modifier.offset(y = 726.dp).fillMaxWidth().height(54.dp).padding(horizontal = 12.dp),
            style = settings.toStyle(GlassShape.Capsule),
        ) {
            Text("NOTES", Modifier.weight(1f), color = Color(0xE517232A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("SEARCH", Modifier.weight(1f), color = Color(0xE517232A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("TOOLS", Modifier.weight(1f), color = Color(0xE517232A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GlassPane(
    provider: LiquidGlassProviderState,
    settings: LabSettings,
    shape: GlassShape,
    modifier: Modifier,
    label: String,
) {
    val actualShape = if (label.endsWith("PROBE")) settings.toShape() else shape
    Box(
        modifier.liquidGlass(provider, settings.toStyle(actualShape)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, Modifier.padding(4.dp), color = Color(0xE517232A), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.35.sp)
    }
}

@Composable
private fun MergeDemo(provider: LiquidGlassProviderState, state: LiquidGlassContainerState, settings: LabSettings, modifier: Modifier) {
    val gap by animateDpAsState(
        targetValue = settings.clusterGapDp.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "reference2-cluster-gap",
    )
    val members: @Composable RowScope.() -> Unit = {
        when (settings.clusterLayout) {
            ClusterLayout.Two -> {
                MergeMember(provider, state, "EDIT", 54.dp, "edit", settings, settings.mergeContainerEnabled)
                MergeMember(provider, state, "SHARE", 54.dp, "share", settings, settings.mergeContainerEnabled)
            }
            ClusterLayout.Three -> {
                MergeMember(provider, state, "EDIT", 54.dp, "edit", settings, settings.mergeContainerEnabled)
                MergeMember(provider, state, "SHARE", 54.dp, "share", settings, settings.mergeContainerEnabled)
                MergeMember(provider, state, "SAVE", 54.dp, "save", settings, settings.mergeContainerEnabled)
            }
            ClusterLayout.FabActions -> {
                MergeMember(provider, state, "EDIT", 48.dp, "edit", settings, settings.mergeContainerEnabled)
                MergeMember(provider, state, "SHARE", 48.dp, "share", settings, settings.mergeContainerEnabled)
                MergeMember(provider, state, "FAB", 64.dp, "fab", settings, settings.mergeContainerEnabled)
            }
        }
    }
    val content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {
        Column(Modifier.fillMaxSize().padding(top = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (settings.mergeContainerEnabled) "LIQUID MERGE · upstream container" else "SEPARATE SURFACES · container disabled", color = Color(0xFF15242A), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) { members() }
        }
    }
    if (settings.mergeContainerEnabled) LiquidGlassContainer(
        state = state,
        modifier = modifier,
        style = settings.toStyle(GlassShape.Capsule),
        spacing = settings.mergeSpacingDp.dp,
        content = content,
    ) else Box(modifier, content = content)
}

@Composable
private fun MergeMember(
    provider: LiquidGlassProviderState,
    state: LiquidGlassContainerState,
    label: String,
    diameter: androidx.compose.ui.unit.Dp,
    id: String,
    settings: LabSettings,
    merged: Boolean,
) {
    val shape = GlassShape.Circle
    val style = settings.toStyle(shape)
    Box(
        if (merged) Modifier.size(diameter).glassEffect(state, id = id, shape = shape, interactive = settings.interactive)
        else Modifier.size(diameter).liquidGlass(provider, style),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xE517232A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

private fun LabSettings.toShape(): GlassShape = when (probeShape) {
    ProbeShape.RoundedRectangle -> GlassShape.RoundedRectangle(cornerRadiusDp.dp)
    ProbeShape.Capsule -> GlassShape.Capsule
    ProbeShape.Circle -> GlassShape.Circle
}

private fun LabSettings.toStyle(shape: GlassShape): GlassStyle = GlassStyle(
    shape = shape,
    blurRadius = blurDp.dp,
    refraction = GlassRefraction(height = refractionHeightDp.dp, amount = refractionAmountDp.dp),
    saturation = saturation,
    tint = if (tint.isSpecified && tintAlpha > 0f) tint.copy(alpha = tintAlpha) else Color.Unspecified,
    highlight = GlassHighlight(width = highlightWidthDp.dp, alpha = highlightAlpha, lightAngleDegrees = lightAngleDegrees),
    noiseAlpha = noiseAlpha,
    chromaticAberration = chromaticAberration,
    isInteractive = interactive,
    fallbackScrim = fallbackScrim,
)

@Composable
private fun MaterialLab(settings: LabSettings, onChange: (LabSettings) -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1C232A)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("MATERIAL / OPTICS LAB", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Text("Live controls map to GlassStyle / GlassRefraction / GlassHighlight", color = Color(0xFF9DAAB4), fontSize = 9.sp)
                }
                Button(onClick = { onChange(LabSettings()) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334454))) { Text("RESET", fontSize = 10.sp) }
            }
            Text("ABDULLAJON LIQUIDGLASS REFERENCE · MATERIAL PRESET", modifier = Modifier.padding(top = 10.dp), color = Color(0xFF9BC8FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MaterialPreset.entries.forEach { preset ->
                    FilterChip(selected = settings.materialPreset == preset, onClick = { onChange(settings.withPreset(preset)) }, label = { Text(preset.label, fontSize = 8.sp) })
                }
            }
            Text("REFRACTION", modifier = Modifier.padding(top = 10.dp), color = Color(0xFF9BC8FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            LabSlider("Blur radius", settings.blurDp, 0f..40f, "%.1f dp".format(settings.blurDp)) { onChange(settings.copy(blurDp = it)) }
            LabSlider("GlassRefraction.height", settings.refractionHeightDp, 0f..40f, "%.1f dp".format(settings.refractionHeightDp)) { onChange(settings.copy(refractionHeightDp = it)) }
            LabSlider("GlassRefraction.amount", settings.refractionAmountDp, -48f..48f, "%.1f dp".format(settings.refractionAmountDp)) { onChange(settings.copy(refractionAmountDp = it)) }
            LabSlider("Saturation", settings.saturation, 0f..2.5f, "%.2f".format(settings.saturation)) { onChange(settings.copy(saturation = it)) }
            LabSlider("Chromatic aberration", settings.chromaticAberration, 0f..1f, "%.2f".format(settings.chromaticAberration)) { onChange(settings.copy(chromaticAberration = it)) }
            Text("HIGHLIGHT / RIM", modifier = Modifier.padding(top = 10.dp), color = Color(0xFF9BC8FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            LabSlider("GlassHighlight.width", settings.highlightWidthDp, 0f..12f, "%.1f dp".format(settings.highlightWidthDp)) { onChange(settings.copy(highlightWidthDp = it)) }
            LabSlider("GlassHighlight.alpha", settings.highlightAlpha, 0f..1f, "%.2f".format(settings.highlightAlpha)) { onChange(settings.copy(highlightAlpha = it)) }
            LabSlider("Light angle", settings.lightAngleDegrees, 0f..360f, "%.0f°".format(settings.lightAngleDegrees)) { onChange(settings.copy(lightAngleDegrees = it)) }
            Text("TINT / GRAIN", modifier = Modifier.padding(top = 10.dp), color = Color(0xFF9BC8FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("Tint choices · neutral / cool / warm / green / violet", color = Color(0xFFAEB9C4), fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TintChoices.forEach { color ->
                    Surface(
                        onClick = { onChange(settings.copy(tint = color, tintAlpha = if (!color.isSpecified) 0f else settings.tintAlpha.takeIf { it > 0f } ?: 0.35f)) },
                        modifier = Modifier.size(30.dp), shape = CircleShape,
                        color = if (color.isSpecified) color else Color(0xFF606A72),
                        border = androidx.compose.foundation.BorderStroke(if (settings.tint == color) 2.dp else 1.dp, Color.White.copy(alpha = 0.7f)),
                    ) { if (!color.isSpecified) Text("∅", color = Color.White, fontSize = 12.sp) }
                }
            }
            LabSlider("Tint alpha / mix", settings.tintAlpha, 0f..1f, "%.2f".format(settings.tintAlpha), enabled = settings.tint.isSpecified) { onChange(settings.copy(tintAlpha = it)) }
            LabSlider("Dither noise alpha", settings.noiseAlpha, 0f..0.05f, "%.3f".format(settings.noiseAlpha)) { onChange(settings.copy(noiseAlpha = it)) }
            Text("Fallback scrim color · used only by SCRIM tier", color = Color(0xFFAEB9C4), fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf(Color.Unspecified, Color.White, Color(0xFF263747), Color(0xFFB8DCFF))).forEach { color ->
                    Surface(
                        onClick = { onChange(settings.copy(fallbackScrim = color)) },
                        modifier = Modifier.size(30.dp), shape = CircleShape,
                        color = if (color.isSpecified) color else Color(0xFF606A72),
                        border = androidx.compose.foundation.BorderStroke(if (settings.fallbackScrim == color) 2.dp else 1.dp, Color.White.copy(alpha = 0.7f)),
                    ) { if (!color.isSpecified) Text("∅", color = Color.White, fontSize = 12.sp) }
                }
            }
            Text("SHAPE / INTERACTION / MERGING", modifier = Modifier.padding(top = 10.dp), color = Color(0xFF9BC8FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("Main probe GlassShape", color = Color(0xFFAEB9C4), fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ProbeShape.entries.forEach { shape ->
                    FilterChip(selected = settings.probeShape == shape, onClick = { onChange(settings.copy(probeShape = shape)) }, label = { Text(shape.name, fontSize = 9.sp) })
                }
            }
            LabSlider("RoundedRectangle.cornerRadius", settings.cornerRadiusDp, 0f..64f, "%.0f dp".format(settings.cornerRadiusDp)) { onChange(settings.copy(cornerRadiusDp = it)) }
            ToggleRow("Interactive gel press (GlassStyle.isInteractive)", settings.interactive) { onChange(settings.copy(interactive = it)) }
            Text("Tap any surface or merge action to see the reference’s press spring, local shader bulge, and brighter rim.", color = Color(0xFFAEB9C4), fontSize = 9.sp, lineHeight = 13.sp)
            Text("Merge geometry", color = Color(0xFFAEB9C4), fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
            ToggleRow("LiquidGlassContainer enabled", settings.mergeContainerEnabled) { onChange(settings.copy(mergeContainerEnabled = it)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ClusterLayout.entries.forEach { layout ->
                    FilterChip(selected = settings.clusterLayout == layout, onClick = { onChange(settings.copy(clusterLayout = layout)) }, label = { Text(layout.label, fontSize = 8.sp) })
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ClusterPhase.entries.forEach { phase ->
                    FilterChip(selected = settings.clusterGapDp == phase.gapDp, onClick = { onChange(settings.copy(clusterGapDp = phase.gapDp)) }, label = { Text(phase.label, fontSize = 8.sp) })
                }
            }
            LabSlider("LiquidGlassContainer.spacing (merge distance / smooth-min K)", settings.mergeSpacingDp, 0f..64f, "%.0f dp".format(settings.mergeSpacingDp), enabled = settings.mergeContainerEnabled) { onChange(settings.copy(mergeSpacingDp = it)) }
            Text("RENDER TIER", modifier = Modifier.padding(top = 8.dp), color = Color(0xFFAEB9C4), fontSize = 9.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(null, GlassRenderTier.SHADER, GlassRenderTier.BLUR, GlassRenderTier.SCRIM).forEach { tier ->
                    val label = tier?.name ?: "AUTO"
                    FilterChip(selected = settings.tier == tier, onClick = { onChange(settings.copy(tier = tier)) }, label = { Text(label, fontSize = 8.sp) })
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TierScope.entries.forEach { scope ->
                    FilterChip(selected = settings.tierScope == scope, onClick = { onChange(settings.copy(tierScope = scope)) }, label = { Text("CAP VIA ${scope.label}", fontSize = 8.sp) })
                }
            }
            val deviceTier = GlassRenderTier.select(android.os.Build.VERSION.SDK_INT)
            Text(
                "Device maximum: $deviceTier · request: ${settings.tier ?: "AUTO"} · active: ${GlassRenderTier.select(android.os.Build.VERSION.SDK_INT, settings.tier)}. Requests can only lower fidelity.",
                color = Color(0xFFAEB9C4), fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                when (GlassRenderTier.select(android.os.Build.VERSION.SDK_INT, settings.tier)) {
                    GlassRenderTier.SHADER -> "AGSL: SDF lens, merge union, dispersion, gel bulge, blur/saturation and shader rim."
                    GlassRenderTier.BLUR -> "Fallback: blurred/saturated clipped backdrop, tint wash and drawn rim; no lens or merge."
                    GlassRenderTier.SCRIM -> "Fallback: unblurred clipped backdrop with scrim and drawn rim; no lens or merge."
                },
                color = Color(0xFFAEB9C4), fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 3.dp),
            )
            ToggleRow("Show glass surfaces", settings.showGlass) { onChange(settings.copy(showGlass = it)) }
            ToggleRow("Animate a backdrop marker", settings.animateBackdrop) { onChange(settings.copy(animateBackdrop = it)) }
            LabSlider("Move small probe (coordinate test)", settings.surfaceShiftDp, 0f..80f, "%.0f dp".format(settings.surfaceShiftDp)) { onChange(settings.copy(surfaceShiftDp = it)) }
            NotPresent("Physical IOR / Snell refraction")
            NotPresent("Final glass alpha / independent transparency")
        }
    }
}

@Composable
private fun LabSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    enabled: Boolean = true,
    onValue: (Float) -> Unit,
) {
    Column(Modifier.padding(top = 7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFFE3E9EE), fontSize = 10.sp)
            Text(valueLabel, color = Color(0xFF9BC8FF), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(value = value, onValueChange = onValue, valueRange = range, steps = 20, enabled = enabled)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFE3E9EE), fontSize = 10.sp)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun NotPresent(label: String) {
    Text("NOT PRESENT IN REFERENCE: $label", modifier = Modifier.padding(top = 7.dp), color = Color(0xFFF0C7A9), fontSize = 9.sp)
}

@Composable
private fun ReferenceInfo(settings: LabSettings, tier: GlassRenderTier) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("REFERENCE", color = Color(0xFF9BC8FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text("Abdullajon1881 / LiquidGlass · main · 72ad05c", color = Color(0xFFE4EAF0), fontSize = 12.sp)
        Text("Active renderer: $tier · API 33+ shader, API 31–32 blur, API 21+ scrim.", color = Color(0xFF9DA9B3), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        Text("IOR: not present · blur ${settings.blurDp.toInt()} dp · refraction ${settings.refractionAmountDp.toInt()} dp · CA ${"%.2f".format(settings.chromaticAberration)} · merge K ${settings.mergeSpacingDp.toInt()} dp", color = Color(0xFF9DA9B3), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
