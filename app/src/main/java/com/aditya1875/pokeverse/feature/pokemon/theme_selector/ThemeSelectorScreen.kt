package com.aditya1875.pokeverse.feature.pokemon.theme_selector

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.premium.components.PremiumBottomSheet
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.ProChip
import com.aditya1875.pokeverse.feature.pokemon.theme_selector.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import com.aditya1875.pokeverse.ui.theme.AppTheme
import com.aditya1875.pokeverse.feature.progression.data.ProgressionRepository
import com.aditya1875.pokeverse.feature.progression.data.ProgressionState
import com.aditya1875.pokeverse.feature.progression.domain.Cosmetics
import com.aditya1875.pokeverse.feature.core.navigation.components.Route
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.rememberAdaptiveHPadding
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Theme picker: each theme is shown with its Pokémon's official artwork on the theme's own
 * palette (no emoji), in Free and Pro sections. Tapping applies instantly, so the whole screen
 * recolours as a live preview; a locked Pro theme opens the plans.
 */
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

    // Pro themes bought with Poké Coins work for everyone, Pro or not.
    val progression: ProgressionRepository = koinInject()
    val progressionState by progression.state.collectAsState(initial = ProgressionState())
    val coinThemes = Cosmetics.themeUnlockedBy(progressionState.owned)

    val themes = remember { getStarterThemes() }

    // What's actually applied (mirrors MainActivity). Display-only: the saved choice is never
    // overwritten, so a premium user whose billing is still loading can't lose their theme.
    val safeTheme = if (currentTheme.isPremium && subscriptionState is SubscriptionState.Free && currentTheme.name !in coinThemes) {
        AppTheme.DEXVERSE
    } else currentTheme
    var showPremiumSheet by remember { mutableStateOf(false) }

    val hPad = rememberAdaptiveHPadding()
    val (freeThemes, proThemes) = remember(themes) { themes.partition { !it.premium } }

    fun choose(theme: StarterTheme) {
        if (theme.premium && !isPremium && theme.theme.name !in coinThemes) {
            showPremiumSheet = true
            return
        }
        scope.launch {
            themePreferences.setTheme(theme.theme)
            onThemeSelected(theme.theme)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_choose_your_vibe), color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = hPad, end = hPad, top = 12.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            themes.firstOrNull { it.theme == safeTheme }?.let { active ->
                item(key = "hero", span = { GridItemSpan(maxLineSpan) }) { ActiveThemeHero(active) }
            }

            item(key = "free_header", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(title = stringResource(R.string.theme_section_free), subtitle = stringResource(R.string.theme_tap_to_apply))
            }
            items(freeThemes, key = { it.theme.name }) { theme ->
                ThemeArtCard(theme, isSelected = safeTheme == theme.theme, isLocked = false) { choose(theme) }
            }

            item(key = "pro_header", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = stringResource(R.string.theme_section_pro),
                    subtitle = stringResource(
                        if (isPremium) R.string.theme_section_pro_owned else R.string.theme_section_pro_locked,
                        proThemes.size
                    ),
                    trailing = {
                        if (!isPremium) {
                            TextButton(onClick = { showPremiumSheet = true }) {
                                Text(stringResource(R.string.theme_unlock_all))
                            }
                        }
                    },
                    showProChip = true
                )
            }
            items(proThemes, key = { it.theme.name }) { theme ->
                ThemeArtCard(
                    theme,
                    isSelected = safeTheme == theme.theme,
                    isLocked = !isPremium && theme.theme.name !in coinThemes
                ) { choose(theme) }
            }

            if (!isPremium) {
                item(key = "coin_themes", span = { GridItemSpan(maxLineSpan) }) {
                    TextButton(onClick = { navController.navigate(Route.Rewards.route) }) {
                        Text(stringResource(R.string.theme_unlock_with_coins))
                    }
                }
            }
        }
    }

    if (showPremiumSheet) PremiumBottomSheet(onDismiss = { showPremiumSheet = false })
}

// ─────────────────────────────────────────────────────────────────────────────
// Pieces
// ─────────────────────────────────────────────────────────────────────────────

/** Big card for the theme in use: artwork on its palette, name, types and description. */
@Composable
private fun ActiveThemeHero(theme: StarterTheme) {
    val start by animateColorAsState(theme.colors[0], label = "heroStart")
    val end by animateColorAsState(theme.colors.getOrElse(1) { theme.colors[0] }, label = "heroEnd")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(start.copy(alpha = 0.9f), end)))
    ) {
        ThemeArt(theme, size = 168.dp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp, end = 150.dp)
        ) {
            Text(
                stringResource(R.string.theme_active).uppercase(),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            Text(
                theme.pokemonName,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                theme.theme.localizedTypes(),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(theme.theme.descriptionRes()),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    showProChip: Boolean = false,
    trailing: @Composable () -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                if (showProChip) {
                    Spacer(Modifier.width(8.dp))
                    ProChip(onClick = {})
                }
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}

/**
 * One theme: artwork on the theme's gradient, then name, types and a palette strip. Selected =
 * gradient border + check; locked = PRO chip and a slightly muted art (still tappable: it
 * opens the plans).
 */
@Composable
private fun ThemeArtCard(theme: StarterTheme, isSelected: Boolean, isLocked: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        if (isSelected) 1.03f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "cardScale"
    )
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (isSelected) Modifier.border(2.5.dp, Brush.linearGradient(theme.colors), shape)
                else Modifier
            )
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .clipToBounds() // keep the spotlight inside the art area
                    .background(
                        Brush.verticalGradient(
                            listOf(theme.colors[0].copy(alpha = 0.55f), theme.colors.getOrElse(1) { theme.colors[0] }.copy(alpha = 0.9f))
                        )
                    )
                    .drawBehind {
                        // Soft spotlight behind the Pokémon.
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                                center = center.copy(y = size.height * 0.62f),
                                radius = size.minDimension * 0.55f
                            ),
                            radius = size.minDimension * 0.55f,
                            center = center.copy(y = size.height * 0.62f)
                        )
                    }
            ) {
                ThemeArt(
                    theme,
                    size = 112.dp,
                    muted = isLocked,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                )
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    when {
                        isSelected -> Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.filter_selected),
                                tint = theme.colors[0].takeIf { it.luminanceOk() } ?: Color.Black,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        isLocked -> ProChip(onClick = onClick)
                    }
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    theme.pokemonName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    theme.theme.localizedTypes(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                // The palette as one strip, the way it'll actually sit together in the app.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                ) {
                    theme.colors.forEach { c -> Box(Modifier.weight(1f).fillMaxSize().background(c)) }
                }
            }
        }
    }
}

