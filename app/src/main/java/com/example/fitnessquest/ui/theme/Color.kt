package com.example.fitnessquest.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
val NeonBlue = Color(0xFF00CFFF)
val NeonGreen = Color(0xFF44FF88)
val GymRed = Color(0xFFFF3C3C)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1C1C1E)
val SurfaceVariantDark = Color(0xFF2A2A2D)


val BroDarkColors = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color.Black,
    secondary = NeonBlue,
    onSecondary = Color.Black,
    background = BackgroundDark,
    onBackground = Color(0xFFECECEC),
    surface = SurfaceDark,
    onSurface = Color(0xFFECECEC),
    surfaceVariant = SurfaceVariantDark,
)