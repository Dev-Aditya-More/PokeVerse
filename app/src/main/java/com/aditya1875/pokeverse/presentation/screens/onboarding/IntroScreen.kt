package com.aditya1875.pokeverse.presentation.screens.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.presentation.components.NeuromorphicButton
import com.aditya1875.pokeverse.presentation.components.QuoteCarousel
import com.aditya1875.pokeverse.presentation.screens.home.components.Route
import com.aditya1875.pokeverse.utils.ScreenStateManager.markIntroSeen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun IntroScreen(
    navController: NavController
) {
    val imageList = listOf(
        painterResource(id = R.drawable.dragonite1),
        painterResource(id = R.drawable.snorlax),
        painterResource(id = R.drawable.ashninja)
    )

    val pageCount = imageList.size
    var currentPage by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Auto-slide
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentPage = (currentPage + 1) % pageCount
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // Crossfade image
            Crossfade(
                targetState = currentPage,
                animationSpec = tween(durationMillis = 600),
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Image(
                    painter = imageList[page],
                    contentDescription = "Intro Image $page",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.85f
                            scaleX = 1.08f
                            scaleY = 1.08f
                        }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Content column at bottom
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QuoteCarousel(
                        quotesGenerate = listOf(
                            "More than a Pokédex.",
                            "Understand your Pokémon.",
                            "Turn data into mastery."
                        )
                    )
                }

                // Continue button
                NeuromorphicButton(
                    text = "Continue",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            markIntroSeen(context)
                        }
                        navController.navigate(Route.BottomBar.Home.route) {
                            popUpTo(Route.Onboarding.route) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun IntroScreenPreview() {
    IntroScreen(
        navController = NavController(LocalContext.current)
    )
}