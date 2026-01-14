package com.aditya1875.pokeverse.screens.team.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyTeamView(
    navController: NavController,
    viewModel: PokemonViewModel = koinViewModel()
) {

    val pokeballGradient = Brush.verticalGradient(
        listOf(Color(0xFF1E1E2C), Color(0xFF2C5364))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pokeballGradient)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val floatAnim by rememberInfiniteTransition(label = "float").animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "floating"
            )

            Image(
                painter = painterResource(id = R.drawable.teampoke),
                contentDescription = null,
                modifier = Modifier
                    .size(300.dp)
                    .graphicsLayer {
                        translationY = floatAnim
                        alpha = 0.9f
                    }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Your Team is Empty",
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Build a balanced team.\nMix types for better coverage.",
                color = Color(0xFFAAAAAA),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
//                    showTeamPicker = true
                    navController.navigate("home") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // 2. Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // 3. Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC3545),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Build Your Team",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}