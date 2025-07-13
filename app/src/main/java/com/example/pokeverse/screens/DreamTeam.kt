package com.example.pokeverse.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pokeverse.data.local.entity.TeamMemberEntity
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

    // Fetch full Pokemon data for the team
    val teamDetails = remember(team) {
        team.mapNotNull { member ->
            viewModel.fetchPokemonData(member.name) // Trigger fetch
            viewModel.uiState.value.pokemon // Get the latest fetched data
        } // Remove nulls if fetch is async
    }

    // Calculate team strength
    val teamStrength = if (teamDetails.isNotEmpty()) {
        val totalStats = teamDetails.sumOf { pokemon ->
            pokemon.stats.sumOf { it.base_stat } // Sum of base stats
        }
        val avgStats = totalStats / teamDetails.size / 6 // Normalize to 600
        (avgStats * 100 / 100).coerceIn(0, 100) // Scale to 0-100
    } else 0

    // Type analysis
    val typeDistribution = teamDetails.flatMap { it.types }
        .groupBy { it.type.name }.mapValues { it.value.size }
    val dominantType = typeDistribution.maxByOrNull { it.value }?.key ?: "None"
    val weaknessFeedback = when (dominantType.lowercase()) {
        "fire" -> "Weak to Water and Rock"
        "water" -> "Weak to Grass and Electric"
        "grass" -> "Weak to Fire and Flying"
        else -> "Balanced type coverage"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dream Team") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val isLoading = viewModel.isLoading

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 110.dp)
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
                Column {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(team) { index, pokemon ->
                            var isPressed by remember { mutableStateOf(false) }

                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1f,
                                animationSpec = tween(durationMillis = 150),
                                label = "cardScale"
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clickable {
                                        isPressed = true
                                        navController.navigate("pokemon_detail/${pokemon.name}")
                                    },
                                elevation = CardDefaults.cardElevation(4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black)
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
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    IconButton(onClick = { onRemove(pokemon) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove from Team",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Team Rating and Feedback Section
                    if (team.size == 6) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Team Strength: $teamStrength/100",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Type Analysis: $dominantType dominant, $weaknessFeedback",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Add ${6 - team.size} more Pokémon to rate your team!",
                            color = Color.White,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}