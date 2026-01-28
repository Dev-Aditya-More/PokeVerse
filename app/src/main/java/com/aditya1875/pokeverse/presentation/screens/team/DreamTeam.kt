package com.aditya1875.pokeverse.presentation.screens.team

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity
import com.aditya1875.pokeverse.presentation.screens.team.components.EmptyTeamView
import com.aditya1875.pokeverse.presentation.screens.team.components.FavoritesContent
import com.aditya1875.pokeverse.presentation.screens.team.components.TabButton
import com.aditya1875.pokeverse.presentation.screens.team.components.TeamContent
import com.aditya1875.pokeverse.presentation.ui.viewmodel.PokemonViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamTeam(
    navController: NavController,
    team: List<TeamMemberEntity>,
    onRemove: (TeamMemberEntity) -> Unit,
    selectedName: String,
    onNameChange: (String) -> Unit,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    viewModel: PokemonViewModel = koinViewModel()
) {
    var isEditingName by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                viewModel.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                team.isEmpty() && favorites.isEmpty() -> {
                    EmptyTeamView(navController)
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {

                        Spacer(Modifier.height(32.dp))

                        // Team Name Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
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
                                        value = selectedName,
                                        onValueChange = onNameChange,
                                        textStyle = LocalTextStyle.current.copy(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                                            cursorColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )

                                    IconButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isEditingName = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            contentDescription = "Confirm"
                                        )
                                    }
                                } else {
                                    Text(
                                        text = selectedName,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { isEditingName = true }
                                    )

                                    IconButton(onClick = { isEditingName = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit team name",
                                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Tab Selector
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            ) {
                                // Team Tab
                                TabButton(
                                    text = "Team (${team.size})",
                                    icon = Icons.Default.Add,
                                    isSelected = selectedTab == 0,
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = { onTabChange(0) },
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(Modifier.width(8.dp))

                                // Favorites Tab
                                TabButton(
                                    text = "Favorites (${favorites.size})",
                                    icon = Icons.Default.Star,
                                    isSelected = selectedTab == 1,
                                    color = MaterialTheme.colorScheme.secondary,
                                    onClick = { onTabChange(1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Content based on selected tab
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith
                                        fadeOut(animationSpec = tween(300))
                            },
                            label = "tab_content"
                        ) { tab ->
                            when (tab) {
                                0 -> TeamContent(
                                    team = team,
                                    navController = navController,
                                    onRemove = onRemove,
                                    accentColor = MaterialTheme.colorScheme.primary
                                )
                                1 -> FavoritesContent(
                                    favorites = favorites,
                                    navController = navController,
                                    onRemove = { viewModel.removeFromFavorites(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}