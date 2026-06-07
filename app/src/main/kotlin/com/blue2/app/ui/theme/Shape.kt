package com.blue2.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 Expressive shapes — generous rounding
val Blue2Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Custom shapes for specific UI elements
val CarCardShape = RoundedCornerShape(28.dp)
val ControlButtonShape = RoundedCornerShape(20.dp)
val StatusChipShape = RoundedCornerShape(50.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
val SquircleShape = RoundedCornerShape(percent = 35)
