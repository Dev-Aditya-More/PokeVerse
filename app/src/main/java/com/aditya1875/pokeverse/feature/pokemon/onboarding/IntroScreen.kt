package com.aditya1875.pokeverse.feature.pokemon.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.core.navigation.components.Route
import com.aditya1875.pokeverse.utils.ScreenStateManager.markIntroSeen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class IntroSlide(
    val imageRes: Int,
    val badge: String,
    val headline: String,
    val subtitle: String
)

private val slides = listOf(
    IntroSlide(
        imageRes = R.drawable.dragonite1,
        badge = "Pokédex",
        headline = "Your ultimate\nPokémon companion",
        subtitle = "Explore the complete Pokédex with rich stats, moves, and lore."
    ),
    IntroSlide(
        imageRes = R.drawable.snorlax,
        badge = "Mini Games",
        headline = "Play. Compete.\nMaster.",
        subtitle = "Test your knowledge with PokeGuess, PokeQuiz, PokeMatch & more."
    ),
    IntroSlide(
        imageRes = R.drawable.ashninja,
        badge = "Leaderboard",
        headline = "Rise among the\nbest trainers",
        subtitle = "Earn XP, level up, and compete with trainers worldwide."
    )
)

private val Purple = Color(0xFF7C4DFF)
private val Cyan = Color(0xFF00E5FF)

@Suppress("EffectKeys")
@Composable
fun IntroScreen(navController: NavController, modifier: Modifier = Modifier) {
    var currentPage by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            currentPage = (currentPage + 1) % slides.size
        }
    }

    Scaffold(
        containerColor = Color.Black,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // 1. Full-screen crossfade image
            Crossfade(
                targetState = currentPage,
                animationSpec = tween(durationMillis = 700),
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Image(
                    painter = painterResource(id = slides[page].imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.80f
                            scaleX = 1.06f
                            scaleY = 1.06f
                        }
                )
            }

            // 2. Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            0.35f to Color.Transparent,
                            0.6f to Color.Black.copy(alpha = 0.5f),
                            1f to Color.Black.copy(alpha = 0.95f)
                        )
                    )
            )

            // 3. Top branding
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DEXVERSE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 5.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(36.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Brush.horizontalGradient(listOf(Purple, Cyan)))
                )
            }

            // 4. Bottom content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Badge pill
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        (fadeIn(tween(350))) togetherWith (fadeOut(tween(200)))
                    },
                    label = "badge"
                ) { page ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = slides[page].badge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Headline
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        (slideInVertically(tween(400)) { it / 3 } + fadeIn(tween(400))) togetherWith
                                (slideOutVertically(tween(300)) { -it / 3 } + fadeOut(tween(250)))
                    },
                    label = "headline"
                ) { page ->
                    Text(
                        text = slides[page].headline,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 38.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Subtitle
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        (slideInVertically(tween(450)) { it / 3 } + fadeIn(tween(450))) togetherWith
                                (slideOutVertically(tween(300)) { -it / 3 } + fadeOut(tween(250)))
                    },
                    label = "subtitle"
                ) { page ->
                    Text(
                        text = slides[page].subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Page dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.indices.forEach { index ->
                        val isSelected = currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 22.dp else 6.dp,
                            animationSpec = tween(300),
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(6.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected)
                                        Brush.horizontalGradient(listOf(Purple, Cyan))
                                    else
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.35f),
                                                Color.White.copy(alpha = 0.35f)
                                            )
                                        )
                                )
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // CTA button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(Purple, Cyan)))
                        .clickable {
                            scope.launch {
                                withContext(Dispatchers.IO) { markIntroSeen(context) }
                                navController.navigate(Route.BottomBar.Home.route) {
                                    popUpTo(Route.Onboarding.route) { inclusive = true }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun IntroScreenPreview() {
    IntroScreen(navController = NavController(LocalContext.current))
}