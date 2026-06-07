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

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppThemeStyle {
    MATERIAL_EXPRESSIVE,  // Google Material 3 Expressive — default
    LIQUID_GLASS,         // Apple-inspired glass morphism
    ONE_UI,               // Samsung OneUI 8.5/9 style
}

// ─── Material 3 Expressive ────────────────────────────────────────────────────

private val M3ExpressiveLightColors = lightColorScheme(
    primary = Blue2Primary,
    onPrimary = Color.White,
    primaryContainer = Blue2PrimaryContainer,
    onPrimaryContainer = Blue2Dark,
    secondary = Blue2Secondary,
    onSecondary = Color.White,
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

private val M3ExpressiveDarkColors = darkColorScheme(
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

// ─── Liquid Glass (Apple-inspired) ────────────────────────────────────────────

private val LiquidGlassLightColors = lightColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF00325A),
    secondary = Color(0xFF30B0C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F4F9),
    tertiary = Color(0xFF5E5CE6),
    tertiaryContainer = Color(0xFFEAEAFD),
    surface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFFE5E5EA),
    background = Color(0xFFF2F2F7),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF3A3A3C),
    outline = Color(0xFFC7C7CC),
    error = Color(0xFFFF3B30),
    errorContainer = Color(0xFFFFDDD9),
)

private val LiquidGlassDarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00325A),
    onPrimaryContainer = Color(0xFFADD4FF),
    secondary = Color(0xFF5AC8FA),
    onSecondary = Color(0xFF003444),
    secondaryContainer = Color(0xFF004D63),
    tertiary = Color(0xFFBF5AF2),
    tertiaryContainer = Color(0xFF3A0070),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    background = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline = Color(0xFF38383A),
    error = Color(0xFFFF453A),
    errorContainer = Color(0xFF7C0000),
)

// ─── Samsung OneUI ────────────────────────────────────────────────────────────

private val OneUiLightColors = lightColorScheme(
    primary = Color(0xFF1259C3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE8FF),
    onPrimaryContainer = Color(0xFF001947),
    secondary = Color(0xFF555F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFF6E54A1),
    tertiaryContainer = Color(0xFFEEDCFF),
    surface = Color(0xFFF8F9FF),
    surfaceVariant = Color(0xFFE0E2EC),
    background = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C22),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF747780),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

private val OneUiDarkColors = darkColorScheme(
    primary = Color(0xFFB2C5FF),
    onPrimary = Color(0xFF002B76),
    primaryContainer = Color(0xFF003FA3),
    onPrimaryContainer = Color(0xFFDBE8FF),
    secondary = Color(0xFFBBC7DC),
    onSecondary = Color(0xFF253141),
    secondaryContainer = Color(0xFF3C4859),
    tertiary = Color(0xFFD5BAFF),
    tertiaryContainer = Color(0xFF553C87),
    surface = Color(0xFF111318),
    surfaceVariant = Color(0xFF44474F),
    background = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
)

// ─── AMOLED overlay ───────────────────────────────────────────────────────────

private fun amoledColorScheme(base: ColorScheme) = base.copy(
    background = AmoledBackground,
    surface = AmoledSurface,
    surfaceVariant = AmoledSurfaceVariant,
    surfaceContainer = AmoledCard,
    surfaceContainerHigh = AmoledCard,
    surfaceContainerHighest = AmoledCard,
    surfaceContainerLow = AmoledSurface,
    surfaceContainerLowest = AmoledBackground,
)

// ─── Shapes per theme ─────────────────────────────────────────────────────────

private val oneUiShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32),
)

// ─── Theme LocalComposition ───────────────────────────────────────────────────

val LocalAppThemeStyle = staticCompositionLocalOf { AppThemeStyle.MATERIAL_EXPRESSIVE }

@Composable
fun Blue2Theme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeStyle: AppThemeStyle = AppThemeStyle.MATERIAL_EXPRESSIVE,
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
        // Dynamic color only applies to M3 Expressive
        themeStyle == AppThemeStyle.MATERIAL_EXPRESSIVE &&
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && seedColor == null -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (themeStyle) {
            AppThemeStyle.MATERIAL_EXPRESSIVE -> if (isDark) M3ExpressiveDarkColors else M3ExpressiveLightColors
            AppThemeStyle.LIQUID_GLASS -> if (isDark) LiquidGlassDarkColors else LiquidGlassLightColors
            AppThemeStyle.ONE_UI -> if (isDark) OneUiDarkColors else OneUiLightColors
        }
    }

    if (amoledMode && isDark) {
        colorScheme = amoledColorScheme(colorScheme)
    }

    val typography = if (useAtkinsonFont) AtkinsonTypography else DefaultTypography
    val shapes = if (themeStyle == AppThemeStyle.ONE_UI) oneUiShapes else Blue2Shapes

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

    CompositionLocalProvider(LocalAppThemeStyle provides themeStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}
