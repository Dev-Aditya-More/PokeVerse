package com.example.pokeverse.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.pokeverse.data.local.entity.TeamMemberEntity
import com.example.pokeverse.data.remote.model.PokemonResponse
import com.example.pokeverse.ui.viewmodel.PokemonViewModel
import com.example.pokeverse.utils.TeamMapper.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val viewModel: PokemonViewModel = koinViewModel()
    val pokemonList by viewModel.pokemonList.collectAsState()
    val isLoading = viewModel.isLoading
    val endReached = viewModel.endReached
    var query by rememberSaveable { mutableStateOf("") } // Persistent search query
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Used for animated visibility
    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }

    val pokeballGradient = Brush.verticalGradient(
        listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A))
    )

    LaunchedEffect(Unit) {
        viewModel.loadPokemonList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = pokeballGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "PokeVerse",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 4.dp) // Adjusted padding for alignment
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White
                    )
                    // Uncomment if you want a custom height: modifier = Modifier.height(45.dp)
                )
            },
            floatingActionButton = {
                var expanded by remember { mutableStateOf(false) }
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 45f else 0f,
                    animationSpec = tween(durationMillis = 250),
                    label = "FAB rotation"
                )

                Box {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box {
                            FloatingActionButton(
                                onClick = { expanded = !expanded },
                                containerColor = Color(0xFF802525),
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = rotation
                                }
                            ) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Features")
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .background(Color(0xFF1C1C1C))
                                    .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                            ) {

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Team Rating",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    onClick = {
                                        navController.navigate("dream_team")
                                        expanded = false
                                    },
                                    modifier = Modifier
                                        .background(Color(0xFF2E2E2E))
                                        .padding(vertical = 4.dp)
                                        .width(220.dp)
                                        .height(60.dp)
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Win Predictor",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    onClick = {
                                        // Add logic here
                                        expanded = false
                                    },
                                    modifier = Modifier
                                        .background(Color(0xFF2E2E2E))
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            val focusManager = LocalFocusManager.current

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { // Removed inner background to let Scaffold handle it
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                        .background(pokeballGradient) // Moved background here for full coverage
                ) {
                    val filterState by viewModel.filters.collectAsState()

                    FilterBar(
                        currentFilter = filterState,
                        onRegionChange = { viewModel.setRegionFilter(it) },
                    )

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search a Pokémon", color = Color.White) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (query.isNotBlank()) {
                                        coroutineScope.launch {
                                            val success = viewModel.fetchPokemonData(query.lowercase()).toString()
                                            if (success.isNotEmpty()) {
                                                navController.navigate("pokemon_detail/${query.lowercase()}")
                                            } else {
                                                Toast.makeText(context, "Pokémon not found", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (query.isNotBlank()) {
                                    viewModel.fetchPokemonData(query.lowercase())
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.DarkGray,
                            unfocusedTrailingIconColor = Color.DarkGray,
                            unfocusedLabelColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            focusedTrailingIconColor = Color.White,
                            focusedBorderColor = Color(0xFF802525)
                        )
                    )

                    if (pokemonList.isEmpty() && isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CustomProgressIndicator()
                        }
                    } else {

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(pokemonList) { index, pokemon ->
                                    if (index >= pokemonList.size - 5 && !isLoading && !endReached) {
                                        viewModel.loadPokemonList()
                                        Text(text = "Total: ${pokemonList.size} Pokémon shown")
                                    }
                                    var isPressed by remember { mutableStateOf(false) }

                                    val scale by animateFloatAsState(
                                        targetValue = if (isPressed) 0.97f else 1f,
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
                                                coroutineScope.launch {
                                                    delay(150)
                                                    isPressed = false
                                                    withContext(Dispatchers.Main) {
                                                        navController.navigate("pokemon_detail/${pokemon.name}")
                                                    }
                                                }
                                            },
                                        elevation = CardDefaults.cardElevation(4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(end = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val isInTeam by viewModel.isInTeam(pokemon.name)
                                                .collectAsState(initial = false)
                                            val team = viewModel.team.collectAsState()

                                            Text(
                                                text = pokemon.name.replaceFirstChar { it.uppercase() },
                                                modifier = Modifier.padding(16.dp),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White
                                            )

                                            IconButton(
                                                onClick = {
                                                    val team =
                                                        viewModel.team.value // Safe here outside composition
                                                    if (isInTeam) {
                                                        viewModel.removeFromTeam(pokemon.toEntity())
                                                    } else {
                                                        if (team.size >= 6) {
                                                            Toast.makeText(
                                                                context,
                                                                "Team already has 6 Pokémon!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else {
                                                            viewModel.addToTeam(pokemon)
                                                        }
                                                    }
                                                },
                                                enabled = isInTeam || team.value.size < 6
                                            ) {
                                                Icon(
                                                    imageVector = if (isInTeam) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = if (isInTeam) "Remove from Team" else "Add to Team",
                                                    tint = if (isInTeam) Color.Yellow else Color.White.copy(
                                                        alpha = if (team.value.size >= 6) 0.2f else 1f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isLoading && pokemonList.isNotEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CustomProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}
