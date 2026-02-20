package com.aditya1875.pokeverse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aditya1875.pokeverse.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.presentation.components.PokemonNotFoundScreen
import com.aditya1875.pokeverse.presentation.screens.analysis.TeamAnalysisScreen
import com.aditya1875.pokeverse.presentation.screens.detail.PokemonDetailScreen
import com.aditya1875.pokeverse.presentation.screens.game.GameHubScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.PokeGuessDifficultyScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.PokeGuessGameScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.components.GuessDifficulty
import com.aditya1875.pokeverse.presentation.screens.game.pokematch.GameScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokematch.components.DifficultyScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.QuizDifficultyScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.QuizGameScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizDifficulty
import com.aditya1875.pokeverse.presentation.screens.home.HomeScreen
import com.aditya1875.pokeverse.presentation.screens.home.components.Route
import com.aditya1875.pokeverse.presentation.screens.onboarding.IntroScreen
import com.aditya1875.pokeverse.presentation.screens.settings.SettingsScreen
import com.aditya1875.pokeverse.presentation.screens.splash.SplashScreen
import com.aditya1875.pokeverse.presentation.screens.team.DreamTeam
import com.aditya1875.pokeverse.presentation.screens.theme.ThemeSelectorScreen
import com.aditya1875.pokeverse.presentation.ui.theme.AppTheme
import com.aditya1875.pokeverse.presentation.ui.theme.PokeverseTheme
import com.aditya1875.pokeverse.utils.Difficulty
import com.aditya1875.pokeverse.utils.NotificationUtils
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.WithBottomBar
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()

        NotificationUtils.createNotificationChannel(this)

        FirebaseMessaging.getInstance().subscribeToTopic("theme_updates")

        setContent {
            val themePreferences = koinInject<ThemePreferences>()
            val selectedTheme by themePreferences.selectedTheme.collectAsState(
                initial = AppTheme.POKEVERSE
            )

            var currentTheme by rememberSaveable { mutableStateOf(selectedTheme) }

            LaunchedEffect(selectedTheme) {
                currentTheme = selectedTheme
            }

            val startDestination = remember { mutableStateOf("splash") }
            val context = LocalContext.current

            PokeverseTheme(selectedTheme = currentTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                var selectedRoute by remember {
                    mutableStateOf<Route.BottomBar>(Route.BottomBar.Home)
                }

                LaunchedEffect(currentRoute) {
                    selectedRoute = when (currentRoute) {
                        Route.BottomBar.Home.route -> Route.BottomBar.Home
                        Route.BottomBar.Team.route -> Route.BottomBar.Team
                        Route.BottomBar.Game.route -> Route.BottomBar.Game
                        Route.BottomBar.Settings.route -> Route.BottomBar.Settings
                        else -> selectedRoute
                    }
                }

                LaunchedEffect(Unit) {
                    val introSeen = ScreenStateManager.isIntroSeen(this@MainActivity)
                    val lastRoute = ScreenStateManager.getLastRoute(this@MainActivity)

                    startDestination.value = when {
                        !introSeen -> Route.Onboarding.route
                        !lastRoute.isNullOrBlank() -> lastRoute
                        else -> Route.BottomBar.Home.route
                    }
                }

                val scope = rememberCoroutineScope()

                NavHost(
                    navController = navController,
                    startDestination = startDestination.value,
                ) {
                    // ── CORE SCREENS ──────────────────────────────────────
                    composable(Route.Splash.route) {
                        SplashScreen(navController)
                    }

                    composable(Route.Onboarding.route) {
                        IntroScreen(navController)
                    }

                    composable(Route.BottomBar.Home.route) {
                        WithBottomBar(navController) {
                            HomeScreen(navController)
                        }
                    }

                    composable(Route.BottomBar.Team.route) {
                        WithBottomBar(
                            navController,
                            selectedRoute = selectedRoute,
                            onRouteChange = { selectedRoute = it }
                        ) {
                            DreamTeam(navController = navController)
                        }
                    }

                    composable(Route.BottomBar.Game.route) {
                        WithBottomBar(
                            navController = navController,
                            selectedRoute = selectedRoute,
                            onRouteChange = { selectedRoute = it }
                        ) {
                            GameHubScreen(
                                onGameSelected = { gameId ->
                                    when (gameId) {
                                        "pokematch" -> navController.navigate(Route.GameDifficulty.route)
                                        "pokequiz" -> navController.navigate(Route.QuizDifficulty.route)
                                        "pokeguess" -> navController.navigate(Route.GuessDifficulty.route)
                                    }
                                }
                            )
                        }
                    }

                    composable(Route.BottomBar.Settings.route) {
                        WithBottomBar(
                            navController,
                            selectedRoute = selectedRoute,
                            onRouteChange = { selectedRoute = it }
                        ) {
                            SettingsScreen(navController)
                        }
                    }

                    // ── POKÉMATCH NAVIGATION ──────────────────────────────
                    composable(Route.GameDifficulty.route) {
                        DifficultyScreen(
                            onDifficultySelected = { difficulty ->
                                navController.navigate(Route.GamePlay.createRoute(difficulty.name))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Route.GamePlay.route,
                        arguments = listOf(
                            navArgument("difficulty") {
                                type = NavType.StringType
                                defaultValue = "EASY"
                            }
                        )
                    ) { backStackEntry ->
                        val difficultyName = backStackEntry.arguments?.getString("difficulty") ?: "EASY"
                        val difficulty = try {
                            Difficulty.valueOf(difficultyName)
                        } catch (e: IllegalArgumentException) {
                            Difficulty.EASY
                        }

                        GameScreen(
                            difficulty = difficulty,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // ── POKÉQUIZ NAVIGATION ───────────────────────────────
                    composable(Route.QuizDifficulty.route) {
                        QuizDifficultyScreen(
                            onDifficultySelected = { difficulty ->
                                navController.navigate(Route.QuizPlay.createRoute(difficulty.name))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Route.QuizPlay.route,
                        arguments = listOf(
                            navArgument("difficulty") {
                                type = NavType.StringType
                                defaultValue = "EASY"
                            }
                        )
                    ) { backStackEntry ->
                        val difficultyName = backStackEntry.arguments?.getString("difficulty") ?: "EASY"
                        val difficulty = try {
                            QuizDifficulty.valueOf(difficultyName)
                        } catch (e: IllegalArgumentException) {
                            QuizDifficulty.EASY
                        }

                        QuizGameScreen(
                            difficulty = difficulty,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // PokeGuess Integration
                    composable(Route.GuessDifficulty.route) {
                        PokeGuessDifficultyScreen(
                            onDifficultySelected = { difficulty ->
                                navController.navigate(Route.GuessPlay.createRoute(difficulty.name))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Route.GuessPlay.route,
                        arguments = listOf(
                            navArgument("difficulty") {
                                type = NavType.StringType
                                defaultValue = "EASY"
                            }
                        )
                    ) { backStackEntry ->
                        val difficultyName = backStackEntry.arguments?.getString("difficulty") ?: "EASY"
                        val difficulty = try {
                            GuessDifficulty.valueOf(difficultyName)
                        } catch (e: IllegalArgumentException) {
                            GuessDifficulty.EASY
                        }

                        PokeGuessGameScreen(
                            difficulty = difficulty,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // ── OTHER SCREENS ─────────────────────────────────────
                    composable(Route.Analysis.route) {
                        TeamAnalysisScreen(navController = navController)
                    }

                    composable(Route.Details.route) { backStackEntry ->
                        val pokemonName = backStackEntry.arguments?.getString("pokemonName")
                        if (pokemonName != null) {
                            PokemonDetailScreen(pokemonName, navController)
                        } else {
                            PokemonNotFoundScreen(
                                onRetryClick = {
                                    navController.navigate(Route.BottomBar.Home.route)
                                }
                            )
                        }
                    }

                    composable(Route.ThemeSelector.route) {
                        ThemeSelectorScreen(
                            navController = navController,
                            onThemeSelected = { theme ->
                                scope.launch {
                                    themePreferences.setTheme(theme)
                                }
                            }
                        )
                    }
                }

                // Save current route
                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { entry ->
                        entry.destination.route?.let { route ->
                            ScreenStateManager.saveCurrentRoute(context, route)
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(navController = NavController(LocalContext.current))
}