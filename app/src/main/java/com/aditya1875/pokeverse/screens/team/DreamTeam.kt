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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity
import com.aditya1875.pokeverse.screens.detail.components.CustomProgressIndicator
import com.aditya1875.pokeverse.screens.team.components.EmptyTeamView
import com.aditya1875.pokeverse.screens.team.components.TeamPickerPokemonCard
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
    val backgroundColor = Color(0xFF1C1C1C)
    val cardBackground = Color(0xFF000000)
    val accentRed = Color(0xFFDC3545) // Red accent from the chips
    val yellowStar = Color(0xFFFFC107) // Yellow star color

    var teamName by rememberSaveable { mutableStateOf("My Team") }
    var isEditingName by rememberSaveable { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = backgroundColor,
        modifier = Modifier.fillMaxSize()
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
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

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2A2A2A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (isEditingName) {
                                    OutlinedTextField(
                                        value = teamName,
                                        onValueChange = { teamName = it },
                                        textStyle = LocalTextStyle.current.copy(
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = accentRed,
                                            unfocusedBorderColor = Color(0xFF3A3A3A),
                                            cursorColor = accentRed
                                        )
                                    )

                                    IconButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isEditingName = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            tint = Color(0xFF4CAF50),
                                            contentDescription = "Confirm"
                                        )
                                    }
                                } else {
                                    Text(
                                        text = teamName,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { isEditingName = true }
                                    )

                                    IconButton(
                                        onClick = { isEditingName = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit team name",
                                            tint = Color(0xFF9E9EB0)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Progress Section - matching card style
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2A2A2A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Team Progress",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        text = "${team.size}/6",
                                        color = accentRed,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = when {
                                        team.size < 6 -> "Add ${6 - team.size} more Pokémon to complete your team"
                                        else -> "Team complete! Ready for battle"
                                    },
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(16.dp))

                                VibrantProgressBar(
                                    progress = team.size / 6f,
                                    modifier = Modifier.fillMaxWidth(),
                                    accentColor = accentRed
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Pokémon Cards List
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(team) { pokemon ->

                                TeamPokemonCard(
                                    pokemon = pokemon,
                                    navController = navController,
                                    onRemove = onRemove
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}