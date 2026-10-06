package com.aditya1875.pokeverse.feature.pokemon.theme_selector

import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.pokemon.theme_selector.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.ui.theme.AppTheme
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorScreen(
    navController: NavController,
    themePreferences: ThemePreferences = koinInject(),
    onThemeSelected: (AppTheme) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentTheme by themePreferences.selectedTheme.collectAsState(initial = AppTheme.DEXVERSE)

    val billingViewModel: BillingViewModel = koinViewModel()
    val subscriptionState by billingViewModel.subscriptionState.collectAsStateWithLifecycle()

    val isPremium = subscriptionState is SubscriptionState.Premium

    val themes = remember { getStarterThemes() }

    // What's actually applied (mirrors MainActivity). Display-only: the saved choice is never
    // overwritten, so a premium user whose billing is still loading can't lose their theme.
    val safeTheme = if (currentTheme.isPremium && subscriptionState is SubscriptionState.Free) {
        AppTheme.DEXVERSE
    } else currentTheme
    var showPremiumSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.theme_choose_your_vibe),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        val selectedThemeData = themes.firstOrNull { it.theme == safeTheme }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Live gradient banner for selected theme
            item {
                selectedThemeData?.let { active ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                Brush.linearGradient(active.colors)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = active.emoji,
                                fontSize = 36.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = active.pokemonName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.theme_active),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }

            item {
                Text(
                    text = stringResource(R.string.theme_choose_your_vibe),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.theme_tap_to_apply),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            items(themes) { starterTheme ->
                val locked = starterTheme.premium && !isPremium
                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                    StarterThemeCard(
                        starterTheme = starterTheme,
                        isSelected = safeTheme == starterTheme.theme,
                        isLocked = locked,
                        onClick = {
                            if (starterTheme.premium && !isPremium) {
                                showPremiumSheet = true
                                return@StarterThemeCard
                            }
                            scope.launch {
                                themePreferences.setTheme(starterTheme.theme)
                                onThemeSelected(starterTheme.theme)
                            }
                        }
                    )
                }
            }
        }
    }
}

data class StarterTheme(
    val theme: AppTheme,
    val pokemonName: String,
    val pokemonNumber: String,
    val type: String,
    val emoji: String,
    val description: String,
    val colors: List<Color>
) {
    val premium: Boolean get() = theme.isPremium
}

