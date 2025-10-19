@file:Suppress("DEPRECATION")

package com.aditya1875.pokeverse

import android.content.Context
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
}

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val scale = remember { Animatable(0.8f) }
    val viewModel: PokemonViewModel = koinViewModel()
    val showTagline by viewModel.showTagline.collectAsStateWithLifecycle()
    val alpha = remember { Animatable(0f) }
    val isDark = isSystemInDarkTheme()
    val taglineColor = if (isDark) Color(0xFFBDBDBD) else Color(0xFF424242)

    LaunchedEffect(Unit) {
        // Scale animation for logo
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = { OvershootInterpolator(2f).getInterpolation(it) })
        )

        if (showTagline) {
            alpha.animateTo(1f, animationSpec = tween(800))
            delay(1800)
            ScreenStateManager.markFirstLaunchShown(context)
        } else {
            delay(2000)
        }

        val next = if (ScreenStateManager.isIntroSeen(context)) "home" else "intro"
        navController.navigate(next) {
            popUpTo("splash") { inclusive = true }
        }
    }

    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF3C3C3C), Color(0xFF1A1A1A))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.mysplash2),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(450.dp)
                    .scale(scale.value)
            )

            if (showTagline) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Explore the legends behind the stats",
                    color = taglineColor,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier
                        .alpha(alpha.value)
                        .scale(scale.value.coerceAtMost(1.05f))
                )
            }

        }
    }
}


@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonNotFoundScreen(onRetryClick: () -> Unit = {}) {
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A))
    )

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(100.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "We couldn’t find that Pokémon...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Try searching again or check the spelling.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onRetryClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        containerColor = Color(0xFF802525)
                    )
                ) {
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