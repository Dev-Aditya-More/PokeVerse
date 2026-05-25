package com.aditya1875.pokeverse.feature.pokemon.home.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.utils.ScreenStateManager
import kotlinx.coroutines.launch

private const val RATING_MIN_MINUTES = 20L
private const val PREMIUM_MIN_MINUTES = 40L
private const val MIN_GAP_BETWEEN_POPUPS_MINUTES = 30L
private const val SLOW_POPUP_STARTUP_DELAY_MS = 4_000L

enum class HomePopup { None, Update, Assets, Rating, Premium }

// Single dialog visible at a time. Priority:
//   1. Update  — urgent, shows as soon as DataStore is ready
//   2. Assets  — first-ever launch only, shows after DataStore is ready
//   3. Rating  — signed-in users only; triggers Play In-App Review after RATING_MIN_MINUTES
//   4. Premium — after PREMIUM_MIN_MINUTES, 30-min gap since last popup

@Composable
fun HomePopupOrchestrator(
    originalAssetsEnabled: Boolean,
    totalSessionMinutes: Long,
    isGuest: Boolean,
    isPremium: Boolean,
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
        kotlinx.coroutines.delay(SLOW_POPUP_STARTUP_DELAY_MS)
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

        HomePopup.Rating -> {
            // Trigger Play In-App Review silently — no custom dialog shown
            LaunchedEffect(Unit) {
                onRateNow()
                ScreenStateManager.markLastPopupShownAt(context, totalSessionMinutes)
                ScreenStateManager.markRatingShown(context)
                lastPopupAtMinutes = totalSessionMinutes
                ratingShown = true
            }
        }

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
                stringResource(R.string.popup_update_title),
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    stringResource(R.string.popup_update_body),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        stringResource(R.string.popup_update_tip),
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
                Text(stringResource(R.string.action_update_now), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_later)) } }
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
