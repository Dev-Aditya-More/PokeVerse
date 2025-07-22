@file:Suppress("DEPRECATION")

package com.example.pokeverse

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.pokeverse.screens.IntroScreen
import com.example.pokeverse.screens.PokemonDetailScreen
import com.example.pokeverse.ui.theme.PokeVerseTheme
import com.example.pokeverse.ui.viewmodel.PokemonViewModel
import com.example.pokeverse.utils.ScreenStateManager
import com.example.pokeverse.utils.ScreenStateManager.markIntroSeen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    @OptIn(ExperimentalAnimationApi::class)
    @RequiresApi(Build.VERSION_CODES.N)
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
                val viewModel: PokemonViewModel = koinViewModel()

                val startDestination = remember { mutableStateOf("splash") }

                LaunchedEffect(Unit) {
                    val introSeen = ScreenStateManager.isIntroSeen(this@MainActivity)
                    val lastRoute = ScreenStateManager.getLastRoute(this@MainActivity)

                    startDestination.value = when {
                        !introSeen -> "intro"
                        !lastRoute.isNullOrBlank() -> lastRoute
                        else -> "home"
                    }
                }

                AnimatedNavHost(
                    navController = navController,
                    startDestination = startDestination.value,
                    enterTransition = { fadeIn(animationSpec = tween(500)) + scaleIn(tween(300))},
                    exitTransition = { fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(500)) + scaleIn(tween(300)) },
                    popExitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    composable("splash") {
                        SplashScreen(navController)
                    }
                    composable("intro") {
                        IntroScreen(navController = navController)
                    }
                    composable(
                        "home",
                        exitTransition = {
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { -it / 2 }
                            )
                        }
                    ) {
                        HomeScreen(navController)
                    }
                    composable(
                        "dream_team",
                        enterTransition = {
                            slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { it / 2 }
                        )
                        }
                    ) { backStackEntry ->
                        DreamTeam(
                            navController = navController,
                            team = viewModel.team.collectAsState().value,
                            onRemove = { viewModel.removeFromTeam(it) }
                        )
                    }
                    composable(
                        "pokemon_detail/{pokemonName}"
                    ){ backStackEntry ->
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
                markIntroSeen(this@MainActivity)
            }
        }
    }

}

@Composable
fun SplashScreen(navController: NavController) {

    LaunchedEffect(Unit) {
        delay(3000)
        navController.navigate("intro") {
            popUpTo("splash") { inclusive = true }
        }
    }


    val pokeballGradient = Brush.verticalGradient(
        listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = pokeballGradient)
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