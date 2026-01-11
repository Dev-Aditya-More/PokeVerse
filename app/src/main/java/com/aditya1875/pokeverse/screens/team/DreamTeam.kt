package com.aditya1875.pokeverse.screens.team

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.components.CustomProgressIndicator
import com.aditya1875.pokeverse.components.VibrantProgressBar
import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity
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
                            Text(
                                text = "Team Progress: ${team.size}/6",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 15.sp
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

@Composable
private fun EmptyTeamView(
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E1E2C), Color(0xFF2C5364))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = 600f
                    )
                )
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val floatAnim by rememberInfiniteTransition().animateFloat(
                initialValue = -8f,
                targetValue = 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Image(
                painter = painterResource(id = R.drawable.teampoke),
                contentDescription = null,
                modifier = Modifier
                    .size(370.dp)
                    .graphicsLayer {
                        translationY = floatAnim
                        alpha = 0.95f
                    }
            )

            Spacer(modifier = Modifier.height(25.dp))

            Text(
                text = "Your Team is Empty",
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                letterSpacing = 0.5.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Catch some Pokémon and build your squad!",
                color = Color(0xFFB0BEC5),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    navController.navigate("home") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // 2. Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // 3. Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Build Your Team",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TeamPokemonCard(
    pokemon: TeamMemberEntity,
    navController: NavController,
    onRemove: (TeamMemberEntity) -> Unit,
    viewModel: PokemonViewModel
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "cardScale"
    )

    val team by viewModel.team.collectAsStateWithLifecycle()
    val teamMembershipMap = remember(team) { team.associate { it.name to true } }
    val isInTeam = teamMembershipMap[pokemon.name] ?: false

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
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(pokemon.imageUrl)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = pokemon.name,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.15f))
            )

            Text(
                text = pokemon.name.replaceFirstChar { it.uppercase() },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(onClick = { if (isInTeam) onRemove(pokemon) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from Team",
                    tint = Color(0xFFFF4444)
                )
            }
        }
    }
}
