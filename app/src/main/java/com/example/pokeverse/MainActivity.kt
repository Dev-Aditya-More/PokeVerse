package com.example.pokeverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.pokeverse.di.appModule
import com.example.pokeverse.screens.HomeScreen
import com.example.pokeverse.screens.PokemonDetailScreen
import com.example.pokeverse.ui.theme.PokeVerseTheme
import com.example.pokeverse.utils.ScreenStateManager
import kotlinx.coroutines.delay
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        setContent {
            PokeVerseTheme {

                val navController = rememberNavController()
                val context = LocalContext.current
                val startDestination = remember { mutableStateOf("splash") }

                LaunchedEffect(Unit) {
                    val savedRoute = ScreenStateManager.getLastRoute(context)

                    if (!savedRoute.isNullOrBlank()) {
                        if (savedRoute.startsWith("pokemon_detail/")) {
                            // Clear all backstack and go home first
                            navController.navigate("home") {
                                popUpTo(0) { inclusive = true }
                            }

                            // Then navigate to detail so backstack is home -> detail
                            navController.navigate(savedRoute)
                        } else {
                            startDestination.value = savedRoute
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination.value
                ) {
                    composable("splash") {
                        SplashScreen(navController = navController)
                    }
                    composable("home") {
                        HomeScreen(navController = navController)
                    }
                    composable("pokemon_detail/{pokemonName}") { backStackEntry ->
                        val pokemonName = backStackEntry.arguments?.getString("pokemonName")
                        if (pokemonName != null) {
                            PokemonDetailScreen(
                                pokemonName = pokemonName,
                                navController = navController
                            )
                        } else {
                            Text("Error: Pokémon not found")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {

    LaunchedEffect(Unit) {
        delay(3000)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true } // removes splash from backstack
        }
    }

    // Control animation + transition
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.pokemon_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    val brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE3350D), Color(0xFFFFFFFF)),
    start = Offset(0f, 0f),
    end = Offset(0f, 1000f) // vertical gradient
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(200.dp)
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(navController = NavController(LocalContext.current))
}