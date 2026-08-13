package com.ridgeline.android.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ridgeline.ui.theme.TextStyleSpec
import com.ridgeline.ui.theme.TextWeight

/**
 * :ui carries colors as ARGB Long and text styles as [TextStyleSpec] rather
 * than Compose types, since it has no Compose dependency -- see
 * ui/theme/Colors.kt. These convert at the call site, in the one module
 * (:android) where a Compose type is available.
 */
fun Long.toComposeColor(): Color = Color(this)

private fun TextWeight.toFontWeight(): FontWeight = when (this) {
    TextWeight.MEDIUM -> FontWeight.Medium
    TextWeight.SEMIBOLD -> FontWeight.SemiBold
    TextWeight.BOLD -> FontWeight.Bold
    TextWeight.EXTRABOLD -> FontWeight.ExtraBold
}

fun TextStyleSpec.toTextStyle(color: Color): TextStyle = TextStyle(
    color = color,
    fontSize = sizeSp.sp,
    fontWeight = weight.toFontWeight(),
    letterSpacing = letterSpacingEm.em,
)
