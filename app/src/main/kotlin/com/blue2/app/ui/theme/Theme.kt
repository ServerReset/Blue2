package com.blue2.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Theme modes
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightColorScheme = lightColorScheme(
    primary = Blue2Primary,
    onPrimary = Blue2OnPrimary,
    primaryContainer = Blue2PrimaryContainer,
    secondary = Blue2Secondary,
    secondaryContainer = Blue2SecondaryContainer,
    tertiary = Blue2Tertiary,
    tertiaryContainer = Blue2TertiaryContainer,
    error = Blue2Error,
    errorContainer = Blue2ErrorContainer,
    surface = Blue2Surface,
    surfaceVariant = Blue2SurfaceVariant,
    background = Blue2Background,
    onSurface = Blue2OnSurface,
    outline = Blue2Outline,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue2PrimaryDark,
    onPrimary = Color(0xFF00315C),
    primaryContainer = Blue2PrimaryContainerDark,
    secondary = Blue2SecondaryDark,
    secondaryContainer = Blue2SecondaryContainerDark,
    tertiary = Blue2TertiaryDark,
    tertiaryContainer = Blue2TertiaryContainerDark,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    surface = Blue2SurfaceDark,
    surfaceVariant = Blue2SurfaceVariantDark,
    background = Blue2BackgroundDark,
    onSurface = Blue2OnSurfaceDark,
    outline = Blue2OutlineDark,
)

private fun amoledColorScheme(base: ColorScheme): ColorScheme = base.copy(
    background = AmoledBackground,
    surface = AmoledSurface,
    surfaceVariant = AmoledSurfaceVariant,
    surfaceContainer = AmoledCard,
    surfaceContainerHigh = AmoledCard,
    surfaceContainerHighest = AmoledCard,
    surfaceContainerLow = AmoledSurface,
    surfaceContainerLowest = AmoledBackground,
)

@Composable
fun Blue2Theme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    amoledMode: Boolean = false,
    seedColor: Color? = null,
    useAtkinsonFont: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && seedColor == null -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        seedColor != null -> {
            // Generate scheme from seed color
            val scheme = MaterialTheme.colorScheme
            scheme // fallback; real dynamic from seed needs m3-expressive APIs
            if (isDark) DarkColorScheme else LightColorScheme
        }
        else -> if (isDark) DarkColorScheme else LightColorScheme
    }

    if (amoledMode && isDark) {
        colorScheme = amoledColorScheme(colorScheme)
    }

    val typography = if (useAtkinsonFont) AtkinsonTypography else DefaultTypography

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        SideEffect {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = Blue2Shapes,
        content = content,
    )
}
