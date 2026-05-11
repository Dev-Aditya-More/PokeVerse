package com.aditya1875.pokeverse.feature.pokemon.home.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.aditya1875.pokeverse.utils.ScreenStateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RATING_MIN_MINUTES = 20L
private const val PREMIUM_MIN_MINUTES = 40L
private const val MIN_GAP_BETWEEN_POPUPS_MINUTES = 5L
private const val SLOW_POPUP_STARTUP_DELAY_MS = 4_000L

enum class HomePopup { None, Update, Assets, Rating, Premium }

// Single dialog visible at a time. Priority:
//   1. Update  — urgent, shows as soon as DataStore is ready
//   2. Assets  — first-ever launch only, shows after DataStore is ready
//   3. Rating  — after RATING_MIN_MINUTES cumulative AND 4s startup delay AND 5-min gap since last popup
//   4. Premium — after PREMIUM_MIN_MINUTES cumulative AND same guards as Rating
// All thresholds use accumulated DataStore minutes so they work across sessions.

@Composable
fun HomePopupOrchestrator(
    originalAssetsEnabled: Boolean,
    totalSessionMinutes: Long,
    isGuest: Boolean,
    isPremium: Boolean,
    // Update dialog — wire these from SettingsViewModel + BuildConfig
    latestVersionCode: Long = 0L,
    currentVersionCode: Long = 0L,
    onEnableAssets: () -> Unit,
    onRateNow: () -> Unit,
    onGoPremium: () -> Unit,
    onGoUpdate: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var assetsShown by remember { mutableStateOf(false) }
    var ratingShown by remember { mutableStateOf(false) }
    var premiumShown by remember { mutableStateOf(false) }
    var updateShownVersion by remember { mutableStateOf(0L) }
    var lastPopupAtMinutes by remember { mutableLongStateOf(0L) }
    var isReady by remember { mutableStateOf(false) }
    var slowPopupsUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        assetsShown = ScreenStateManager.isAssetsShown(context)
        ratingShown = ScreenStateManager.isRatingShown(context)
        premiumShown = ScreenStateManager.isPremiumShown(context)
        updateShownVersion = ScreenStateManager.getUpdateShownVersion(context)
        lastPopupAtMinutes = ScreenStateManager.getLastPopupShownAtMinutes(context)
        isReady = true
        delay(SLOW_POPUP_STARTUP_DELAY_MS)
        slowPopupsUnlocked = true
    }

    if (!isReady) return

    var activePopup by remember { mutableStateOf<HomePopup>(HomePopup.None) }

    LaunchedEffect(
        assetsShown,
        originalAssetsEnabled,
        ratingShown,
        premiumShown,
        totalSessionMinutes,
        isGuest,
        isPremium,
        latestVersionCode,
        updateShownVersion,
        currentVersionCode,
        slowPopupsUnlocked,
        lastPopupAtMinutes
    ) {
        val minutesSinceLastPopup = totalSessionMinutes - lastPopupAtMinutes
        val cooldownPassed = minutesSinceLastPopup >= MIN_GAP_BETWEEN_POPUPS_MINUTES

        activePopup = when {
            latestVersionCode > currentVersionCode &&
                    updateShownVersion < latestVersionCode -> HomePopup.Update

            !assetsShown && !originalAssetsEnabled -> HomePopup.Assets

            !ratingShown &&
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

    val dismissPopup: (suspend () -> Unit) -> Unit = { action ->
        coroutineScope.launch {
            ScreenStateManager.markLastPopupShownAt(context, totalSessionMinutes)
            lastPopupAtMinutes = totalSessionMinutes
            action()
        }
    }

    when (activePopup) {
        HomePopup.Update -> UpdateAvailableDialog(
            onGoUpdate = {
                dismissPopup {
                    ScreenStateManager.markUpdateShown(context, latestVersionCode)
                    updateShownVersion = latestVersionCode
                }
                onGoUpdate()
            },
            onDismiss = {
                dismissPopup {
                    ScreenStateManager.markUpdateShown(context, latestVersionCode)
                    updateShownVersion = latestVersionCode
                }
            }
        )

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
            onRateNow = { 
                dismissPopup {
                    ScreenStateManager.markRatingShown(context)
                    ratingShown = true
                }
                onRateNow() 
            },
            onMaybeLater = {
                dismissPopup {
                    ScreenStateManager.markRatingShown(context)
                    ratingShown = true
                }
            },
            onDontAsk = {
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

        HomePopup.None -> {

        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Update dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun UpdateAvailableDialog(onGoUpdate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                Icons.Default.SystemUpdate, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                "Update Available",
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "A new version of Dexverse is ready with bug fixes and new features.",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        "Update now for the best experience.",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onGoUpdate, shape = RoundedCornerShape(12.dp)) {
                Text("Update Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } }
    )
}

@Composable
fun AssetsOnboardingDialog(onEnable: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("🎨", fontSize = 36.sp) },
        title = {
            Text(
                "Original Visuals Available",
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Dexverse can show original franchise sprites and play authentic audio for a richer experience.",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "⚠️  These assets belong to their respective owners. This app is unofficial and unaffiliated.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    "You can always change this in Settings → Original Assets.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onEnable, shape = RoundedCornerShape(12.dp)) {
                Text("I Understand, Enable", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not Now") } }
    )
}

@Composable
fun RatingPromptDialog(onRateNow: () -> Unit, onMaybeLater: () -> Unit, onDontAsk: () -> Unit) {
    AlertDialog(
        onDismissRequest = onMaybeLater,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("⭐", fontSize = 36.sp) },
        title = {
            Text(
                "Enjoying Dexverse?",
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { Text("⭐", fontSize = 22.sp) }
                }
                Text(
                    "A quick rating helps us reach more Pokémon fans and keeps the app improving.",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onRateNow, shape = RoundedCornerShape(12.dp)) {
                Text("Rate Now ⭐", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = onMaybeLater) { Text("Maybe Later") }
                TextButton(
                    onClick = onDontAsk,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Don't ask again", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    )
}

@Composable
fun PremiumUpsellDialog(onGoPremium: () -> Unit, onDismiss: () -> Unit) {
    val features = listOf(
        "🎯" to "Hard mode in all 4 games",
        "✨" to "Full Dexverse Experience",
        "🔮" to "Exclusive themes & badges",
        "🚀" to "Support Dexverse's growth",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        icon = { Text("👑", fontSize = 36.sp) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Pokeverse Premium",
                    fontWeight = FontWeight.Black, textAlign = TextAlign.Center
                )
                Text(
                    "Unlock the full experience",
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
            ) { Text("Go Premium 👑", fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Maybe Later") } }
    )
}