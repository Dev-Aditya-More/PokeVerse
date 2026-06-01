package com.aditya1875.pokeverse.feature.pokemon.settings.presentation.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.core.navigation.components.Route
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.components.ResponsiveMetaballSwitch
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.components.SettingsCard
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.components.zigZagBackground
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels.SettingsViewModel
import com.aditya1875.pokeverse.utils.EffectCapabilities
import com.aditya1875.pokeverse.utils.LocaleHelper
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val specialEffectsEnabled by settingsViewModel.specialEffectsEnabled
        .collectAsStateWithLifecycle()
    val supportsShaders = EffectCapabilities.supportsShaders

    var isAboutExpanded by remember { mutableStateOf(false) }
    var isSpecialEffectsExpanded by remember { mutableStateOf(!supportsShaders) }

    val originalAssetsEnabled by settingsViewModel.originalAssetsEnabled
        .collectAsStateWithLifecycle()

    var showOriginalAssetsDialog by remember { mutableStateOf(false) }
    var isPrivacyPolicyExpanded by remember { mutableStateOf(false) }
    var isHelpExpanded by remember { mutableStateOf(false) }
    var isAssetsExpanded by remember { mutableStateOf(false) }

    val selectedLanguage by settingsViewModel.selectedLanguage.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 26.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
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

                // About
                SettingsCard(
                    title = stringResource(R.string.settings_about),
                    icon = Icons.Default.Info,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    expanded = isAboutExpanded,
                    onExpandToggle = { isAboutExpanded = !isAboutExpanded }
                ) {
                    Text(
                        stringResource(R.string.settings_about_crafted),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Language selector
                val currentLangName = when (selectedLanguage) {
                    LocaleHelper.LANG_PORTUGUESE_BR -> stringResource(R.string.language_portuguese_br)
                    LocaleHelper.LANG_HINDI -> stringResource(R.string.language_hindi)
                    LocaleHelper.LANG_FRENCH -> stringResource(R.string.language_french)
                    else -> stringResource(R.string.language_english)
                }
                SettingsCard(
                    title = stringResource(R.string.settings_language),
                    icon = Icons.Default.Language,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    expanded = false,
                    onExpandToggle = { showLanguageDialog = true },
                    trailing = {
                        TextButton(onClick = { showLanguageDialog = true }) {
                            Text(
                                currentLangName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {}

                if (showLanguageDialog) {
                    val languages = listOf(
                        LocaleHelper.LANG_ENGLISH to stringResource(R.string.language_english),
                        LocaleHelper.LANG_PORTUGUESE_BR to stringResource(R.string.language_portuguese_br),
                        LocaleHelper.LANG_HINDI to stringResource(R.string.language_hindi),
                        LocaleHelper.LANG_FRENCH to stringResource(R.string.language_french)
                    )
                    AlertDialog(
                        onDismissRequest = { showLanguageDialog = false },
                        shape = RoundedCornerShape(24.dp),
                        title = { Text(stringResource(R.string.settings_language_dialog_title)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                languages.forEach { (tag, name) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (tag != selectedLanguage) {
                                                    settingsViewModel.setLanguage(tag)
                                                    showLanguageDialog = false
                                                    activity?.recreate()
                                                } else {
                                                    showLanguageDialog = false
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedLanguage == tag,
                                            onClick = null
                                        )
                                        Text(name, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showLanguageDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

                // Original Assets
                SettingsCard(
                    title = stringResource(R.string.settings_original_assets),
                    icon = Icons.Default.PhotoLibrary,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    expanded = isAssetsExpanded,
                    onExpandToggle = { isAssetsExpanded = !isAssetsExpanded },
                    trailing = {
                        ResponsiveMetaballSwitch(
                            checked = originalAssetsEnabled,
                            onCheckedChange = { checked ->
                                if (checked) showOriginalAssetsDialog = true
                                else settingsViewModel.toggleOriginalAssetsEnabled()
                            }
                        )
                    }
                ) {
                    Text(
                        stringResource(R.string.settings_original_assets_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                if (showOriginalAssetsDialog) {
                    AlertDialog(
                        onDismissRequest = { showOriginalAssetsDialog = false },
                        title = { Text(stringResource(R.string.settings_original_assets)) },
                        text = { Text(stringResource(R.string.settings_original_assets_dialog_body)) },
                        confirmButton = {
                            TextButton(onClick = {
                                settingsViewModel.toggleOriginalAssetsEnabled()
                                showOriginalAssetsDialog = false
                            }) {
                                Text(stringResource(R.string.settings_understand_enable))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showOriginalAssetsDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

                // Special Effects
                SettingsCard(
                    title = stringResource(R.string.settings_special_effects),
                    icon = Icons.Default.AutoAwesome,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    iconSize = 25.dp,
                    expanded = isSpecialEffectsExpanded,
                    onExpandToggle = {
                        if (supportsShaders) isSpecialEffectsExpanded = !isSpecialEffectsExpanded
                    },
                    trailing = {
                        ResponsiveMetaballSwitch(
                            checked = specialEffectsEnabled && supportsShaders,
                            onCheckedChange = {
                                if (supportsShaders) settingsViewModel.toggleSpecialEffects(it)
                            },
                            enabled = supportsShaders
                        )
                    }
                ) {
                    Text(
                        text = if (!supportsShaders)
                            stringResource(R.string.settings_special_effects_requires)
                        else
                            stringResource(R.string.settings_special_effects_desc),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Theme
                var themeExpanded by rememberSaveable { mutableStateOf(false) }
                SettingsCard(
                    title = stringResource(R.string.settings_theme),
                    icon = Icons.Default.Palette,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    iconSize = 25.dp,
                    expanded = themeExpanded,
                    onExpandToggle = { themeExpanded = !themeExpanded },
                    trailing = {
                        IconButton(onClick = { navController.navigate(Route.ThemeSelector.route) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.settings_theme_change)
                            )
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.settings_theme_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Share
                var shareExpanded by rememberSaveable { mutableStateOf(false) }
                val shareMessage = stringResource(R.string.settings_share_message, context.packageName)
                val shareChooser = stringResource(R.string.settings_share_chooser)
                SettingsCard(
                    title = stringResource(R.string.settings_share),
                    icon = Icons.Default.Share,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    expanded = shareExpanded,
                    onExpandToggle = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, shareChooser))
                        shareExpanded = !shareExpanded
                    }
                ) {
                    Text(
                        stringResource(R.string.settings_share_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Rate
                var rateExpanded by rememberSaveable { mutableStateOf(false) }
                SettingsCard(
                    title = stringResource(R.string.settings_rate),
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
                        stringResource(R.string.settings_rate_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Privacy Policy
                SettingsCard(
                    title = stringResource(R.string.settings_privacy),
                    icon = Icons.Default.PrivacyTip,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    expanded = isPrivacyPolicyExpanded,
                    onExpandToggle = {
                        isPrivacyPolicyExpanded = !isPrivacyPolicyExpanded
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/Dev-Aditya-More/PokeVerse/raw/master/Privacy_Policy.md".toUri()
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Text(
                        stringResource(R.string.settings_privacy_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Help & Support
                SettingsCard(
                    title = stringResource(R.string.settings_help),
                    icon = Icons.AutoMirrored.Filled.Help,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    expanded = isHelpExpanded,
                    onExpandToggle = {
                        isHelpExpanded = !isHelpExpanded
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:aditya1875more@gmail.com".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, "Dexverse Support")
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(
                        stringResource(R.string.settings_help_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Socials header
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
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.settings_socials),
                            color = MaterialTheme.colorScheme.onSurface
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
                        "https://x.com/Dexverse_App",
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

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_attribution_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_attribution_1),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = stringResource(R.string.settings_attribution_2),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = stringResource(R.string.settings_copyright),
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
