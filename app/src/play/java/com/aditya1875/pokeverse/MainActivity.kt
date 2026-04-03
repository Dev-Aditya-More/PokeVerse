package com.aditya1875.pokeverse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.TeamAnalysisScreen
import com.aditya1875.pokeverse.feature.core.navigation.components.Route
import com.aditya1875.pokeverse.feature.core.navigation.components.WithBottomBar
import com.aditya1875.pokeverse.feature.core.ui.components.PokemonNotFoundScreen
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
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
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.feature.leaderboard.presentation.screens.LeaderboardScreen
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.screens.PokemonDetailScreen
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.screens.HomeScreen
import com.aditya1875.pokeverse.feature.pokemon.onboarding.IntroScreen
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.EditProfileDialog
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.screens.ProfileScreen
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.screens.SettingsScreen
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels.SettingsViewModel
import com.aditya1875.pokeverse.feature.pokemon.splash.SplashScreen
import com.aditya1875.pokeverse.feature.pokemon.theme_selector.ThemeSelectorScreen
import com.aditya1875.pokeverse.feature.pokemon.theme_selector.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.feature.team.presentation.screens.DreamTeam
import com.aditya1875.pokeverse.ui.theme.AppTheme
import com.aditya1875.pokeverse.ui.theme.PokeverseTheme
import com.aditya1875.pokeverse.utils.NotificationUtils
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    private var sessionStartMs = -1L
    private val settingsViewModel: SettingsViewModel by inject()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val billingManager by inject<IBillingManager>()
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

            val context = LocalContext.current
            val navController = rememberNavController()

            XPOverlay(
                result = shownXpResult,
                onDismiss = { shownXpResult = null }
            ) {
                PokeverseTheme(selectedTheme = currentTheme) {
                    AppNavGraph(
                        navController,
                        context
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sessionStartMs = System.currentTimeMillis()
    }

    override fun onStop() {
        super.onStop()
        if (sessionStartMs < 0) return
        val minutes = (System.currentTimeMillis() - sessionStartMs) / 60_000L
        if (minutes >= 1) {
            settingsViewModel.recordSessionMinutes(minutes)
        }
        sessionStartMs = -1L
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