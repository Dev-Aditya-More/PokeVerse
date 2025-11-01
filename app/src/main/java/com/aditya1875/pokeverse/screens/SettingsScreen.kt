package com.aditya1875.pokeverse.screens

import android.content.Intent
import android.media.Image
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.components.CustomProgressIndicator
import com.aditya1875.pokeverse.components.ResponsiveMetaballSwitch
import com.aditya1875.pokeverse.components.SettingsCard
import com.aditya1875.pokeverse.ui.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val specialEffectsEnabled by settingsViewModel.specialEffectsEnabled.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "Gradient")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    val animatedGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A)),
        startY = animatedOffset,
        endY = animatedOffset + 1000f
    )

    var isAboutExpanded by remember { mutableStateOf(false) }
    var isSpecialEffectsExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedGradient)
                .padding(padding)
        ) {
            // Pokéball watermark
            CustomProgressIndicator(
                Modifier
                    .size(280.dp)
                    .align(Alignment.Center)
                    .graphicsLayer(alpha = 0.08f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(top = 15.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // About Section
                SettingsCard(
                    title = "About",
                    icon = Icons.Default.Info,
                    iconTint = Color.White,
                    expanded = isAboutExpanded,
                    onExpandToggle = { isAboutExpanded = !isAboutExpanded }
                ) {
                    Text("Crafted with ❤️ using Jetpack Compose", color = Color.Gray)
                    Text("Version ${BuildConfig.VERSION_NAME}", color = Color.Gray)
                }

                // Special Effects Section
                SettingsCard(
                    title = " Special Effects",
                    icon = Icons.Default.AutoAwesome,
                    iconTint = Color.White,
                    iconSize = 25.dp,
                    expanded = isSpecialEffectsExpanded,
                    onExpandToggle = { isSpecialEffectsExpanded = !isSpecialEffectsExpanded },
                    trailing = {
                        ResponsiveMetaballSwitch(
                            checked = specialEffectsEnabled,
                            onCheckedChange = { settingsViewModel.toggleSpecialEffects(it) },
                            enabled = true
                        )
                    }
                ) {
                    Text(
                        "You'll see particle effects in Pokémon details.\nTry pressing the Pokémon sprite!",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.3f)
                        .background(Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.Gray.copy(alpha = 0.5f), Color.Transparent)
                        )),
                    thickness = DividerDefaults.Thickness, color = DividerDefaults.color
                )

                // Socials Section
                Text(
                    "Connect with us",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                val socials = listOf(
                    SocialLink(
                        "GitHub",
                        "https://github.com/Dev-Aditya-More/PokeVerse",
                        painterResource(R.drawable.github),
                        color = Color.White
                    ),
                    SocialLink(
                        " YouTube",
                        "https://youtube.com/@TheCodeForge-yt",
                        painterResource(R.drawable.youtube),
                        size = 20.dp,
                        Color.Red
                    ),
                    SocialLink(
                        " Twitter",
                        "https://twitter.com/Pokeverse_App",
                        painterResource(R.drawable.xlogo),
                        size = 20.dp,
                        Color.White
                    )
                )

                socials.forEach { social ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, social.url.toUri())
                                context.startActivity(intent)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Image(
                                painter = social.icon,
                                contentDescription = social.name,
                                modifier = Modifier.size(social.size),
                                colorFilter = if (social.name.contains("YouTube", ignoreCase = true)) {
                                    null
                                } else {
                                    ColorFilter.tint(color = social.color) // tint all other icons white
                                }

                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = social.name,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "© Pokéverse 2025. All rights reserved.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(0.6f)
                )
            }
        }
    }
}


data class SocialLink(val name: String, val url: String, val icon: Painter, val size: Dp = 30.dp, val color: Color)
