package com.aditya1875.pokeverse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aditya1875.pokeverse.data.billing.IBillingManager
import com.aditya1875.pokeverse.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.domain.xp.XPResult
import com.aditya1875.pokeverse.presentation.components.PokemonNotFoundScreen
import com.aditya1875.pokeverse.presentation.screens.analysis.TeamAnalysisScreen
import com.aditya1875.pokeverse.presentation.screens.detail.PokemonDetailScreen
import com.aditya1875.pokeverse.presentation.screens.game.GameHubScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.components.PokeGuessDifficultyScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.PokeGuessGameScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.components.GuessDifficulty
import com.aditya1875.pokeverse.presentation.screens.game.pokematch.GameScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokematch.components.DifficultyScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizDifficultyScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.QuizGameScreen
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizDifficulty
import com.aditya1875.pokeverse.presentation.screens.game.poketype.TypeRushScreen
import com.aditya1875.pokeverse.presentation.screens.game.poketype.components.TypeRushDifficulty
import com.aditya1875.pokeverse.presentation.screens.game.poketype.components.TypeRushDifficultyScreen
import com.aditya1875.pokeverse.presentation.screens.home.HomeScreen
import com.aditya1875.pokeverse.navigation.components.Route
import com.aditya1875.pokeverse.presentation.screens.leaderboard.LeaderboardScreen
import com.aditya1875.pokeverse.presentation.screens.leaderboard.components.XPOverlay
import com.aditya1875.pokeverse.presentation.screens.onboarding.IntroScreen
import com.aditya1875.pokeverse.presentation.screens.profile.ProfileScreen
import com.aditya1875.pokeverse.presentation.screens.profile.components.EditProfileDialog
import com.aditya1875.pokeverse.presentation.screens.settings.SettingsScreen
import com.aditya1875.pokeverse.presentation.screens.splash.SplashScreen
import com.aditya1875.pokeverse.presentation.screens.team.DreamTeam
import com.aditya1875.pokeverse.presentation.screens.theme.ThemeSelectorScreen
import com.aditya1875.pokeverse.presentation.ui.theme.AppTheme
import com.aditya1875.pokeverse.presentation.ui.theme.PokeverseTheme
import com.aditya1875.pokeverse.presentation.ui.viewmodel.ProfileViewModel
import com.aditya1875.pokeverse.presentation.ui.viewmodel.SettingsViewModel
import com.aditya1875.pokeverse.utils.Difficulty
import com.aditya1875.pokeverse.utils.NotificationUtils
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.navigation.components.WithBottomBar
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val billingManager: IBillingManager = inject<IBillingManager>().value
        billingManager.startConnection()

        enableEdgeToEdge()

        requestNotificationPermission()

        NotificationUtils.createNotificationChannel(this)

        FirebaseMessaging.getInstance().subscribeToTopic("theme_updates")

        setContent {
            val themePreferences = koinInject<ThemePreferences>()
            val selectedTheme by themePreferences.selectedTheme.collectAsState(
                initial = AppTheme.DEXVERSE
            )

            val profileViewModel: ProfileViewModel = koinViewModel()
            val xpResult by profileViewModel.xpEvent.collectAsState(initial = null)
            var shownXpResult by remember { mutableStateOf<XPResult?>(null) }

            LaunchedEffect(xpResult) {
                xpResult?.let { shownXpResult = it }
            }

            LaunchedEffect(Unit) {
                profileViewModel.onAppLaunch()
            }

            var currentTheme by rememberSaveable { mutableStateOf(selectedTheme) }

            LaunchedEffect(selectedTheme) {
                currentTheme = selectedTheme
            }

            val startDestination = remember { mutableStateOf(Route.Splash.route) }
            val context = LocalContext.current

            XPOverlay(
                result = shownXpResult,
                onDismiss = { shownXpResult = null }
            ) {
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
                            Route.BottomBar.Profile.route -> Route.BottomBar.Profile
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

                    val settingsViewModel: SettingsViewModel = koinViewModel()

                    NavHost(
                        navController = navController,
                        startDestination = startDestination.value,
                    ) {
                        composable(Route.Splash.route) {
                            SplashScreen()
                        }

                        composable(Route.Onboarding.route) {
                            IntroScreen(navController)
                        }

                        composable(Route.BottomBar.Home.route) {
                            WithBottomBar(navController) {
                                HomeScreen(navController, settingsViewModel)
                            }
                        }

                        composable(Route.BottomBar.Team.route) {
                            WithBottomBar(
                                navController,
                                selectedRoute = selectedRoute,
                                onRouteChange = { selectedRoute = it }
                            ) {
                                DreamTeam(
                                    navController = navController,
                                    settingsViewModel = settingsViewModel
                                )
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
                                navController = navController,
                                selectedRoute = selectedRoute,
                                onRouteChange = { selectedRoute = it }
                            ) {
                                LeaderboardScreen()
                            }
                        }

                        composable(Route.BottomBar.Profile.route) {
                            WithBottomBar(
                                navController = navController,
                                selectedRoute = selectedRoute,
                                onRouteChange = { selectedRoute = it }
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

                        composable(Route.TypeRushDifficulty.route) {
                            TypeRushDifficultyScreen(
                                onDifficultySelected = { difficulty ->
                                    navController.navigate(Route.TypeRushPlay.createRoute(difficulty.name))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Route.TypeRushPlay.route,
                            arguments = listOf(navArgument("difficulty") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val difficultyName = backStackEntry.arguments?.getString("difficulty") ?: "EASY"
                            val difficulty = TypeRushDifficulty.valueOf(difficultyName)
                            TypeRushScreen(
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
                                PokemonDetailScreen(pokemonName, navController, settingsViewModel)
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
    SplashScreen()
}