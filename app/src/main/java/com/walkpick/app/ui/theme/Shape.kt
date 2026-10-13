package com.walkpick.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Corner radii the design uses: 12 small tiles, 16 fields and chips, 20 inner cards, 24 cards.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

val CardShape = AppShapes.extraLarge

val PillShape = RoundedCornerShape(percent = 50)

val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
