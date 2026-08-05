package com.aditya1875.pokeverse.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// POKEVERSE CLASSIC

private val PokeverseClassicDark = darkColorScheme(

    primary = Color(0xFF5FD3E6),              // Logo Cyan
    onPrimary = Color(0xFF0B1C2D),

    primaryContainer = Color(0xFF1F4E5F),
    onPrimaryContainer = Color(0xFFB8F3FF),

    secondary = Color(0xFFFF7043),            // Logo Orange-Red
    onSecondary = Color(0xFF1A0F0B),

    secondaryContainer = Color(0xFF5C2B1A),
    onSecondaryContainer = Color(0xFFFFDAD0),

    tertiary = Color(0xFF90CAF9),             // Soft highlight blue
    onTertiary = Color(0xFF0B1C2D),

    background = Color(0xFF0B1C2D),           // Deep Navy
    onBackground = Color(0xFFEAF6FF),

    surface = Color(0xFF13293D),
    onSurface = Color(0xFFEAF6FF),

    surfaceVariant = Color(0xFF1C3A52),
    onSurfaceVariant = Color(0xFFC9DDEB),

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF274A66),
    outlineVariant = Color(0xFF1C3A52),
)

// PIKACHU THEME (Electric)

private val PikachuClassicDark = darkColorScheme(

    primary = Color(0xFFFFD600),           // Pikachu Yellow
    onPrimary = Color(0xFF1A1000),

    primaryContainer = Color(0xFF332200),  // Dark warm container (no yellow-on-yellow)
    onPrimaryContainer = Color(0xFFFFE082),

    secondary = Color(0xFFFFC107),         // Amber electric glow
    onSecondary = Color(0xFF1A0E00),

    secondaryContainer = Color(0xFF2B2400), // Dark amber container
    onSecondaryContainer = Color(0xFFFFEE58),

    tertiary = Color(0xFFFF8F00),          // Thunder amber-orange
    onTertiary = Color(0xFF1A0900),

    tertiaryContainer = Color(0xFF331D00),
    onTertiaryContainer = Color(0xFFFFCA28),

    background = Color(0xFF121006),        // Warm dark background
    onBackground = Color(0xFFFFFFFF),      // Pure white — max readability

    surface = Color(0xFF1C1A0D),
    onSurface = Color(0xFFFFFFFF),

    surfaceVariant = Color(0xFF2A2710),
    onSurfaceVariant = Color(0xFFE6C200),  // Rich yellow on dark variant

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF3D3720),
    outlineVariant = Color(0xFF2A2710),
)

val DarkraiDark = darkColorScheme(
    primary = Color(0xFF9B59B6),           // Shadow Purple — Darkrai's aura
    onPrimary = Color(0xFFF5EEFF),

    primaryContainer = Color(0xFF4A235A),   // Deep void purple
    onPrimaryContainer = Color(0xFFE8CFFF),

    secondary = Color(0xFFE53935),          // Crimson eye
    onSecondary = Color(0xFFFFEBEE),

    secondaryContainer = Color(0xFF7B1FA2), // Dark purple
    onSecondaryContainer = Color(0xFFF3E5F5),

    tertiary = Color(0xFFB0BEC5),           // Pale moonlight silver
    onTertiary = Color(0xFF0D0D0D),

    tertiaryContainer = Color(0xFF37474F),
    onTertiaryContainer = Color(0xFFECEFF1),

    background = Color(0xFF050508),         // True void black
    onBackground = Color(0xFFE8E0F0),

    surface = Color(0xFF0D0D14),            // Near-black with faint purple tint
    onSurface = Color(0xFFE8E0F0),

    surfaceVariant = Color(0xFF1A1025),     // Dark purple-black
    onSurfaceVariant = Color(0xFFCBB8E0),

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF3D2B4D),
    outlineVariant = Color(0xFF1A1025),
)

// MEWTWO THEME (Psychic)

private val MewtwoClassicDark = darkColorScheme(

    primary = Color(0xFFCE93D8),           // Psychic lavender
    onPrimary = Color(0xFF1A0030),

    primaryContainer = Color(0xFF3A0060),  // Deep psychic purple
    onPrimaryContainer = Color(0xFFEDD8FF),

    secondary = Color(0xFF80DEEA),         // Laboratory icy teal
    onSecondary = Color(0xFF00292E),

    secondaryContainer = Color(0xFF003C44),
    onSecondaryContainer = Color(0xFFB2EBF2),

    tertiary = Color(0xFFF48FB1),          // Psychic pink beam
    onTertiary = Color(0xFF2E0015),

    tertiaryContainer = Color(0xFF5C0030),
    onTertiaryContainer = Color(0xFFFFD9E4),

    background = Color(0xFF080010),        // Deep psychic void
    onBackground = Color(0xFFF3E5FF),      // Light lavender — readable and on-brand

    surface = Color(0xFF0F0020),
    onSurface = Color(0xFFF3E5FF),

    surfaceVariant = Color(0xFF1A0835),
    onSurfaceVariant = Color(0xFFD8B8F0),

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF4A2870),
    outlineVariant = Color(0xFF1A0835),
)

