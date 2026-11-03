package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LightLeafGreen,
    onPrimary = Color(0xFF00390F),
    primaryContainer = ForestGreenPrimary,
    onPrimaryContainer = Color(0xFFA6F5A3),
    secondary = SageGreenSecondary,
    onSecondary = Color.White,
    tertiary = MintAccent,
    background = EcoBackgroundDark,
    surface = EcoSurfaceDark,
    surfaceVariant = EcoSurfaceVariantDark,
    onBackground = EcoOnBackgroundDark,
    onSurface = EcoOnSurfaceDark,
    error = PriorityCriticalRed
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6F5A3),
    onPrimaryContainer = Color(0xFF002206),
    secondary = SageGreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC7EBC4),
    onSecondaryContainer = Color(0xFF052107),
    tertiary = MintAccent,
    background = EcoBackgroundLight,
    surface = EcoSurfaceLight,
    surfaceVariant = EcoSurfaceVariantLight,
    onBackground = EcoOnBackgroundLight,
    onSurface = EcoOnSurfaceLight,
    error = PriorityCriticalRed
)

@Composable
fun FoodWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Desactivado para mantener consistencia de marca verde ecológica
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FoodWiseTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
