package com.aditya1875.pokeverse.screens.team

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity
import com.aditya1875.pokeverse.screens.detail.components.CustomProgressIndicator
import com.aditya1875.pokeverse.screens.team.components.EmptyTeamView
import com.aditya1875.pokeverse.screens.team.components.TeamPokemonCard
import com.aditya1875.pokeverse.screens.team.components.VibrantProgressBar
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
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
        listOf(Color(0xFF0D0D0D), Color(0xFF212226), Color(0xFF121212))
    )

    var teamName by rememberSaveable { mutableStateOf("My Team") }
    var isEditingName by rememberSaveable { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(pokeballGradient)
        ) {
            when {
                viewModel.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CustomProgressIndicator()
                    }
                }

                team.isEmpty() -> EmptyTeamView(navController)

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {

                        Spacer(Modifier.height(32.dp))

                        // Team Name
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isEditingName) {
                                OutlinedTextField(
                                    value = teamName,
                                    onValueChange = { teamName = it },
                                    textStyle = LocalTextStyle.current.copy(
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFEF5350),
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )

                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isEditingName = false
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        tint = Color(0xFF4CAF50),
                                        contentDescription = null
                                    )
                                }
                            } else {
                                Text(
                                    text = teamName,
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { isEditingName = true }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Progress Section
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Team Status", fontWeight = FontWeight.Bold)

                            Text(
                                when {
                                    team.size < 6 -> "Add ${6 - team.size} more Pokémon"
                                    else -> "Team complete! Analyze strengths"
                                }
                            )

                            Spacer(Modifier.height(6.dp))

                            VibrantProgressBar(
                                progress = team.size / 6f,
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Pokémon Cards List
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(team) { pokemon ->
                                TeamPokemonCard(
                                    pokemon = pokemon,
                                    navController = navController,
                                    onRemove = onRemove,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}