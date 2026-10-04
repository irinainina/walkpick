package com.walkspot.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

private fun shadow(y: Dp, blur: Dp, alpha: Float, color: Color = SteelBlue) =
    Shadow(radius = blur, color = color.copy(alpha = alpha), offset = DpOffset(0.dp, y))

/** The drop shadows of the design, for Modifier.dropShadow. */
object AppShadows {
    val card = shadow(y = 8.dp, blur = 24.dp, alpha = 0.12f)
    val accentButton = shadow(y = 8.dp, blur = 20.dp, alpha = 0.3f, color = Blue)
    val circleButton = shadow(y = 4.dp, blur = 12.dp, alpha = 0.12f)
    val segment = shadow(y = 2.dp, blur = 8.dp, alpha = 0.16f)
    val chip = shadow(y = 2.dp, blur = 8.dp, alpha = 0.12f)
    val knob = shadow(y = 1.dp, blur = 4.dp, alpha = 0.2f)
    val navBar = shadow(y = (-4).dp, blur = 16.dp, alpha = 0.08f)
}
