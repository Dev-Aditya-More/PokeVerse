package com.aditya1875.pokeverse.presentation.screens.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
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

@Composable
fun IntroScreen(
    navController: NavController
) {
    val imageList = listOf(
        painterResource(id = R.drawable.team),
        painterResource(id = R.drawable.team3),
        painterResource(id = R.drawable.team5)
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
            .navigationBarsPadding()
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 8.dp)
        ) {

            // Crossfade image transition
            Crossfade(
                targetState = currentPage,
                animationSpec = tween(durationMillis = 600),
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Image(
                    painter = imageList[page],
                    contentDescription = "Intro Image $page",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Quote text at bottom
            Box(modifier = Modifier.fillMaxSize()) {
                QuoteCarousel(
                    quotesGenerate = listOf(
                        "a whole new world of pokemons",
                        "Your favourite pokemons at one place",
                        "Discover the legends behind the stats"
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }

            // Continue button at bottom
            NeuromorphicButton(
                text = "Continue",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 32.dp, end = 32.dp)
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                scope.launch {
                    CoroutineScope(Dispatchers.IO).launch {
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


@Preview
@Composable
fun IntroScreenPreview() {
    IntroScreen(
        navController = NavController(LocalContext.current)
    )
}