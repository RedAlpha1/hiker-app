package com.ridgeline.android

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ridgeline.ui.theme.TextStyleSpec

/**
 * `:ui`'s `Colors`/`Typography` are plain ARGB `Long`/spec data classes with
 * no Compose dependency (`:ui` is deliberately Compose-free -- see
 * DECISIONS.md, "Camera preview and map view actuals"). This is the call
 * site their own doc comments point to for the actual conversion.
 */
fun Long.toComposeColor(): Color = Color(this.toInt())

/**
 * Note: [TextStyleSpec.uppercase] isn't a `TextStyle` property -- apply
 * `.uppercase()` to the string itself at the call site where that flag is set.
 * No bundled "Inter" font yet, so this falls back to the platform default.
 */
fun TextStyleSpec.toTextStyle(color: Color): TextStyle = TextStyle(
    color = color,
    fontSize = sizeSp.sp,
    fontWeight = FontWeight(weight.cssValue),
    letterSpacing = letterSpacingEm.em,
)
