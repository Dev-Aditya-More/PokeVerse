package com.example.pokeverse.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.pokeverse.ui.viewmodel.PokemonViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController : NavHostController) {
    val viewModel: PokemonViewModel = koinViewModel()
    val pokemonListState by viewModel.pokemonList.collectAsState()
//    val brush = Brush.linearGradient(
//        colors = listOf(Color(0xFFE3350D), Color(0xFFFFFFFF)),
//        start = Offset(0f, 0f),
//        end = Offset(0f, 1000f) // vertical gradient
//    )

//    val pokeballGradient = Brush.verticalGradient(
//        colorStops = arrayOf(
//            0.0f to Color(0xFFEF5350),      // Pokéball red
//            0.45f to Color(0xFFEF5350),     // Continue red
//            0.46f to Color(0xFF212121),     // Black band starts
//            0.54f to Color(0xFF212121),     // Black band ends
//            0.55f to Color(0xFFEEEEEE),     // Light gray (bottom)
//            1.0f to Color(0xFFF5F5F5)        // Slight white fade
//        )
//    )

    val pokeballGradient = Brush.verticalGradient(
        listOf(Color(0xFFEDE574), Color(0xFFE1F5C4))
    )

    LaunchedEffect(Unit) {
        viewModel.loadPokemonList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(brush = pokeballGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("PokeVerse") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    pokemonListState == null -> {

                        CustomProgressIndicator()
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pokemonListState?.results ?: emptyList()) { pokemon ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("pokemon_detail/${pokemon.name}")
                                        },
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Black
                                    )
                                ) {
                                    Text(
                                        text = pokemon.name.replaceFirstChar { it.uppercase() },
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}


@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {

    HomeScreen(navController = NavHostController(LocalContext.current))
}