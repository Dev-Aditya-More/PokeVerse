package com.aditya1875.pokeverse.presentation.screens.splash

import android.content.Context
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import android.view.animation.OvershootInterpolator
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.presentation.screens.home.components.Route
import com.aditya1875.pokeverse.presentation.screens.splash.components.CardShader
import com.aditya1875.pokeverse.presentation.screens.splash.components.SmokeShader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(showBackground = true)
@Composable
fun SplashScreen(
    navController: NavController = rememberNavController(),
    getNext: (context: Context) -> String = { "home" }
) {
    val context = LocalContext.current
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade in and scale up animation
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
        }

        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(
                durationMillis = 1200,
                easing = { OvershootInterpolator(1.5f).getInterpolation(it) }
            )
        )

        // Subtle breathing effect
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )

        delay(1700)

        // Fade out before navigation
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(300, easing = FastOutLinearInEasing)
        )

        val next = getNext(context)
        navController.navigate(next) {
            popUpTo(Route.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF802525)), // Fallback background color
        contentAlignment = Alignment.Center
    ) {
        val smokeShader = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SmokeShader()
            } else null
        }

        smokeShader?.let {
            CardShader(
                shaderStr = it.shader,
                speed = it.speed
            )
        }

        // Subtle gradient overlay for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF802525).copy(alpha = 0.3f)
                        ),
                        center = Offset(0.5f, 0.5f),
                        radius = 1000f
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alpha.value)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(500.dp)
                        .scale(scale.value)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Image(
                    painter = painterResource(id = R.drawable.mysplash2),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(450.dp)
                        .scale(scale.value)
                )
            }
        }
    }
}