/** Official artwork for a Pokémon theme, the Dexverse wordmark for the brand theme. */
@Composable
private fun ThemeArt(theme: StarterTheme, size: Dp, modifier: Modifier = Modifier, muted: Boolean = false) {
    val filter = if (muted) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.55f) }) else null
    val dexId = theme.dexId
    if (dexId == null) {
        Image(
            painter = painterResource(theme.brandArt),
            contentDescription = theme.pokemonName,
            contentScale = ContentScale.Fit,
            colorFilter = filter,
            modifier = modifier.width(size * 1.15f).height(size * 0.62f)
        )
    } else {
        val context = LocalContext.current
        AsyncImage(
            model = remember(dexId) { ImageRequest.Builder(context).data(officialArtwork(dexId)).crossfade(true).build() },
            contentDescription = theme.pokemonName,
            contentScale = ContentScale.Fit,
            colorFilter = filter,
            modifier = modifier
                .size(size)
                .alpha(if (muted) 0.9f else 1f)
        )
    }
}

private fun officialArtwork(id: Int) =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"

/** True when a colour is dark enough to read as a check mark on white. */
private fun Color.luminanceOk(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.75f

// ─────────────────────────────────────────────────────────────────────────────
// Data
// ─────────────────────────────────────────────────────────────────────────────

data class StarterTheme(
    val theme: AppTheme,
    val pokemonName: String,
    /** National Dex number for the artwork; null for the brand theme. */
    val dexId: Int?,
    val colors: List<Color>,
    @DrawableRes val brandArt: Int = R.drawable.dexverselogo,
) {
    val premium: Boolean get() = theme.isPremium
}

fun getStarterThemes(): List<StarterTheme> = listOf(
    StarterTheme(AppTheme.DEXVERSE, "Dexverse", null, listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF0B0F1A))),
    StarterTheme(AppTheme.CHARIZARD, "Charizard", 6, listOf(Color(0xFFFF6D00), Color(0xFFE65100), Color(0xFF0091EA))),
    StarterTheme(AppTheme.VENUSAUR, "Venusaur", 3, listOf(Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFF26A69A))),
    StarterTheme(AppTheme.BLASTOISE, "Blastoise", 9, listOf(Color(0xFF2196F3), Color(0xFF1565C0), Color(0xFF00BCD4))),
    StarterTheme(AppTheme.PIKACHU, "Pikachu", 25, listOf(Color(0xFFFFD600), Color(0xFFFFEA00), Color(0xFF212121))),
    StarterTheme(AppTheme.DARKRAI, "Darkrai", 491, listOf(Color(0xFF9B59B6), Color(0xFF050508), Color(0xFFE53935))),
    StarterTheme(AppTheme.MEWTWO, "Mewtwo", 150, listOf(Color(0xFFCE93D8), Color(0xFF080010), Color(0xFF80DEEA))),
    StarterTheme(AppTheme.UMBREON, "Umbreon", 197, listOf(Color(0xFFF5C518), Color(0xFF060606), Color(0xFF82B1FF))),
    StarterTheme(AppTheme.GENGAR, "Gengar", 94, listOf(Color(0xFF9C27B0), Color(0xFF0A0612), Color(0xFFC6FF00))),
    StarterTheme(AppTheme.RAYQUAZA, "Rayquaza", 384, listOf(Color(0xFF00C853), Color(0xFF061A0F), Color(0xFFFFD600))),
    StarterTheme(AppTheme.SYLVEON, "Sylveon", 700, listOf(Color(0xFFFF8FB1), Color(0xFF1A0E16), Color(0xFF9FE0FF))),
    StarterTheme(AppTheme.LUGIA, "Lugia", 249, listOf(Color(0xFFB8CCEB), Color(0xFF050D1A), Color(0xFF4DD0E1))),
    StarterTheme(AppTheme.LUCARIO, "Lucario", 448, listOf(Color(0xFF42A5F5), Color(0xFF080B12), Color(0xFFFFE0A3))),
    StarterTheme(AppTheme.GRENINJA, "Greninja", 658, listOf(Color(0xFF4F7FE0), Color(0xFF060A16), Color(0xFFFF6F91))),
    StarterTheme(AppTheme.MIMIKYU, "Mimikyu", 778, listOf(Color(0xFFF2D45C), Color(0xFF0C0A07), Color(0xFFFF8A80))),
    StarterTheme(AppTheme.HO_OH, "Ho-Oh", 250, listOf(Color(0xFFFFB300), Color(0xFF140905), Color(0xFFEF5350))),
)

/** Type ids shown under each theme ("Fire • Flying"), rendered with localized type names. */
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
