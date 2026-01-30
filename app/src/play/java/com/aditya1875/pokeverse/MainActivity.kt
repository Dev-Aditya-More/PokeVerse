package com.aditya1875.pokeverse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.aditya1875.pokeverse.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.di.appModule
import com.aditya1875.pokeverse.presentation.components.PokemonNotFoundScreen
import com.aditya1875.pokeverse.presentation.screens.analysis.TeamAnalysisScreen
import com.aditya1875.pokeverse.presentation.screens.detail.PokemonDetailScreen
import com.aditya1875.pokeverse.presentation.screens.home.HomeScreen
import com.aditya1875.pokeverse.presentation.screens.onboarding.IntroScreen
import com.aditya1875.pokeverse.presentation.screens.settings.SettingsScreen
import com.aditya1875.pokeverse.presentation.screens.splash.SplashScreen
import com.aditya1875.pokeverse.presentation.screens.team.DreamTeam
import com.aditya1875.pokeverse.presentation.screens.theme.ThemeSelectorScreen
import com.aditya1875.pokeverse.presentation.ui.theme.AppTheme
import com.aditya1875.pokeverse.presentation.ui.theme.PokeverseTheme
import com.aditya1875.pokeverse.presentation.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.utils.NotificationUtils
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.WithBottomBar
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
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

        FirebaseMessaging.getInstance()
            .subscribeToTopic("theme_updates")

        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

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

            PokeverseTheme(
                selectedTheme = currentTheme,
            ) {
                val viewModel: PokemonViewModel = koinViewModel()
                val navController = rememberNavController()

                LaunchedEffect(Unit) {

                    val introSeen = ScreenStateManager.isIntroSeen(this@MainActivity)
                    val lastRoute = ScreenStateManager.getLastRoute(this@MainActivity)

                    startDestination.value = when {
                        !introSeen -> "intro"
                        !lastRoute.isNullOrBlank() -> lastRoute
                        else -> "home"
                    }
                }

                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var teamName by rememberSaveable { mutableStateOf("My Team") }
                var favoritesName by rememberSaveable { mutableStateOf("My Favourites") }

                val selectedName =
                    if (selectedTab == 0) teamName else favoritesName

                val scope = rememberCoroutineScope()

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
                                onRetryClick = { navController.navigate("home") }
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
                                scope.launch {
                                    themePreferences.setTheme(theme)
                                }
                            }
                        )
                    }
                }

                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { entry ->
                        entry.destination.route?.let { route ->
                            ScreenStateManager.saveCurrentRoute(context, route)
                        }
                    }
                }
            }
        }

        FirebaseMessaging.getInstance()
            .subscribeToTopic("theme_updates")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Subscribed to theme_updates topic")
                } else {
                    Log.e("FCM", "Topic subscription failed", task.exception)
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