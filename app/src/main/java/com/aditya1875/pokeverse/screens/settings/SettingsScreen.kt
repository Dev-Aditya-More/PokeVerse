package com.aditya1875.pokeverse.screens.settings

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.screens.settings.components.ResponsiveMetaballSwitch
import com.aditya1875.pokeverse.screens.settings.components.SettingsCard
import com.aditya1875.pokeverse.screens.settings.components.zigZagBackground
import com.aditya1875.pokeverse.ui.viewmodel.SettingsViewModel
import com.aditya1875.pokeverse.utils.EffectCapabilities
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val specialEffectsEnabled by settingsViewModel.specialEffectsEnabled.collectAsStateWithLifecycle()

    val supportsShaders = EffectCapabilities.supportsShaders

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
    var isSpecialEffectsExpanded by remember {
        mutableStateOf(!supportsShaders)
    }

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

                SettingsCard(
                    title = " Special Effects",
                    icon = Icons.Default.AutoAwesome,
                    iconTint = Color.White,
                    iconSize = 25.dp,
                    expanded = isSpecialEffectsExpanded,
                    onExpandToggle = {
                        if (supportsShaders) {
                            isSpecialEffectsExpanded = !isSpecialEffectsExpanded
                        }
                    },
                    trailing = {
                        ResponsiveMetaballSwitch(
                            checked = specialEffectsEnabled && supportsShaders,
                            onCheckedChange = {
                                if (supportsShaders) {
                                    settingsViewModel.toggleSpecialEffects(it)
                                }
                            },
                            enabled = supportsShaders
                        )
                    }
                ) {
                    if (!supportsShaders) {
                        Text(
                            text = "Enhanced visual effects require Android 13 or newer.",
                            color = Color(0xFFFFB74D),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            "You'll see particle effects in Pokémon details.\nTry pressing the Pokémon sprite!",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .zigZagBackground()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {},
                        modifier = Modifier.padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF802525))
                    ) {
                        Text(
                            text = "Socials",
                            color = Color.White,
                        )
                    }
                }

                val socials = listOf(
                    SocialLink(
                        "GitHub",
                        "https://github.com/Dev-Aditya-More/PokeVerse",
                        ImageVector.vectorResource(id = R.drawable.github_brands_solid_full),
                        color = Color.White
                    ),
                    SocialLink(
                        "Twitter",
                        "https://twitter.com/Pokeverse_App",
                        ImageVector.vectorResource(id = R.drawable.x_twitter_brands_solid_full),
                        size = 20.dp,
                        Color.White
                    ),
                    SocialLink(
                        "Linkedin",
                        "https://linkedin.com/in/adityamore2005",
                        ImageVector.vectorResource(id = R.drawable.linkedin_brands_solid_full),
                        size = 20.dp,
                        Color.White
                    ),
                    SocialLink(
                        "BuyMeACoffee",
                        "https://www.buymeacoffee.com/aditya1875q",
                        ImageVector.vectorResource(id = R.drawable.buy_me_coffee_icon),
                        size = 20.dp,
                        Color.White
                    ),
                    SocialLink(
                        "Reddit",
                        "https://www.reddit.com/user/Incredible_aditya123/",
                        ImageVector.vectorResource(id = R.drawable.reddit2),
                        size = 20.dp,
                        Color.White
                    ),
                    SocialLink(
                        "YouTube",
                        "https://youtube.com/@TheCodeForge-yt",
                        ImageVector.vectorResource(id = R.drawable.youtube_brands_solid_full_1_),
                        size = 20.dp,
                        Color.White
                    ),
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
                            Icon(
                                imageVector = social.icon,
                                contentDescription = social.name,
                                modifier = Modifier.size(social.size),
                                tint = social.color,
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


data class SocialLink(val name: String, val url: String, val icon: ImageVector, val size: Dp = 28.dp, val color: Color)