fun getStarterThemes(): List<StarterTheme> = listOf(
    StarterTheme(
        theme = AppTheme.DEXVERSE,
        pokemonName = "Dexverse",
        pokemonNumber = "Brand",
        type = "Official Theme",
        emoji = "✨",
        description = "The classic Dexverse theme",
        colors = listOf(
            Color(0xFF7C4DFF), // Purple
            Color(0xFF00E5FF), // Neon cyan
            Color(0xFF0B0F1A)  // Deep space navy
        )
    ),

    StarterTheme(
        theme = AppTheme.PIKACHU,
        pokemonName = "Pikachu",
        pokemonNumber = "#025",
        type = "Electric",
        emoji = "⚡",
        description = "Bright and energetic like everyone's favorite electric mouse",
        colors = listOf(
            Color(0xFFFFD600), // Pikachu Yellow
            Color(0xFFFFEA00), // Bright Yellow
            Color(0xFF212121)
        )
    ),

    StarterTheme(
        theme = AppTheme.DARKRAI,
        pokemonName = "Darkrai",
        pokemonNumber = "#491",
        type = "Dark",
        emoji = "🖤",
        description = "For those who prefer the dark side",
        colors = listOf(Color(0xFF9B59B6), Color(0xFF050508), Color(0xFFE53935))
    ),

    StarterTheme(
        theme = AppTheme.MEWTWO,
        pokemonName = "Mewtwo",
        pokemonNumber = "#150",
        type = "Psychic",
        emoji = "🔮",
        description = "Pure psychic power. Clinical, mysterious, and dangerously elegant",
        colors = listOf(
            Color(0xFFCE93D8), // Psychic lavender
            Color(0xFF080010), // Void black
            Color(0xFF80DEEA)  // Lab teal
        )
    ),

    StarterTheme(
        theme = AppTheme.UMBREON,
        pokemonName = "Umbreon",
        pokemonNumber = "#197",
        type = "Dark",
        emoji = "🌙",
        description = "Sleek as midnight with rings that glow under the moon",
        colors = listOf(
            Color(0xFFF5C518), // Gold rings
            Color(0xFF060606), // Night black
            Color(0xFF82B1FF)  // Blue rings
        )
    ),

    StarterTheme(
        theme = AppTheme.CHARIZARD,
        pokemonName = "Charizard",
        pokemonNumber = "#006",
        type = "Fire • Flying",
        emoji = "🔥",
        description = "Fierce and bold like a fire-breathing dragon",
        colors = listOf(
            Color(0xFFFF6D00),  // Charizard Orange
            Color(0xFFE65100),  // Deep Orange
            Color(0xFF0091EA)   // Blue Wings
        )
    ),
    StarterTheme(
        theme = AppTheme.VENUSAUR,
        pokemonName = "Venusaur",
        pokemonNumber = "#003",
        type = "Grass • Poison",
        emoji = "🌿",
        description = "Fresh and vibrant like a blooming garden",
        colors = listOf(
            Color(0xFF4CAF50),  // Grass Green
            Color(0xFF2E7D32),  // Deep Green
            Color(0xFF26A69A)   // Teal
        )
    ),
    StarterTheme(
        theme = AppTheme.BLASTOISE,
        pokemonName = "Blastoise",
        pokemonNumber = "#009",
        type = "Water",
        emoji = "💧",
        description = "Cool and calm like the deep ocean",
        colors = listOf(
            Color(0xFF2196F3),  // Ocean Blue
            Color(0xFF1565C0),  // Deep Blue
            Color(0xFF00BCD4)   // Cyan
        )
    ),

    StarterTheme(
        theme = AppTheme.GENGAR,
        pokemonName = "Gengar",
        pokemonNumber = "#094",
        type = "Ghost • Poison",
        emoji = "👻",
        description = "Mischievous and spooky — shadow purple with a wicked green grin",
        colors = listOf(
            Color(0xFF9C27B0), // Gengar Purple
            Color(0xFF0A0612), // Shadow Black
            Color(0xFFC6FF00)  // Acid Green
        )
    ),

    StarterTheme(
        theme = AppTheme.RAYQUAZA,
        pokemonName = "Rayquaza",
        pokemonNumber = "#384",
        type = "Dragon • Flying",
        emoji = "🐉",
        description = "Legendary sky serpent — emerald scales and gold markings above the clouds",
        colors = listOf(
            Color(0xFF00C853), // Emerald Scales
            Color(0xFF061A0F), // Upper Atmosphere
            Color(0xFFFFD600)  // Gold Markings
        )
    ),

    StarterTheme(
        theme = AppTheme.SYLVEON,
        pokemonName = "Sylveon",
        pokemonNumber = "#700",
        type = "Fairy",
        emoji = "🎀",
        description = "Soft and sweet — pastel ribbons and gentle warmth",
        colors = listOf(
            Color(0xFFFF8FB1), // Ribbon Pink
            Color(0xFF1A0E16), // Warm Plum
            Color(0xFF9FE0FF)  // Baby Blue
        )
    ),

    StarterTheme(
        theme = AppTheme.LUGIA,
        pokemonName = "Lugia",
        pokemonNumber = "#249",
        type = "Psychic • Flying",
        emoji = "🌊",
        description = "Guardian of the seas — silver plumage over midnight ocean blue",
        colors = listOf(
            Color(0xFFB8CCEB), // Silver Plumage
            Color(0xFF050D1A), // Midnight Ocean
            Color(0xFF4DD0E1)  // Storm Teal
        )
    ),

    StarterTheme(
        theme = AppTheme.LUCARIO,
        pokemonName = "Lucario",
        pokemonNumber = "#448",
        type = "Fighting • Steel",
        emoji = "🔵",
        description = "Master of aura — glowing blue against steel black",
        colors = listOf(
            Color(0xFF42A5F5), // Aura Blue
            Color(0xFF080B12), // Steel Black
            Color(0xFFFFE0A3)  // Cream Fur
        )
    ),

    StarterTheme(
        theme = AppTheme.GRENINJA,
        pokemonName = "Greninja",
        pokemonNumber = "#658",
        type = "Water • Dark",
        emoji = "🥷",
        description = "Silent shinobi — deep blue night with a flash of scarf pink",
        colors = listOf(
            Color(0xFF4F7FE0), // Ninja Blue
            Color(0xFF060A16), // Night Mist
            Color(0xFFFF6F91)  // Scarf Pink
        )
    ),

    StarterTheme(
        theme = AppTheme.MIMIKYU,
        pokemonName = "Mimikyu",
        pokemonNumber = "#778",
        type = "Ghost • Fairy",
        emoji = "🎭",
        description = "Just wants to be loved — a homemade yellow disguise over something darker",
        colors = listOf(
            Color(0xFFF2D45C), // Costume Yellow
            Color(0xFF0C0A07), // What Hides Beneath
            Color(0xFFFF8A80)  // Scribbled Cheeks
        )
    ),

    StarterTheme(
        theme = AppTheme.HO_OH,
        pokemonName = "Ho-Oh",
        pokemonNumber = "#250",
        type = "Fire • Flying",
        emoji = "🔥",
        description = "Rainbow phoenix — sacred gold fire with crimson and emerald wings",
        colors = listOf(
            Color(0xFFFFB300), // Sacred Fire
            Color(0xFF140905), // Ember Sky
            Color(0xFFEF5350)  // Crimson Plumage
        )
    )
)

