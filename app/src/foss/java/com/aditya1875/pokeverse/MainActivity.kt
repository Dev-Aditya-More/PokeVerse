package com.aditya1875.pokeverse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aditya1875.pokeverse.presentation.components.PokemonNotFoundScreen
import com.aditya1875.pokeverse.di.appModule
import com.aditya1875.pokeverse.WorkManager.NotificationScheduler
import com.aditya1875.pokeverse.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.presentation.screens.analysis.TeamAnalysisScreen
import com.aditya1875.pokeverse.presentation.screens.detail.PokemonDetailScreen
import com.aditya1875.pokeverse.presentation.screens.home.HomeScreen
import com.aditya1875.pokeverse.presentation.screens.onboarding.IntroScreen
import com.aditya1875.pokeverse.presentation.screens.settings.SettingsScreen
import com.aditya1875.pokeverse.presentation.screens.splash.SplashScreen
import com.aditya1875.pokeverse.presentation.screens.team.DreamTeam
import com.aditya1875.pokeverse.presentation.screens.theme.ThemeSelectorScreen
import com.aditya1875.pokeverse.presentation.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.utils.NotificationUtils
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.WithBottomBar
import com.aditya1875.pokeverse.presentation.ui.theme.AppTheme
import com.aditya1875.pokeverse.presentation.ui.theme.PokeverseTheme
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext.startKoin
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        NotificationUtils.createNotificationChannel(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        NotificationScheduler.scheduleDailyNotifications(this)

        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        setContent {

            val themePreferences = koinInject<ThemePreferences>()
            val selectedTheme by themePreferences.selectedTheme.collectAsState(
                initial = AppTheme.CHARIZARD
            )

            var currentTheme by remember { mutableStateOf(selectedTheme) }

            LaunchedEffect(selectedTheme) {
                currentTheme = selectedTheme
            }

            val startDestination = remember { mutableStateOf("splash") }
            val context = LocalContext.current

            PokeverseTheme(
                selectedTheme = currentTheme,
            ) {
                val viewModel: PokemonViewModel = koinViewModel()
                val navController = rememberNavController()

                LaunchedEffect(Unit) {

                    val introSeen = ScreenStateManager.isIntroSeen(this@MainActivity)
                    val lastRoute = ScreenStateManager.getLastRoute(this@MainActivity)

                    val validStartRoutes = setOf("home", "dream_team", "settings")

                    startDestination.value = when {
                        !introSeen -> "intro"
                        !lastRoute.isNullOrBlank() && lastRoute in validStartRoutes -> lastRoute
                        else -> "home"
                    }
                }

                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var teamName by rememberSaveable { mutableStateOf("My Team") }
                var favoritesName by rememberSaveable { mutableStateOf("My Favourites") }

                val selectedName =
                    if (selectedTab == 0) teamName else favoritesName

                NavHost(
                    navController = navController,
                    startDestination = startDestination.value,
                ) {
                    composable("splash") { SplashScreen(navController) }
                    composable("intro") { IntroScreen(navController) }
                    composable("home") {
                        WithBottomBar(navController) {
                            HomeScreen(navController)
                        }
                    }

                    composable("dream_team") {
                        WithBottomBar(
                            navController = navController,
                            selectedTab = selectedTab,
                            onTabChange = { selectedTab = it }
                        ) {
                            DreamTeam(
                                navController = navController,
                                team = viewModel.team.collectAsState().value,
                                onRemove = { viewModel.removeFromTeam(it) },
                                selectedName = selectedName,
                                onNameChange = {
                                    if (selectedTab == 0) teamName = it
                                    else favoritesName = it
                                },
                                onTabChange = { selectedTab = it },
                                selectedTab = selectedTab
                            )
                        }
                    }

                    composable("team_analysis") {
                        TeamAnalysisScreen(navController = navController)
                    }
                    composable("pokemon_detail/{pokemonName}") { backStackEntry ->
                        val pokemonName = backStackEntry.arguments?.getString("pokemonName")
                        if (pokemonName != null) {
                            PokemonDetailScreen(pokemonName, navController)
                        } else {
                            PokemonNotFoundScreen(
                                onRetryClick = { navController.navigate(Route.Home.route) }
                            )
                        }
                    }
                    composable("settings") {
                        WithBottomBar(navController) {
                            SettingsScreen(navController)
                        }
                    }

                    composable("theme_selector") {
                        ThemeSelectorScreen(
                            navController = navController,
                            onThemeSelected ={ theme ->
                                currentTheme = theme
                            }
                        )
                    }
                }

                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { entry ->
                        entry.destination.route?.let { route ->
                            val routeToSave = when {
                                route.startsWith("pokemon_detail/") -> "home"
                                route == "splash" -> null
                                route == "intro" -> null
                                route == "theme_selector" -> "settings"
                                route == "team_analysis" -> "dream_team"
                                else -> route
                            }

                            routeToSave?.let {
                                ScreenStateManager.saveCurrentRoute(context, it)
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
    SplashScreen(navController = NavController(LocalContext.current))
}