// UMBREON THEME (Dark / Moonlight)

private val UmbreonClassicDark = darkColorScheme(

    primary = Color(0xFFF5C518),           // Umbreon gold rings
    onPrimary = Color(0xFF1A1000),

    primaryContainer = Color(0xFF3A2800),  // Dark gold container
    onPrimaryContainer = Color(0xFFFFDE80),

    secondary = Color(0xFF82B1FF),         // Blue rings
    onSecondary = Color(0xFF001550),

    secondaryContainer = Color(0xFF002080),
    onSecondaryContainer = Color(0xFFDAE3FF),

    tertiary = Color(0xFF80CBC4),          // Moonlight teal
    onTertiary = Color(0xFF003733),

    tertiaryContainer = Color(0xFF004D49),
    onTertiaryContainer = Color(0xFFB2DFDB),

    background = Color(0xFF060606),        // Moonlit night — true black
    onBackground = Color(0xFFF5F0FF),      // Near-white cool tint

    surface = Color(0xFF0E0E10),
    onSurface = Color(0xFFF5F0FF),

    surfaceVariant = Color(0xFF181818),
    onSurfaceVariant = Color(0xFFD0C8E0),

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF383020),
    outlineVariant = Color(0xFF181818),
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

// GENGAR THEME (Ghost/Poison)

private val GengarDark = darkColorScheme(
    primary = Color(0xFF9C27B0),           // Gengar body purple
    onPrimary = Color(0xFFFDE7FF),

    primaryContainer = Color(0xFF4A0072),  // Deep shadow violet
    onPrimaryContainer = Color(0xFFF0D9FF),

    secondary = Color(0xFFC6FF00),         // Mischievous acid-green grin/eyes
    onSecondary = Color(0xFF1B2600),

    secondaryContainer = Color(0xFF2E3D00),
    onSecondaryContainer = Color(0xFFE6FF8C),

    tertiary = Color(0xFFFF4081),          // Poison glow
    onTertiary = Color(0xFF2E000E),

    tertiaryContainer = Color(0xFF4A0022),
    onTertiaryContainer = Color(0xFFFFD1E3),

    background = Color(0xFF0A0612),        // Shadowy purple-black — Gengar hides here
    onBackground = Color(0xFFF0E5FF),

    surface = Color(0xFF130A22),
    onSurface = Color(0xFFF0E5FF),

    surfaceVariant = Color(0xFF241238),
    onSurfaceVariant = Color(0xFFD9BFEF),

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF5C2E7A),
    outlineVariant = Color(0xFF241238),
)

// RAYQUAZA THEME (Dragon/Flying)

private val RayquazaDark = darkColorScheme(
    primary = Color(0xFF00C853),           // Vivid emerald scales
    onPrimary = Color(0xFF00210B),

    primaryContainer = Color(0xFF00701A),
    onPrimaryContainer = Color(0xFFB9F6CA),

    secondary = Color(0xFFFFD600),         // Gold underbelly markings
    onSecondary = Color(0xFF1A1400),

    secondaryContainer = Color(0xFF4D3F00),
    onSecondaryContainer = Color(0xFFFFF176),

    tertiary = Color(0xFF00E5FF),          // Ozone-layer sky blue
    onTertiary = Color(0xFF00272B),

    tertiaryContainer = Color(0xFF003C40),
    onTertiaryContainer = Color(0xFFB2F5FF),

    background = Color(0xFF061A0F),        // Deep upper-atmosphere night
    onBackground = Color(0xFFE3FFEA),

    surface = Color(0xFF0C2417),
    onSurface = Color(0xFFE3FFEA),

    surfaceVariant = Color(0xFF163826),
    onSurfaceVariant = Color(0xFFB6E2C6),

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF2E5A3E),
    outlineVariant = Color(0xFF163826),
)

// SYLVEON THEME (Fairy)

private val SylveonDark = darkColorScheme(
    primary = Color(0xFFFF8FB1),           // Sylveon ribbon pink
    onPrimary = Color(0xFF3A0018),

    primaryContainer = Color(0xFF5C1030),
    onPrimaryContainer = Color(0xFFFFD9E6),

    secondary = Color(0xFF9FE0FF),         // Baby-blue ribbon accents
    onSecondary = Color(0xFF00293B),

    secondaryContainer = Color(0xFF003E57),
    onSecondaryContainer = Color(0xFFD3F1FF),

    tertiary = Color(0xFFFFF0B3),          // Cream fur highlight
    onTertiary = Color(0xFF3A2E00),

    tertiaryContainer = Color(0xFF554300),
    onTertiaryContainer = Color(0xFFFFF6D6),

    background = Color(0xFF1A0E16),        // Warm dark plum, not harsh black
    onBackground = Color(0xFFFFE9F3),

    surface = Color(0xFF24121F),
    onSurface = Color(0xFFFFE9F3),

    surfaceVariant = Color(0xFF35192E),
    onSurfaceVariant = Color(0xFFE8C2D8),

    error = Color(0xFFCF6679),
    onError = Color.White,

    outline = Color(0xFF5C3350),
    outlineVariant = Color(0xFF35192E),
)

