package com.example.pokeverse.screens

import android.annotation.SuppressLint
import android.content.res.Resources
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.airbnb.lottie.model.content.CircleShape
import kotlin.random.Random

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun AnimatedEmberBackground(types: List<String>) {
    val emberCount = 20
    val emberSize = 3.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val emberColor = remember(types) {
        when {
            "charizard-x" in types.map { it.lowercase() } -> Color(0xFF4FC3F7)
            "charizard-y" in types.map { it.lowercase() } -> Color(0xFFFF7043)
            "gmax" in types.map { it.lowercase() } -> Color(0xFFFF4081)
            "fire" in types -> Color(0xFFFF5722)
            "electric" in types -> Color(0xFFFFEB3B)
            "ghost" in types -> Color(0xFF7E57C2)
            "ice" in types -> Color(0xFF81D4FA)
            "dark" in types -> Color(0xFF616161)
            "psychic" in types -> Color(0xFFE040FB)
            "grass" in types -> Color(0xFF66BB6A)
            "water" in types -> Color(0xFF29B6F6)
            "poison" in types -> Color(0xFF9C27B0)
            "rock" in types -> Color(0xFF795548)
            "dragon" in types -> Color(0xFF673AB7)
            else -> Color(0xFFFFFFFF)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(-1f)
    ) {
        repeat(emberCount) { index ->
            val delay = remember { Random.nextInt(0, 3000) }

            val yOffset by rememberInfiniteTransition(label = "emberY$index")
                .animateFloat(
                    initialValue = screenHeight.value,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 5000, delayMillis = delay, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "y"
                )

            val xOffset by remember { mutableIntStateOf(Random.nextInt(0, screenWidth.value.toInt())) }

            Box(
                modifier = Modifier
                    .offset(x = xOffset.dp, y = yOffset.dp)
                    .size(emberSize)
                    .background(emberColor, CircleShape)
            )
        }
    }
}

data class EmberParticle(
    var x: Float,
    var y: Float,
    var size: Float,
    var velocityY: Float,
    var alpha: Float
) {
    fun update() {
        y -= velocityY
        alpha -= 0.01f
    }

    companion object {
        fun create(): EmberParticle {
            val screenWidth = Resources.getSystem().displayMetrics.widthPixels
            val screenHeight = Resources.getSystem().displayMetrics.heightPixels
            return EmberParticle(
                x = (0..screenWidth).random().toFloat(),
                y = screenHeight.toFloat(),
                size = (4..10).random().toFloat(),
                velocityY = (1..5).random().toFloat(),
                alpha = 1f
            )
        }
    }
}



//@Preview
//@Composable
//fun EmberTestPreview() {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black)
//    ) {
//        AnimatedEmberBackground(
//
//        )
//    }
//}

