package com.aditya1875.pokeverse.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.components.AnimatedBackground
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.ui.viewmodel.SettingsViewModel
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(
    pokemonName: String,
    navController: NavController
) {
    val viewModel: PokemonViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val specialEffectsEnabled by settingsViewModel.specialEffectsEnabled.collectAsStateWithLifecycle()
    val stages by viewModel.evolutionStages.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(pokemonName) {
        viewModel.fetchPokemonData(pokemonName)
        viewModel.fetchVarietyPokemon(pokemonName)
    }

    // fetch evolution chain when available
    LaunchedEffect(uiState.evolutionChainId) {
        uiState.evolutionChainId?.let(viewModel::fetchEvolutionChain)
    }

    // pager setup
    val initialPage = remember(stages, pokemonName) {
        stages.indexOfFirst { it.name.equals(pokemonName, ignoreCase = true) }
            .coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage)

    LaunchedEffect(pagerState, stages) {
        snapshotFlow { pagerState.currentPage }
            .collectLatest { page ->
                stages.getOrNull(page)?.let { stage ->
                    if (!stage.name.equals(uiState.pokemon?.name, ignoreCase = true)) {
                        viewModel.fetchPokemonData(stage.name)
                        viewModel.fetchVarietyPokemon(stage.name)
                    }
                }
            }
    }

    // global state for sprite effects (shared across pages)
    val spriteEffectsEnabledState = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (stages.isEmpty()) {
            PokemonDetailPage(
                currentStage = null,
                showLeftConnector = false,
                showRightConnector = false,
                onConnectorClick = {}, // no-op
                navController = navController,
                specialEffectsEnabled = specialEffectsEnabled,
                spriteEffectsEnabledState = spriteEffectsEnabledState
            )
        } else {
            HorizontalPager(
                count = stages.size,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val stage = stages[page]
                PokemonDetailPage(
                    currentStage = stage,
                    showLeftConnector = stage.hasPrev,
                    showRightConnector = stage.hasNext,
                    onConnectorClick = { targetId ->
                        val idx = stages.indexOfFirst { it.id == targetId }
                        if (idx >= 0) {
                            scope.launch { pagerState.animateScrollToPage(idx) }
                        }
                    },
                    navController = navController,
                    specialEffectsEnabled = specialEffectsEnabled,
                    spriteEffectsEnabledState = spriteEffectsEnabledState
                )
            }
        }
    }
}

@Composable
fun GlossyCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .graphicsLayer {
                alpha = 0.9f
                shadowElevation = 4f
                shape = RoundedCornerShape(20.dp)
                clip = true
            }
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.Black.copy(alpha = 0.5f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        content = content
    )
}

fun getPokemonBackgroundColor(name: String, types: List<String>): Color {
    val pokemonVariantColors = mapOf(
        "charizard-mega-x" to Color(0xFF1e3c72),  // Custom blue flame color
        "charizard-mega-y" to Color(0xFFE08503),  // Red like fire type
        // Mega Mewtwo
        "mewtwo-mega-x" to Color(0xFF4F1026),   // Amethyst purple for Mega Mewtwo X
        "mewtwo-mega-y" to Color(0xFF4C1156),   // Deep sky blue for Mega Mewtwo Y

        // Mega Rayquaza
        "rayquaza-mega"   to Color(0xFF19501C),   // Light Sea Green for a fresh look

        // Mega Gengar
        "gengar-mega"     to Color(0xFF800080),   // Classic purple for Mega Gengar

        // Mega Venusaur
        "venusaur-mega"   to Color(0xFF2E8B57),   // Sea Green gives a grounded, earthy tone

        // Mega Blastoise
        "blastoise-mega"  to Color(0xFF008DF1),    // Steel Blue for a cool, resolute vibe

        "fire" to Color(0xFFEC5343),
        "water" to Color(0xFF30A2BE),
        "grass" to Color(0xFF56ab2f),
        "electric" to Color(0xFFFFE000),
        "psychic" to Color(0xFFFC5C7D),
        "ice" to Color(0xFF83a4d4),
        "dragon" to Color(0xFF1e3c72),
        "dark" to Color(0xFF434343),
        "fairy" to Color(0xFFF78CA0),
        "fighting" to Color(0xFFC31432),
        "rock" to Color(0xFF3C3B3F),
        "ground" to Color(0xFFD1913C),
        "bug" to Color(0xFFa8e063),
        "ghost" to Color(0xFF606c88),
        "steel" to Color(0xFFbdc3c7),
        "poison" to Color(0xFF9D50BB),
        "normal" to Color(0xFFC4C3A5),
        "flying" to Color(0xFF89F7FE)
    )

    // Try variant-specific color first
    val formColor = pokemonVariantColors[name.lowercase()]
    if (formColor != null) return formColor

    // Fallback to type-based color
    for (type in types) {
        val typeColor = pokemonVariantColors[type.lowercase()]
        if (typeColor != null) return typeColor
    }

    // Final fallback
    return Color.LightGray
}