@Composable
fun StarterThemeCard(
    starterTheme: StarterTheme,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(if (isLocked) 0.7f else 1f)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        brush = Brush.linearGradient(starterTheme.colors),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else Modifier
            )
            .clickable(
                enabled = !isLocked,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 12.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Column(modifier = Modifier.weight(1f)) {

                    if (starterTheme.theme == AppTheme.DEXVERSE) {
                        Text(
                            text = stringResource(R.string.theme_classic),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Text(
                            text = starterTheme.emoji,
                            fontSize = 32.sp
                        )

                        Spacer(Modifier.width(12.dp))

                        Column {

                            Text(
                                text = starterTheme.pokemonName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = if (starterTheme.theme == AppTheme.DEXVERSE) stringResource(R.string.theme_brand) else starterTheme.pokemonNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = stringResource(R.string.theme_premium_a11y),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                } else if (isSelected) {

                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(starterTheme.colors)
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.filter_selected),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                } else {

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = starterTheme.colors[0].copy(alpha = 0.15f)
            ) {

                Text(
                    text = starterTheme.theme.localizedTypes(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = starterTheme.colors[0],
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(starterTheme.theme.descriptionRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                starterTheme.colors.forEach { color ->

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = 2.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

/** Type ids shown under each theme card ("Fire • Flying"), rendered with localized type names. */
private fun AppTheme.typeIds(): List<String> = when (this) {
    AppTheme.DEXVERSE -> emptyList()
    AppTheme.PIKACHU -> listOf("electric")
    AppTheme.DARKRAI -> listOf("dark")
    AppTheme.MEWTWO -> listOf("psychic")
    AppTheme.UMBREON -> listOf("dark")
    AppTheme.CHARIZARD -> listOf("fire", "flying")
    AppTheme.VENUSAUR -> listOf("grass", "poison")
    AppTheme.BLASTOISE -> listOf("water")
    AppTheme.GENGAR -> listOf("ghost", "poison")
    AppTheme.RAYQUAZA -> listOf("dragon", "flying")
    AppTheme.SYLVEON -> listOf("fairy")
    AppTheme.LUGIA -> listOf("psychic", "flying")
    AppTheme.LUCARIO -> listOf("fighting", "steel")
    AppTheme.GRENINJA -> listOf("water", "dark")
    AppTheme.MIMIKYU -> listOf("ghost", "fairy")
    AppTheme.HO_OH -> listOf("fire", "flying")
}

@Composable
private fun AppTheme.localizedTypes(): String =
    typeIds().takeIf { it.isNotEmpty() }?.map { localizedTypeName(it) }?.joinToString(" • ")
        ?: stringResource(R.string.theme_official)

@androidx.annotation.StringRes
private fun AppTheme.descriptionRes(): Int = when (this) {
    AppTheme.DEXVERSE -> R.string.theme_desc_dexverse
    AppTheme.PIKACHU -> R.string.theme_desc_pikachu
    AppTheme.DARKRAI -> R.string.theme_desc_darkrai
    AppTheme.MEWTWO -> R.string.theme_desc_mewtwo
    AppTheme.UMBREON -> R.string.theme_desc_umbreon
    AppTheme.CHARIZARD -> R.string.theme_desc_charizard
    AppTheme.VENUSAUR -> R.string.theme_desc_venusaur
    AppTheme.BLASTOISE -> R.string.theme_desc_blastoise
    AppTheme.GENGAR -> R.string.theme_desc_gengar
    AppTheme.RAYQUAZA -> R.string.theme_desc_rayquaza
    AppTheme.SYLVEON -> R.string.theme_desc_sylveon
    AppTheme.LUGIA -> R.string.theme_desc_lugia
    AppTheme.LUCARIO -> R.string.theme_desc_lucario
    AppTheme.GRENINJA -> R.string.theme_desc_greninja
    AppTheme.MIMIKYU -> R.string.theme_desc_mimikyu
    AppTheme.HO_OH -> R.string.theme_desc_hooh
}
