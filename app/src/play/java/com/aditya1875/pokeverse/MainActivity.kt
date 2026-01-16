package com.aditya1875.pokeverse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aditya1875.pokeverse.components.PokemonNotFoundScreen
import com.aditya1875.pokeverse.di.appModule
import com.aditya1875.pokeverse.screens.analysis.TeamAnalysisScreen
import com.aditya1875.pokeverse.screens.detail.PokemonDetailScreen
import com.aditya1875.pokeverse.screens.home.HomeScreen
import com.aditya1875.pokeverse.screens.onboarding.IntroScreen
import com.aditya1875.pokeverse.screens.settings.SettingsScreen
import com.aditya1875.pokeverse.screens.splash.SplashScreen
import com.aditya1875.pokeverse.screens.team.DreamTeam
import com.aditya1875.pokeverse.ui.theme.PokeVerseTheme
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.utils.NotificationUtils
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.WithBottomBar
import com.google.firebase.messaging.FirebaseMessaging
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        NotificationUtils.createNotificationChannel(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        setContent {

            val startDestination = remember { mutableStateOf("splash") }
            val context = LocalContext.current

            PokeVerseTheme {
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
                var selectedTab by rememberSaveable { mutableStateOf(0) }
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

                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { entry ->
                        entry.destination.route?.let { route ->
                            ScreenStateManager.saveCurrentRoute(context, route)
                        }
                    }
                }
            }
        }
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d("FCM", "FCM Token: $token")
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