package com.android.libredialer.view.newui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.android.libredialer.view.theme.MyFontFamily
import com.android.libredialer.view.theme.buildTypography

private val NewUiLightColors = lightColorScheme(
    primary = Color(0xFF415F91),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF284777),
    secondary = Color(0xFF565F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E3F8),
    onSecondaryContainer = Color(0xFF3E4759),
    tertiary = Color(0xFF705575),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBD7FA),
    onTertiaryContainer = Color(0xFF573D5C)
)

private val NewUiDarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF0B305F),
    primaryContainer = Color(0xFF284777),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273141),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFD9E3F8),
    tertiary = Color(0xFFDEBBDD),
    onTertiary = Color(0xFF402843),
    tertiaryContainer = Color(0xFF573D5C),
    onTertiaryContainer = Color(0xFFFBD7FA)
)

private val NewUiShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

object NewUiDimensions {
    val PagePadding = 20.dp
    val SectionSpacing = 24.dp
    val ItemSpacing = 12.dp
    val CompactSpacing = 8.dp
    val ControlHeight = 56.dp
    val CardPadding = 16.dp
}

@Composable
fun NewUiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontFamily: FontFamily = MyFontFamily,
    content: @Composable () -> Unit
) {
    val colors: ColorScheme = if (darkTheme) NewUiDarkColors else NewUiLightColors
    val typography: Typography = buildTypography(fontFamily = fontFamily)

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = NewUiShapes,
        content = content
    )
}
