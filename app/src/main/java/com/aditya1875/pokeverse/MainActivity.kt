@file:Suppress("DEPRECATION")

package com.aditya1875.pokeverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aditya1875.pokeverse.di.appModule
import com.aditya1875.pokeverse.screens.DreamTeam
import com.aditya1875.pokeverse.screens.HomeScreen
import com.aditya1875.pokeverse.screens.IntroScreen
import com.aditya1875.pokeverse.screens.PokemonDetailScreen
import com.aditya1875.pokeverse.screens.SettingsScreen
import com.aditya1875.pokeverse.ui.theme.PokeVerseTheme
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.WithBottomBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    @OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        setContent {

            val startDestination = remember { mutableStateOf("splash") }
            PokeVerseTheme{
                navController = rememberNavController()  // Store in field
                val viewModel: PokemonViewModel = koinViewModel()
                LaunchedEffect(Unit) {

                    val introSeen = ScreenStateManager.isIntroSeen(this@MainActivity)
                    val lastRoute = ScreenStateManager.getLastRoute(this@MainActivity)

                    startDestination.value = when {
                        !introSeen -> "intro"
                        !lastRoute.isNullOrBlank() -> lastRoute
                        else -> "home"
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination.value
                ) {
                    composable("splash") { SplashScreen(navController) }
                    composable("intro") { IntroScreen(navController) }
                    composable("home") {
                        WithBottomBar(navController) {
                            HomeScreen(navController)
                        }
                    }
                    composable("dream_team") {
                        WithBottomBar(navController) {
                            DreamTeam(
                                navController = navController,
                                team = viewModel.team.collectAsState().value,
                                onRemove = { viewModel.removeFromTeam(it) }
                            )
                        }
                    }
                    composable("pokemon_detail/{pokemonName}") { backStackEntry ->
                        val pokemonName = backStackEntry.arguments?.getString("pokemonName")
                        if (pokemonName != null) {
                            PokemonDetailScreen(pokemonName, navController)
                        } else {
                            PokemonNotFoundScreen(
                                onBackClick = { navController.popBackStack() }, // a buggggggg
                                onRetryClick = { navController.navigate("home") }
                            )
                        }
                    }
                    composable("settings") {
                        WithBottomBar(navController) {
                            SettingsScreen(navController)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonNotFoundScreen(
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    val pokeballGradient = Brush.verticalGradient(
        listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Oops!") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = pokeballGradient)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Placeholder for image/animation
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Not Found",
                    tint = Color.LightGray,
                    modifier = Modifier.size(96.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "We couldn’t find that Pokémon...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Try searching again or check the spelling.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(Modifier.height(24.dp))

                Button(onClick = onRetryClick) {
                    Text("Try Again")
                }
            }
        }
    }
}


@Preview(showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(navController = NavController(LocalContext.current))
}