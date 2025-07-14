package com.example.pokeverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pokeverse.di.appModule
import com.example.pokeverse.screens.DreamTeam
import com.example.pokeverse.screens.HomeScreen
import com.example.pokeverse.screens.PokemonDetailScreen
import com.example.pokeverse.ui.theme.PokeVerseTheme
import com.example.pokeverse.ui.viewmodel.PokemonViewModel
import com.example.pokeverse.utils.ScreenStateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        setContent {
            PokeVerseTheme {
                navController = rememberNavController()  // Store in field
                val context = LocalContext.current
                val viewModel: PokemonViewModel = koinViewModel()

                val startDestination = remember { mutableStateOf("splash") }

                // Restore saved screen on launch
                LaunchedEffect(Unit) {
                    val savedRoute = ScreenStateManager.getLastRoute(context)
                    if (!savedRoute.isNullOrBlank()) {
                        startDestination.value = savedRoute
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination.value
                ) {
                    composable("splash") {
                        SplashScreen(navController)
                    }
                    composable("home") {
                        HomeScreen(navController)
                    }
                    composable("dream_team") { backStackEntry ->
                        DreamTeam(
                            navController = navController,
                            team = viewModel.team.collectAsState().value,
                            onRemove = { viewModel.removeFromTeam(it) }
                        )
                    }
                    composable("pokemon_detail/{pokemonName}") { backStackEntry ->
                        val pokemonName = backStackEntry.arguments?.getString("pokemonName")
                        if (pokemonName != null) {
                            PokemonDetailScreen(pokemonName, navController)
                        } else {
                            val pokeballGradient = Brush.verticalGradient(
                                listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A))
                            )
                            Box(
                                modifier = Modifier.fillMaxSize().background(brush = pokeballGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Error: Pokémon not found", color = Color.White)

                            }
                        }
                    }
                }
            }
        }
    }
    override fun onStop() {
        super.onStop()

        val route = navController.currentBackStackEntry?.destination?.route
        if (!route.isNullOrBlank()) {
            lifecycleScope.launch {
                ScreenStateManager.saveCurrentRoute(this@MainActivity, route)
            }
        }
    }

}

@Composable
fun SplashScreen(navController: NavController) {

    LaunchedEffect(Unit) {

        delay(2000)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true } // removes splash from backstack
        }
    }


    val pokeballGradient = Brush.verticalGradient(
        listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = pokeballGradient)
//            .scale(scale)
//            .alpha(alpha)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(

            painterResource(id = R.drawable.mysplash2),
            contentDescription = "Logo",
            modifier = Modifier.size(450.dp)
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(navController = NavController(LocalContext.current))
}