package com.aditya1875.pokeverse.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// POKEVERSE CLASSIC

private val PokeverseClassicDark = darkColorScheme(
    primary = Color(0xFFB63A3A),            // Muted Pokeverse Red
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8E2B2B),
    onPrimaryContainer = Color(0xFFFFDAD6),

    secondary = Color(0xFF3A3A3A),          // Neutral Dark
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFE0E0E0),

    tertiary = Color(0xFF5A5A5A),           // Subtle highlights
    onTertiary = Color.White,

    background = Color(0xFF0E0E0E),         // Near-black (NOT pure black)
    onBackground = Color(0xFFEDEDED),

    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFCFCFCF),

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A),
)

// CHARIZARD THEME (Fire/Flying)

private val CharizardDark = darkColorScheme(
    primary = Color(0xFFFF6D00),           // Charizard Orange
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE65100),   // Deep Orange
    onPrimaryContainer = Color(0xFFFFCCBC),

    secondary = Color(0xFFFF5722),          // Fire Red
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD84315),
    onSecondaryContainer = Color(0xFFFFCCBC),

    tertiary = Color(0xFF0091EA),           // Blue Wings
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0277BD),
    onTertiaryContainer = Color(0xFFB3E5FC),

    background = Color(0xFF1A0F0A),         // Dark Charcoal
    onBackground = Color(0xFFFFE7D6),

    surface = Color(0xFF2A1810),            // Warm Dark Surface
    onSurface = Color(0xFFFFE7D6),
    surfaceVariant = Color(0xFF3D2317),
    onSurfaceVariant = Color(0xFFFFCCBC),

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF5D3A2E),
    outlineVariant = Color(0xFF3D2317),
)

// VENUSAUR THEME

private val VenusaurDark = darkColorScheme(
    primary = Color(0xFF4CAF50),           // Grass Green
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E7D32),   // Deep Green
    onPrimaryContainer = Color(0xFFC8E6C9),

    secondary = Color(0xFF26A69A),          // Teal (Poison)
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF00897B),
    onSecondaryContainer = Color(0xFFB2DFDB),

    tertiary = Color(0xFF66BB6A),           // Light Green
    onTertiary = Color(0xFF1B5E20),
    tertiaryContainer = Color(0xFF43A047),
    onTertiaryContainer = Color(0xFFDCEDC8),

    background = Color(0xFF0D1A0D),         // Dark Forest
    onBackground = Color(0xFFE8F5E9),

    surface = Color(0xFF1A2A1A),            // Deep Green Surface
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF263626),
    onSurfaceVariant = Color(0xFFC8E6C9),

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF3D5A3D),
    outlineVariant = Color(0xFF263626),
)

// BLASTOISE THEME

private val BlastoiseDark = darkColorScheme(
    primary = Color(0xFF2196F3),           // Ocean Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1565C0),   // Deep Blue
    onPrimaryContainer = Color(0xFFBBDEFB),

    secondary = Color(0xFF00BCD4),          // Cyan (Water Cannons)
    onSecondary = Color(0xFF01579B),
    secondaryContainer = Color(0xFF0097A7),
    onSecondaryContainer = Color(0xFFB2EBF2),

    tertiary = Color(0xFF03A9F4),           // Light Blue
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0277BD),
    onTertiaryContainer = Color(0xFFB3E5FC),

    background = Color(0xFF0A1420),         // Deep Ocean
    onBackground = Color(0xFFE1F5FE),

    surface = Color(0xFF15202B),            // Dark Blue Surface
    onSurface = Color(0xFFE1F5FE),
    surfaceVariant = Color(0xFF1E2C3A),
    onSurfaceVariant = Color(0xFFBBDEFB),

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF2C4A5D),
    outlineVariant = Color(0xFF1E2C3A),
)

// THEME ENUM

enum class AppTheme {
    POKEVERSE,
    CHARIZARD,
    VENUSAUR,
    BLASTOISE
}

// MAIN THEME COMPOSABLE
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PokeverseTheme(
    selectedTheme: AppTheme = AppTheme.POKEVERSE,
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        AppTheme.POKEVERSE -> PokeverseClassicDark
        AppTheme.CHARIZARD -> CharizardDark
        AppTheme.VENUSAUR -> VenusaurDark
        AppTheme.BLASTOISE -> BlastoiseDark
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Let content draw behind system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Make status bar transparent
            window.statusBarColor = Color.Transparent.toArgb()

            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false // dark icons? true
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = AppTypography,
        content = content
    )
}
