package com.aiphoto.manager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Small components: Chips, small buttons, icons
    small = RoundedCornerShape(8.dp),

    // Medium components: Cards, text fields, search bars
    medium = RoundedCornerShape(16.dp),

    // Large components: Dialogs, large cards, bottom sheets
    large = RoundedCornerShape(24.dp),

    // Extra large: Hero sections, full-width cards
    extraLarge = RoundedCornerShape(28.dp)
)

// Special anime-themed shapes
val PillShape = RoundedCornerShape(50)
val CardTopRounded = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
val CardBottomRounded = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
