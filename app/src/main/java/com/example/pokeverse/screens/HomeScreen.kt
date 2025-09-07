package com.example.pokeverse.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.pokeverse.ui.viewmodel.PokemonViewModel
import com.example.pokeverse.utils.TeamMapper.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@RequiresApi(Build.VERSION_CODES.N)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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

    val pokeballGradient = Brush.verticalGradient(
        listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A))
    )

    LaunchedEffect(Unit) {
        viewModel.loadPokemonList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pokeballGradient)

    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Pokéverse",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 26.sp, letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )

                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination?.route

                BottomAppBar(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ) {
                    NavigationBarItem(
                        selected = currentDestination == "home",
                        onClick = { navController.navigate("home") },
                        icon = {
                            Icon(Icons.Default.Home, contentDescription = "Home")
                        },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF802525),
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF1A1A1A)
                        )
                    )

                    NavigationBarItem(
                        selected = currentDestination == "dream_team",
                        onClick = { navController.navigate("dream_team") },
                        icon = {
                            Icon(Icons.Default.Star, contentDescription = "Team")
                        },
                        label = { Text("Team") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF802525),
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF1A1A1A)
                        )
                    )

                    NavigationBarItem(
                        selected = currentDestination == "settings",
                        onClick = { navController.navigate("settings") },
                        icon = {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF802525),
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF1A1A1A)
                        )
                    )
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
                            .padding(8.dp)
                            .padding(bottom = 4.dp),
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
                                        animationSpec = tween(durationMillis = 100),
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
                                                    ),
                                                    modifier = Modifier.graphicsLayer(
                                                        shadowElevation = 8f,
                                                        shape = CircleShape,
                                                        clip = true
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
