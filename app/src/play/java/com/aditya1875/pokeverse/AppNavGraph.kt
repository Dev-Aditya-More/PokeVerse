package com.aditya1875.pokeverse

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.glance.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.TeamAnalysisScreen
import com.aditya1875.pokeverse.feature.core.navigation.components.Route
import com.aditya1875.pokeverse.feature.core.navigation.components.WithBottomBar
import com.aditya1875.pokeverse.feature.core.ui.components.PokemonNotFoundScreen
import com.aditya1875.pokeverse.feature.game.core.presentation.GameHubScreen
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.GuessDifficulty
import com.aditya1875.pokeverse.feature.game.pokeguess.presentation.components.PokeGuessDifficultyScreen
import com.aditya1875.pokeverse.feature.game.pokeguess.presentation.screens.PokeGuessGameScreen
import com.aditya1875.pokeverse.feature.game.pokematch.domain.model.Difficulty
import com.aditya1875.pokeverse.feature.game.pokematch.presentation.components.DifficultyScreen
import com.aditya1875.pokeverse.feature.game.pokematch.presentation.screens.GameScreen
import com.aditya1875.pokeverse.feature.game.pokequiz.domain.model.QuizDifficulty
import com.aditya1875.pokeverse.feature.game.pokequiz.presentation.components.QuizDifficultyScreen
import com.aditya1875.pokeverse.feature.game.pokequiz.presentation.screens.QuizGameScreen
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushDifficulty
import com.aditya1875.pokeverse.feature.game.poketype.presentation.components.TypeRushDifficultyScreen
import com.aditya1875.pokeverse.feature.game.poketype.presentation.screens.TypeRushScreen
import com.aditya1875.pokeverse.feature.item.presentation.screens.ItemDetailScreen
import com.aditya1875.pokeverse.feature.leaderboard.presentation.screens.LeaderboardScreen
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.screens.PokemonDetailScreen
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.screens.HomeScreen
import com.aditya1875.pokeverse.feature.pokemon.onboarding.IntroScreen
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.EditProfileDialog
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.screens.ProfileScreen
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.screens.SettingsScreen
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels.SettingsViewModel
import com.aditya1875.pokeverse.feature.pokemon.splash.SplashScreen
import com.aditya1875.pokeverse.feature.pokemon.theme_selector.ThemeSelectorScreen
import com.aditya1875.pokeverse.feature.pokemon.theme_selector.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.feature.team.presentation.screens.DreamTeam
import com.aditya1875.pokeverse.utils.ScreenStateManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    context: Context,
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    var selectedRoute by remember {
        mutableStateOf<Route.BottomBar>(Route.BottomBar.Home)
    }

    val themePreferences = koinInject<ThemePreferences>()

    LaunchedEffect(currentRoute) {
        selectedRoute = when (currentRoute) {
            Route.BottomBar.Home.route -> Route.BottomBar.Home
            Route.BottomBar.Team.route -> Route.BottomBar.Team
            Route.BottomBar.Game.route -> Route.BottomBar.Game
            Route.BottomBar.Profile.route -> Route.BottomBar.Profile
            else -> selectedRoute
        }
    }

    LaunchedEffect(Unit) {
        val introSeen = ScreenStateManager.isIntroSeen(context)
        val lastRoute = ScreenStateManager.getLastRoute(context)

        val validRoutes = setOf(
            Route.BottomBar.Home.route,
            Route.BottomBar.Team.route,
            Route.BottomBar.Game.route,
            Route.BottomBar.Profile.route
        )

        val safeRoute = if (lastRoute in validRoutes) lastRoute else Route.BottomBar.Home.route
        val targetRoute = if (!introSeen) Route.Onboarding.route else safeRoute

        val navigated = withTimeoutOrNull(3000) {
            try {
                navController.navigate(targetRoute ?: Route.BottomBar.Home.route) {
                    popUpTo(Route.Splash.route) { inclusive = true }
                    launchSingleTop = true
                }
                true
            } catch (e: Exception) {
                navController.navigate(Route.BottomBar.Home.route) {
                    popUpTo(Route.Splash.route) { inclusive = true }
                }
                true
            }
        }
        if (navigated == null) {
            // Fallback: navigation took too long
            navController.navigate(Route.BottomBar.Home.route) {
                popUpTo(Route.Splash.route) { inclusive = true }
            }
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    SharedTransitionLayout {

        NavHost(
            navController = navController,
            startDestination = Route.Splash.route,
        ) {
            composable(Route.Splash.route) {
                SplashScreen()
            }

            composable(Route.Onboarding.route) {
                IntroScreen(navController)
            }

            composable(Route.BottomBar.Home.route) {
                val animatedVisibilityScope = this

                WithBottomBar(navController) {
                    HomeScreen(
                        navController = navController,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }

            composable(
                route = Route.ItemDetail.route,
                arguments = listOf(
                    navArgument("itemName") { type = NavType.StringType }
                )
            ) { backStackEntry ->

                val animatedVisibilityScope = this

                val itemName = backStackEntry.arguments?.getString("itemName")

                if (itemName != null) {
                    ItemDetailScreen(
                        itemName = itemName,
                        onBack = { navController.popBackStack() },
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }

            composable(Route.BottomBar.Team.route) {
                WithBottomBar(
                    navController
                ) {
                    DreamTeam(
                        navController = navController,
                        settingsViewModel = settingsViewModel
                    )
                }
            }

            composable(Route.BottomBar.Game.route) {
                WithBottomBar(
                    navController = navController
                ) {
                    GameHubScreen(
                        onGameSelected = { gameId ->
                            when (gameId) {
                                "poketype" -> navController.navigate(Route.TypeRushDifficulty.route)
                                "pokematch" -> navController.navigate(Route.GameDifficulty.route)
                                "pokequiz" -> navController.navigate(Route.QuizDifficulty.route)
                                "pokeguess" -> navController.navigate(Route.GuessDifficulty.route)
                            }
                        }
                    )
                }
            }

            composable(Route.BottomBar.Leaderboard.route) {
                WithBottomBar(
                    navController = navController
                ) {
                    LeaderboardScreen()
                }
            }

            composable(Route.BottomBar.Profile.route) {
                WithBottomBar(
                    navController = navController
                ) {
                    ProfileScreen(
                        onSettingsClick = {
                            navController.navigate(Route.Settings.route)
                        },
                        onEditName = {
                            navController.navigate(Route.EditProfile.route)
                        }
                    )
                }
            }

            dialog(Route.EditProfile.route) {
                EditProfileDialog(
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Route.Settings.route) {
                SettingsScreen(navController, settingsViewModel)
            }

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
                val difficultyName =
                    backStackEntry.arguments?.getString("difficulty") ?: "EASY"
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
                val difficultyName =
                    backStackEntry.arguments?.getString("difficulty") ?: "EASY"
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

            composable(Route.GuessDifficulty.route) {
                PokeGuessDifficultyScreen(
                    onDifficultySelected = { difficulty ->
                        navController.navigate(
                            Route.GuessPlay.createRoute(
                                difficulty.name
                            )
                        )
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
                val difficultyName =
                    backStackEntry.arguments?.getString("difficulty") ?: "EASY"
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

            // TypeRush Integration
            composable(Route.TypeRushDifficulty.route) {
                TypeRushDifficultyScreen(
                    onDifficultySelected = { difficulty ->
                        navController.navigate(
                            Route.TypeRushPlay.createRoute(
                                difficulty.name
                            )
                        )
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Route.TypeRushPlay.route,
                arguments = listOf(
                    navArgument("difficulty") {
                        type = NavType.StringType
                        defaultValue = "EASY"
                    }
                )
            ) { backStackEntry ->
                val difficultyName =
                    backStackEntry.arguments?.getString("difficulty") ?: "EASY"
                val difficulty = try {
                    TypeRushDifficulty.valueOf(difficultyName)
                } catch (e: IllegalArgumentException) {
                    TypeRushDifficulty.EASY
                }

                TypeRushScreen(
                    difficulty = difficulty,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Route.Analysis.route,
                arguments = listOf(
                    navArgument("teamId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->

                val teamId = backStackEntry.arguments?.getString("teamId")

                TeamAnalysisScreen(
                    navController = navController,
                    teamId = teamId
                )
            }

            composable(
                route = Route.Details.route,
                arguments = listOf(
                    navArgument("pokemonName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val pokemonName = backStackEntry.arguments?.getString("pokemonName")
                if (pokemonName != null) {
                    PokemonDetailScreen(pokemonName, navController, settingsViewModel)
                } else {
                    PokemonNotFoundScreen(
                        onRetryClick = { navController.navigate(Route.BottomBar.Home.route) }
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

        val validRoutes = setOf(
            Route.BottomBar.Home.route,
            Route.BottomBar.Team.route,
            Route.BottomBar.Game.route,
            Route.BottomBar.Leaderboard.route,
            Route.BottomBar.Profile.route
        )

        LaunchedEffect(navController) {
            navController.currentBackStackEntryFlow.collect { entry ->
                entry.destination.route?.let { route ->
                    if (route in validRoutes) {
                        ScreenStateManager.saveCurrentRoute(context, route)
                    }
                }
            }
        }
    }
}