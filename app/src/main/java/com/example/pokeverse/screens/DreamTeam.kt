package com.example.pokeverse.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pokeverse.data.local.entity.TeamMemberEntity
import com.example.pokeverse.data.remote.model.PokemonResponse
import com.example.pokeverse.ui.viewmodel.PokemonViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamTeam(
    navController: NavController,
    team: List<TeamMemberEntity>,
    onRemove: (TeamMemberEntity) -> Unit,
    viewModel: PokemonViewModel = koinViewModel()
) {
    val pokeballGradient = Brush.verticalGradient(
        listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A))
    )

    var showAnalysis by remember { mutableStateOf(false) }
    val teamDetails by remember(team) {
        derivedStateOf {
            team.mapNotNull { member ->
                viewModel.fetchPokemonData(member.name)
                viewModel.uiState.value.pokemon
            }
        }
    }

    // Comprehensive team rating logic
    fun rateTeam(pokemonList: List<PokemonResponse>): Triple<Double, Map<String, Double>, List<Pair<String, Double>>> {
        if (pokemonList.isEmpty()) return Triple(0.0, emptyMap(), emptyList())

        // Base stat calculation
        val totalBaseStats = pokemonList.sumOf { it.stats.sumOf { s -> s.base_stat } }
        val avgBaseStat = totalBaseStats / (pokemonList.size * 6.0) // Normalize to 6 stats per Pokémon
        val statRating = (avgBaseStat / 150.0) * 100 // Max stat per category is ~150

        // Type coverage and synergy
        val allTypes = pokemonList.flatMap { it.types.map { type -> type.type.name } }
        val uniqueTypes = allTypes.toSet()
        val typeDiversity = (uniqueTypes.size.toDouble() / 18.0) * 100 // 18 types in Pokémon
        val typeOverlapPenalty = allTypes.groupingBy { it }.eachCount().count { it.value > 1 } * 5.0 // Penalty for duplicates

        // Individual contributions
        val contributions = pokemonList.map { pokemon ->
            val pokemonStatContribution = pokemon.stats.sumOf { it.base_stat } / 600.0 * 100 // Per Pokémon contribution
            val typeContribution = if (uniqueTypes.contains(pokemon.types.firstOrNull()?.type?.name)) 10.0 else 0.0 // Bonus for unique type
            pokemon.name to (pokemonStatContribution + typeContribution)
        }

        // Final team score
        val finalScore = (statRating * 0.5 + typeDiversity * 0.4 - typeOverlapPenalty).coerceIn(0.0, 100.0)
        val typeBreakdown = mapOf(
            "Diversity" to typeDiversity,
            "Overlap Penalty" to -typeOverlapPenalty
        )

        return Triple(finalScore, typeBreakdown, contributions)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Dream Team", style = MaterialTheme.typography.headlineSmall, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        val isLoading = viewModel.isLoading

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(brush = pokeballGradient)
        ) {
            if (team.isEmpty() && isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CustomProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(team) { index, pokemon ->
                            var isPressed by remember { mutableStateOf(false) }

                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1f,
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                label = "cardScale"
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clickable(
                                        onClick = {
                                            isPressed = true
                                            navController.navigate("pokemon_detail/${pokemon.name}")
                                        },
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = pokemon.name.replaceFirstChar { it.uppercase() },
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    IconButton(onClick = { onRemove(pokemon) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove from Team",
                                            tint = Color(0xFFFF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Analysis section
                    val (teamRating, typeBreakdown, contributions) = rateTeam(teamDetails)
                    val dominantType = teamDetails.flatMap { it.types.map { it.type.name } }
                        .groupBy { it }
                        .maxByOrNull { it.value.size }?.key ?: "None"
                    val weaknessFeedback = when (dominantType.lowercase()) {
                        "fire" -> "Weak to Water and Rock"
                        "water" -> "Weak to Grass and Electric"
                        "grass" -> "Weak to Fire and Flying"
                        else -> "Balanced type coverage"
                    }

                    if (team.size == 6) {
                        val buttonVisible by animateFloatAsState(
                            targetValue = if (team.size == 6) 1f else 0f,
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            label = "buttonFade"
                        )

                        Column {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                elevation = CardDefaults.cardElevation(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Team Rating: %.1f/100".format(teamRating),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Type Analysis: $dominantType dominant, $weaknessFeedback",
                                        color = Color(0xFFB0B0B0),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Diversity: %.1f%%".format(typeBreakdown["Diversity"]),
                                        color = Color(0xFFB0B0B0),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Overlap Penalty: %.1f".format(typeBreakdown["Overlap Penalty"]),
                                        color = Color(0xFFFF4444),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = team.size == 6,
                                enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(300)),
                                exit = fadeOut()
                            ) {
                                Button(
                                    onClick = { showAnalysis = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp, vertical = 16.dp)
                                        .graphicsLayer { alpha = buttonVisible },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCB05)),
                                    elevation = ButtonDefaults.buttonElevation(4.dp)
                                ) {
                                    Text(
                                        "Analyze Team",
                                        color = Color.Black,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Add ${6 - team.size} more Pokémon to analyze your team!",
                                color = Color(0xFFB0B0B0),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    // Full-screen analysis screen
                    if (showAnalysis) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.9f))
                                .clickable(
                                    onClick = { showAnalysis = false },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "Team Analysis",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Text(
                                    text = "Overall Rating: %.1f/100".format(teamRating),
                                    color = Color(0xFFFFCB05),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Type Breakdown:",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                typeBreakdown.forEach { (key, value) ->
                                    Text(
                                        text = "$key: %.1f%%".format(value),
                                        color = if (key == "Overlap Penalty") Color(0xFFFF4444) else Color(0xFFB0B0B0),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Pokémon Contributions:",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                )
                                contributions.forEach { (name, contribution) ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E)),
                                        elevation = CardDefaults.cardElevation(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = name.replaceFirstChar { it.uppercase() },
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "%.1f%%".format(contribution),
                                                color = Color(0xFFFFCB05),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = { showAnalysis = false },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = 16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCB05))
                                ) {
                                    Text("Back", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}