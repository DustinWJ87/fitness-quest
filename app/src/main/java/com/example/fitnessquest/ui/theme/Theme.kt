package com.example.fitnessquest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable


@Composable
fun FQTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BroDarkColors,
        typography = Typography(),
        content = content
    )
}