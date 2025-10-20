package com.aditya1875.pokeverse.screens

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.utils.ScreenStateManager
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

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
