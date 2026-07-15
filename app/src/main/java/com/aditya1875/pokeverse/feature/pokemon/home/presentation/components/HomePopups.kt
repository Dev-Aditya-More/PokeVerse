package com.aditya1875.pokeverse.feature.pokemon.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.utils.ScreenStateManager
import kotlinx.coroutines.launch

private const val RATING_MIN_MINUTES = 10L
private const val PREMIUM_MIN_MINUTES = 40L
private const val MIN_GAP_BETWEEN_POPUPS_MINUTES = 30L
private const val SLOW_POPUP_STARTUP_DELAY_MS = 4_000L

enum class HomePopup { None, Assets, Rating, Premium }

// Single dialog visible at a time. Priority:
//   1. Assets  — first-ever launch only, shows after DataStore is ready
//   2. Rating  — signed-in users only; asks "enjoying it?" after RATING_MIN_MINUTES,
//                only launching Play In-App Review if they say yes
//   3. Premium — after PREMIUM_MIN_MINUTES, 30-min gap since last popup

@Composable
fun HomePopupOrchestrator(
    originalAssetsEnabled: Boolean,
    totalSessionMinutes: Long,
    isGuest: Boolean,
    isPremium: Boolean,
    onEnableAssets: () -> Unit,
    onRateNow: () -> Unit,
    onGoPremium: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var assetsShown by remember { mutableStateOf(false) }
    var ratingShown by remember { mutableStateOf(false) }
    var premiumShown by remember { mutableStateOf(false) }
    var lastPopupAtMinutes by remember { mutableLongStateOf(0L) }
    var isReady by remember { mutableStateOf(false) }
    var slowPopupsUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        assetsShown = ScreenStateManager.isAssetsShown(context)
        ratingShown = ScreenStateManager.isRatingShown(context)
        premiumShown = ScreenStateManager.isPremiumShown(context)
        lastPopupAtMinutes = ScreenStateManager.getLastPopupShownAtMinutes(context)
        isReady = true
        kotlinx.coroutines.delay(SLOW_POPUP_STARTUP_DELAY_MS)
        slowPopupsUnlocked = true
    }

    var activePopup by remember { mutableStateOf<HomePopup>(HomePopup.None) }

    LaunchedEffect(
        isReady,
        assetsShown,
        originalAssetsEnabled,
        ratingShown,
        premiumShown,
        totalSessionMinutes,
        isGuest,
        isPremium,
        slowPopupsUnlocked,
        lastPopupAtMinutes
    ) {
        if (!isReady) return@LaunchedEffect

        val minutesSinceLastPopup = totalSessionMinutes - lastPopupAtMinutes
        val cooldownPassed = minutesSinceLastPopup >= MIN_GAP_BETWEEN_POPUPS_MINUTES

        activePopup = when {
            !assetsShown && !originalAssetsEnabled -> HomePopup.Assets

            !ratingShown && !isGuest &&
                    slowPopupsUnlocked &&
                    totalSessionMinutes >= RATING_MIN_MINUTES &&
                    cooldownPassed -> HomePopup.Rating

            ratingShown && !premiumShown &&
                    !isPremium && !isGuest &&
                    slowPopupsUnlocked &&
                    totalSessionMinutes >= PREMIUM_MIN_MINUTES &&
                    cooldownPassed -> HomePopup.Premium

            else -> HomePopup.None
        }
    }

    if (!isReady) return

    val dismissPopup: (suspend () -> Unit) -> Unit = { action ->
        coroutineScope.launch {
            ScreenStateManager.markLastPopupShownAt(context, totalSessionMinutes)
            lastPopupAtMinutes = totalSessionMinutes
            action()
        }
    }

    when (activePopup) {
        HomePopup.Assets -> AssetsOnboardingDialog(
            onEnable = {
                dismissPopup {
                    ScreenStateManager.markAssetsShown(context)
                    assetsShown = true
                }
                onEnableAssets()
            },
            onDismiss = {
                dismissPopup {
                    ScreenStateManager.markAssetsShown(context)
                    assetsShown = true
                }
            }
        )

        HomePopup.Rating -> RatingPromptDialog(
            onEnjoying = {
                dismissPopup {
                    ScreenStateManager.markRatingShown(context)
                    ratingShown = true
                }
                onRateNow()
            },
            onNotReally = {
                dismissPopup {
                    ScreenStateManager.markRatingShown(context)
                    ratingShown = true
                }
            }
        )

        HomePopup.Premium -> PremiumUpsellDialog(
            onGoPremium = {
                dismissPopup {
                    ScreenStateManager.markPremiumShown(context)
                    premiumShown = true
                }
                onGoPremium()
            },
            onDismiss = {
                dismissPopup {
                    ScreenStateManager.markPremiumShown(context)
                    premiumShown = true
                }
            }
        )

        HomePopup.None -> {}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Force update screen — non-dismissible, blocks the entire app
// Shown when BuildConfig.VERSION_CODE < minVersionCode from Firestore
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun UpdateAvailableDialog(
    latestVersionName: String,
    packageName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = { Icon(Icons.Default.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
        title = {
            Text(
                "Update Available",
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "Version $latestVersionName is here with new features and improvements. Update now to stay in the game!",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val uri = "market://details?id=$packageName".toUri()
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {
                        val webUri = "https://play.google.com/store/apps/details?id=$packageName".toUri()
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Maybe Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Preview(showSystemUi = true)
@Composable
fun ForceUpdateScreen(onUpdate: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 28.dp,
                    vertical = 36.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.force_update_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.force_update_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_update_now),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AssetsOnboardingDialog(onEnable: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("🎨", fontSize = 36.sp) },
        title = {
            Text(
                stringResource(R.string.popup_assets_title),
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    stringResource(R.string.popup_assets_body),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        stringResource(R.string.popup_assets_disclaimer),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    stringResource(R.string.popup_assets_change_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onEnable, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.settings_understand_enable), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_not_now)) } }
    )
}

@Composable
fun RatingPromptDialog(onEnjoying: () -> Unit, onNotReally: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNotReally,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("⭐", fontSize = 36.sp) },
        title = {
            Text(
                stringResource(R.string.popup_rating_title),
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                stringResource(R.string.popup_rating_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(onClick = onEnjoying, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.action_rating_yes), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onNotReally) { Text(stringResource(R.string.action_rating_no)) } }
    )
}

@Composable
fun PremiumUpsellDialog(onGoPremium: () -> Unit, onDismiss: () -> Unit) {
    val features = listOf(
        "🎯" to stringResource(R.string.popup_premium_feature_hard_mode),
        "✨" to stringResource(R.string.popup_premium_feature_experience),
        "🔮" to stringResource(R.string.popup_premium_feature_themes),
        "🚀" to stringResource(R.string.popup_premium_feature_support),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        icon = { Text("👑", fontSize = 36.sp) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.popup_premium_title),
                    fontWeight = FontWeight.Black, textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.popup_premium_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                features.forEach { (emoji, text) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(emoji, fontSize = 20.sp)
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGoPremium, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700), contentColor = Color.Black
                )
            ) { Text(stringResource(R.string.action_go_premium), fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_later)) } }
    )
}
