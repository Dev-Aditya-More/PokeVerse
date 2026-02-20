package com.aditya1875.pokeverse.presentation.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.presentation.screens.home.components.Route
import com.aditya1875.pokeverse.presentation.screens.settings.components.ResponsiveMetaballSwitch
import com.aditya1875.pokeverse.presentation.screens.settings.components.SettingsCard
import com.aditya1875.pokeverse.presentation.screens.settings.components.zigZagBackground
import com.aditya1875.pokeverse.presentation.ui.viewmodel.SettingsViewModel
import com.aditya1875.pokeverse.utils.EffectCapabilities
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val specialEffectsEnabled by settingsViewModel.specialEffectsEnabled.collectAsStateWithLifecycle()

    val supportsShaders = EffectCapabilities.supportsShaders

    var isAboutExpanded by remember { mutableStateOf(false) }
    var isSpecialEffectsExpanded by remember {
        mutableStateOf(!supportsShaders)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 26.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    expanded = isAboutExpanded,
                    onExpandToggle = { isAboutExpanded = !isAboutExpanded }
                ) {
                    Text(
                        "Crafted with ❤️ using Jetpack Compose",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Version ${BuildConfig.VERSION_NAME}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                SettingsCard(
                    title = "Special Effects",
                    icon = Icons.Default.AutoAwesome,
                    iconTint = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            "You'll see particle effects in Pokémon details.\nTry pressing the Pokémon sprite!",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                var themeExpanded by rememberSaveable { mutableStateOf(false) }

                SettingsCard(
                    title = "Theme",
                    icon = Icons.Default.Palette,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    iconSize = 25.dp,
                    expanded = themeExpanded,
                    onExpandToggle = {
                        themeExpanded = !themeExpanded
                    },
                    trailing = {
                        IconButton(
                            onClick = {
                                navController.navigate(Route.ThemeSelector.route)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Change theme"
                            )
                        }
                    }
                ) {
                    Text(
                        "Choose your starter theme",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                val context = LocalContext.current

                var shareExpanded by rememberSaveable { mutableStateOf(false) }

                SettingsCard(
                    title = "Share Pokeverse",
                    icon = Icons.Default.Share,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    expanded = shareExpanded,
                    onExpandToggle = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Hey mate, Check out Pokeverse — a clean Pokédex app for Pokémon fans:\n" +
                                        "https://play.google.com/store/apps/details?id=${context.packageName}"
                            )
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share Pokeverse via")
                        )

                        shareExpanded = !shareExpanded
                    }
                ) {
                    Text(
                        "Tell your friends about Pokeverse!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                var rateExpanded by rememberSaveable { mutableStateOf(false) }

                SettingsCard(
                    title = "Rate us on Google Play",
                    icon = Icons.Default.StarRate,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    expanded = rateExpanded,
                    onExpandToggle = {
                        val packageName = context.packageName

                        val uri = "market://details?id=$packageName".toUri()
                        val goToMarket = Intent(Intent.ACTION_VIEW, uri).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NO_HISTORY or
                                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                            )
                        }

                        try {
                            context.startActivity(goToMarket)
                        } catch (e: ActivityNotFoundException) {
                            val webUri =
                                "https://play.google.com/store/apps/details?id=$packageName".toUri()
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }

                        rateExpanded = !rateExpanded
                    }
                ) {
                    Text(
                        "If you like Pokeverse, please take a moment to rate it on Play Store. It really helps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .zigZagBackground(
                            outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {},
                        modifier = Modifier.padding(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Socials",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                val socials = listOf(
                    SocialLink(
                        "Github",
                        "https://github.com/Dev-Aditya-More/PokeVerse",
                        ImageVector.vectorResource(id = R.drawable.github_brands_solid_full),
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    SocialLink(
                        "Twitter",
                        "https://twitter.com/Pokeverse_App",
                        ImageVector.vectorResource(id = R.drawable.x_twitter_brands_solid_full),
                        size = 20.dp,
                        MaterialTheme.colorScheme.onSurface
                    ),
                    SocialLink(
                        "Linkedin",
                        "https://linkedin.com/in/adityamore2005",
                        ImageVector.vectorResource(id = R.drawable.linkedin_brands_solid_full),
                        size = 20.dp,
                        MaterialTheme.colorScheme.onSurface
                    ),
                    SocialLink(
                        "BuyMeACoffee",
                        "https://www.buymeacoffee.com/aditya1875q",
                        ImageVector.vectorResource(id = R.drawable.buy_me_coffee_icon),
                        size = 20.dp,
                        MaterialTheme.colorScheme.onSurface
                    ),
                    SocialLink(
                        "Reddit",
                        "https://www.reddit.com/user/Incredible_aditya123/",
                        ImageVector.vectorResource(id = R.drawable.reddit2),
                        size = 20.dp,
                        MaterialTheme.colorScheme.onSurface
                    ),
                    SocialLink(
                        "YouTube",
                        "https://youtube.com/@TheCodeForge-yt",
                        ImageVector.vectorResource(id = R.drawable.youtube_brands_solid_full_1_),
                        size = 20.dp,
                        MaterialTheme.colorScheme.onSurface
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
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
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

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "© Pokéverse 2026. All rights reserved.",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(0.6f)
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

data class SocialLink(
    val name: String,
    val url: String,
    val icon: ImageVector,
    val size: Dp = 28.dp,
    val color: Color
)
