package com.aditya1875.pokeverse.screens.splash

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
import androidx.compose.runtime.remember
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.screens.splash.components.CardShader
import com.aditya1875.pokeverse.screens.splash.components.SmokeShader

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(showBackground = true)
@Composable
fun SplashScreen(
    navController: NavController = rememberNavController(),
    getNext: (context: Context) -> String = { "home" }
) {
    val context = LocalContext.current
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(2000, easing = { OvershootInterpolator(2f).getInterpolation(it) })
        )

        val next = getNext(context)
        navController.navigate(next) {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        // Full-screen shader background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            CardShader(SmokeShader().shader, SmokeShader().speed)
        } else null

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
        }
    }
}