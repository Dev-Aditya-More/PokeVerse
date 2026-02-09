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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.presentation.screens.team.components.CreateTeamDialog
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
    viewModel: PokemonViewModel = koinViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val allTeams by viewModel.allTeams.collectAsStateWithLifecycle()
    val currentTeam by viewModel.currentTeam.collectAsStateWithLifecycle()
    val currentTeamMembers by viewModel.currentTeamMembers.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var showTeamOptionsMenu by remember { mutableStateOf(false) }
    var teamCreationError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isEditingTeamName by remember { mutableStateOf(false) }
    var editedTeamName by remember { mutableStateOf("") }

    LaunchedEffect(currentTeam) {
        editedTeamName = currentTeam?.teamName ?: ""
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showCreateTeamDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new team",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
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

                allTeams.isEmpty() -> {
                    EmptyTeamView(navController)
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {
                        Spacer(Modifier.height(32.dp))

                        // Team Selector Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Team name with dropdown and options
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (allTeams.size > 1 && !isEditingTeamName) {
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { showTeamOptionsMenu = true }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = currentTeam?.teamName ?: "My Team",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Select team",
                                                    tint = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }

                                            // Dropdown menu for team selection
                                        DropdownMenu(
                                            expanded = showTeamOptionsMenu,
                                            onDismissRequest = { showTeamOptionsMenu = false }
                                        ) {
                                            allTeams.forEach { team ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column {
                                                                Text(
                                                                    text = team.teamName,
                                                                    fontWeight = if (team.teamId == currentTeam?.teamId)
                                                                        FontWeight.Bold else FontWeight.Normal
                                                                )
                                                                if (team.isDefault) {
                                                                    Text(
                                                                        text = "Default",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                }
                                                            }
                                                            if (team.teamId == currentTeam?.teamId) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Selected",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        viewModel.selectTeam(team.teamId)
                                                        showTeamOptionsMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    } else if (isEditingTeamName) {
                                        // Team name editing
                                        OutlinedTextField(
                                            value = editedTeamName,
                                            onValueChange = { editedTeamName = it },
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
                                                cursorColor = MaterialTheme.colorScheme.onPrimary,
                                                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = currentTeam?.teamName ?: "My Team",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Edit/Confirm button
                                    if (isEditingTeamName) {
                                        IconButton(onClick = {
                                            currentTeam?.let { team ->
                                                viewModel.updateTeamName(
                                                    teamId = team.teamId,
                                                    newName = editedTeamName,
                                                    onSuccess = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        isEditingTeamName = false
                                                    },
                                                    onError = { error ->
                                                        // Show error toast
                                                        teamCreationError = error
                                                    }
                                                )
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                contentDescription = "Confirm"
                                            )
                                        }
                                    } else {
                                        IconButton(onClick = {
                                            isEditingTeamName = true
                                            editedTeamName = currentTeam?.teamName ?: ""
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit team name",
                                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    if (currentTeam?.isDefault == false) {
                                        IconButton(onClick = { showDeleteConfirmation = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete team",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
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
                                TabButton(
                                    text = "Team (${currentTeamMembers.size})",
                                    icon = Icons.Default.Add,
                                    isSelected = selectedTab == 0,
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = { selectedTab = 0 },
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(Modifier.width(8.dp))

                                TabButton(
                                    text = "Favorites (${favorites.size})",
                                    icon = Icons.Default.Star,
                                    isSelected = selectedTab == 1,
                                    color = MaterialTheme.colorScheme.secondary,
                                    onClick = { selectedTab = 1 },
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
                                    team = currentTeamMembers,
                                    navController = navController,
                                    onRemove = { viewModel.removeFromTeam(it) },
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

        // Create Team Dialog
        if (showCreateTeamDialog) {
            CreateTeamDialog(
                onCreateTeam = { teamName ->
                    viewModel.createTeam(
                        teamName = teamName,
                        onSuccess = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showCreateTeamDialog = false
                            teamCreationError = null
                        },
                        onError = { error ->
                            teamCreationError = error
                        }
                    )
                },
                onDismiss = {
                    showCreateTeamDialog = false
                    teamCreationError = null
                },
                errorMessage = teamCreationError
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = {
                    Text(
                        text = "Delete Team?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("Are you sure you want to delete \"${currentTeam?.teamName}\"? All Pokémon in this team will be removed.")
                },
                confirmButton = {
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            currentTeam?.let { team ->
                                viewModel.deleteTeam(
                                    teamId = team.teamId,
                                    onSuccess = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showDeleteConfirmation = false
                                    },
                                    onError = {
                                        teamCreationError = it
                                        showDeleteConfirmation = false
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}