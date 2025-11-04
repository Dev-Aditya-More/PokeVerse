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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.aditya1875.pokeverse.notifications.NotificationWorker
import com.aditya1875.pokeverse.screens.DreamTeam
import com.aditya1875.pokeverse.screens.HomeScreen
import com.aditya1875.pokeverse.screens.IntroScreen
import com.aditya1875.pokeverse.screens.PokemonDetailScreen
import com.aditya1875.pokeverse.screens.SettingsScreen
import com.aditya1875.pokeverse.screens.SplashScreen
import com.aditya1875.pokeverse.ui.theme.PokeVerseTheme
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.utils.NotificationUtils
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.WithBottomBar
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext.startKoin
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        NotificationUtils.createNotificationChannel(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, java.util.concurrent.TimeUnit.DAYS)
            .setInitialDelay(10, java.util.concurrent.TimeUnit.MINUTES) // optional
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_pokeverse_notification",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

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

@Preview(showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(navController = NavController(LocalContext.current))
}