// THEME ENUM

enum class AppTheme {
    DEXVERSE,
    PIKACHU,
    DARKRAI,
    MEWTWO,
    UMBREON,
    CHARIZARD,
    VENUSAUR,
    BLASTOISE,
    GENGAR,
    RAYQUAZA,
    SYLVEON
}

// MAIN THEME COMPOSABLE
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PokeverseTheme(
    selectedTheme: AppTheme = AppTheme.DEXVERSE,
    content: @Composable () -> Unit
) {
    val targetScheme = when (selectedTheme) {
        AppTheme.DEXVERSE -> PokeverseClassicDark
        AppTheme.PIKACHU -> PikachuClassicDark
        AppTheme.DARKRAI -> DarkraiDark
        AppTheme.MEWTWO -> MewtwoClassicDark
        AppTheme.UMBREON -> UmbreonClassicDark
        AppTheme.CHARIZARD -> CharizardDark
        AppTheme.VENUSAUR -> VenusaurDark
        AppTheme.BLASTOISE -> BlastoiseDark
        AppTheme.GENGAR -> GengarDark
        AppTheme.RAYQUAZA -> RayquazaDark
        AppTheme.SYLVEON -> SylveonDark
    }

    // Every color cross-fades to the new palette instead of hard-swapping
    val colorScheme = animatedColorScheme(targetScheme)

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            WindowCompat.setDecorFitsSystemWindows(window, false)

            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false // dark icons? true
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = AppTypography,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            // Expanding energy wave in the new theme's colors on switch
            ThemeSwitchWave(
                themeKey = selectedTheme,
                accent = targetScheme.primary,
                secondary = targetScheme.secondary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Smooth palette cross-fade: each key color animates to its new value
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun animatedColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = 700, easing = FastOutSlowInEasing)

    val primary by animateColorAsState(target.primary, spec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(target.secondary, spec, label = "secondary")
    val onSecondary by animateColorAsState(target.onSecondary, spec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, spec, label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, spec, label = "onSecondaryContainer")
    val tertiary by animateColorAsState(target.tertiary, spec, label = "tertiary")
    val onTertiary by animateColorAsState(target.onTertiary, spec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, spec, label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, spec, label = "onTertiaryContainer")
    val background by animateColorAsState(target.background, spec, label = "background")
    val onBackground by animateColorAsState(target.onBackground, spec, label = "onBackground")
    val surface by animateColorAsState(target.surface, spec, label = "surface")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfaceVariant")
    val outline by animateColorAsState(target.outline, spec, label = "outline")
    val outlineVariant by animateColorAsState(target.outlineVariant, spec, label = "outlineVariant")

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Theme-switch flourish: an energy wave in the new theme's colors ripples out
// from the centre with drifting sparkles, then dissolves. Skips the first
// composition so it only plays on an actual switch.
// ─────────────────────────────────────────────────────────────────────────────
private data class WaveSparkle(val angle: Float, val radiusJitter: Float, val size: Float, val speed: Float)

@Composable
private fun ThemeSwitchWave(
    themeKey: AppTheme,
    accent: Color,
    secondary: Color
) {
    var lastTheme by remember { mutableStateOf(themeKey) }
    val progress = remember { Animatable(1f) }
    val sparkles = remember(themeKey) {
        List(18) {
            WaveSparkle(
                angle = Random.nextFloat() * 360f,
                radiusJitter = 0.82f + Random.nextFloat() * 0.3f,
                size = 2.5f + Random.nextFloat() * 4.5f,
                speed = 0.9f + Random.nextFloat() * 0.25f
            )
        }
    }

    LaunchedEffect(themeKey) {
        if (lastTheme != themeKey) {
            lastTheme = themeKey
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1100, easing = LinearOutSlowInEasing)
            )
        }
    }

    val p = progress.value
    if (p >= 1f) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxRadius = hypot(size.width, size.height) * 0.72f
        val radius = (maxRadius * p).coerceAtLeast(1f)
        val fade = 1f - p

        // Soft full-screen flash in the new accent, gone quickly
        drawRect(accent.copy(alpha = 0.10f * fade * fade))

        // Expanding ring — a band of accent glow with transparent core
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.78f to Color.Transparent,
                    0.90f to accent.copy(alpha = 0.45f * fade),
                    0.96f to secondary.copy(alpha = 0.30f * fade),
                    1.00f to Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Sparkles drifting along the wave front
        sparkles.forEach { s ->
            val sparkleRadius = radius * s.radiusJitter * s.speed
            val rad = Math.toRadians(s.angle.toDouble())
            val pos = Offset(
                x = center.x + (sparkleRadius * cos(rad)).toFloat(),
                y = center.y + (sparkleRadius * sin(rad)).toFloat()
            )
            drawCircle(
                color = accent.copy(alpha = (0.85f * fade).coerceIn(0f, 1f)),
                radius = s.size * (0.5f + 0.5f * fade),
                center = pos
            )
        }
    }
}
