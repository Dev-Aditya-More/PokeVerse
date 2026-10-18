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
import com.aditya1875.pokeverse.feature.widget.streak.StreakWidgetManualStepsDialog
import com.aditya1875.pokeverse.feature.widget.streak.StreakWidgetPinning
import com.aditya1875.pokeverse.feature.widget.streak.StreakWidgetPromptDialog
import com.aditya1875.pokeverse.utils.ScreenStateManager
import kotlinx.coroutines.launch
import com.aditya1875.pokeverse.feature.dexmode.data.DexModeStore
import com.aditya1875.pokeverse.feature.dexmode.presentation.DexModeAnnouncementDialog
import org.koin.compose.koinInject
import com.aditya1875.pokeverse.feature.referral.data.ReferralRepository
import com.aditya1875.pokeverse.feature.feedback.data.FeedbackRepository
import com.aditya1875.pokeverse.feature.referral.presentation.ReferralClaimDialog
import com.aditya1875.pokeverse.feature.referral.presentation.InvitePromptDialog
import com.aditya1875.pokeverse.feature.feedback.presentation.FeedbackPromptDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val RATING_MIN_MINUTES = 10L
private const val PREMIUM_MIN_MINUTES = 40L
private const val MIN_GAP_BETWEEN_POPUPS_MINUTES = 30L
private const val SLOW_POPUP_STARTUP_DELAY_MS = 4_000L
/** Ask about the streak widget once there's a streak worth protecting. */
private const val WIDGET_MIN_STREAK = 2
/** At most two asks, and the second only after this long. */
private const val WIDGET_MAX_ASKS = 2
private const val WIDGET_REASK_GAP_MS = 5L * 24 * 60 * 60 * 1000
private const val INVITE_MIN_MINUTES = 15L
private const val INVITE_MAX_ASKS = 2
private const val INVITE_REASK_GAP_MS = 10L * 24 * 60 * 60 * 1000
private const val FEEDBACK_MIN_MINUTES = 25L
private const val FEEDBACK_MAX_ASKS = 2
private const val FEEDBACK_REASK_GAP_MS = 21L * 24 * 60 * 60 * 1000

enum class HomePopup { None, Assets, DexMode, ReferralClaim, Widget, Rating, Invite, Feedback, Premium }

// Single dialog visible at a time. Priority:
//   1. Assets  — first-ever launch only, shows after DataStore is ready
//   1b. DexMode — one-time announcement of Dex Mode (skipped once the user has opened it)
//   1c. ReferralClaim — installed via a friend's invite link; signed in and not yet redeemed
//   2. Widget  — streak ≥ 2 days and the widget isn't on the home screen yet; asked at
//                most twice (5+ days apart). Android never surfaces widgets on its own.
//   3. Rating  — signed-in users only; asks "enjoying it?" after RATING_MIN_MINUTES,
//                only launching Play In-App Review if they say yes
//   3b. Invite  — signed-in users after INVITE_MIN_MINUTES; at most twice, 10+ days apart
//   3c. Feedback — "what should we build next?" after FEEDBACK_MIN_MINUTES; twice max, 21+ days apart
//   4. Premium — after PREMIUM_MIN_MINUTES, 30-min gap since last popup

@Composable
fun HomePopupOrchestrator(
    originalAssetsEnabled: Boolean,
    totalSessionMinutes: Long,
    isGuest: Boolean,
    isPremium: Boolean,
    dailyStreak: Int,
    onEnableAssets: () -> Unit,
    onRateNow: () -> Unit,
    onGoPremium: () -> Unit,
    onOpenDexMode: () -> Unit,
    onOpenInvite: (prefillCode: String?) -> Unit,
    onOpenFeedback: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var assetsShown by remember { mutableStateOf(false) }
    var ratingShown by remember { mutableStateOf(false) }
    var premiumShown by remember { mutableStateOf(false) }
    var lastPopupAtMinutes by remember { mutableLongStateOf(0L) }
    var isReady by remember { mutableStateOf(false) }
    var slowPopupsUnlocked by remember { mutableStateOf(false) }
    var widgetAsks by remember { mutableStateOf(0) }
    var widgetLastAskMs by remember { mutableLongStateOf(0L) }
    var widgetOnHomeScreen by remember { mutableStateOf(true) } // assume yes until checked — never ask by mistake
    val widgetPinSupported = remember { StreakWidgetPinning.isPinSupported(context) }
    var showWidgetManualSteps by remember { mutableStateOf(false) }
    val dexModeStore: DexModeStore = koinInject()
    var dexAnnounced by remember { mutableStateOf(true) }
    val referralRepository: ReferralRepository = koinInject()
    val feedbackRepository: FeedbackRepository = koinInject()
    val pendingReferralCode by referralRepository.pendingInstallCode.collectAsStateWithLifecycle()
    var referralRedeemed by remember { mutableStateOf(true) }
    var inviteAsks by remember { mutableStateOf(0) }
    var inviteLastAskMs by remember { mutableLongStateOf(0L) }
    var feedbackAsks by remember { mutableStateOf(0) }
    var feedbackLastAskMs by remember { mutableLongStateOf(0L) }
    var feedbackSubmitted by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        assetsShown = ScreenStateManager.isAssetsShown(context)
        ratingShown = ScreenStateManager.isRatingShown(context)
        premiumShown = ScreenStateManager.isPremiumShown(context)
        lastPopupAtMinutes = ScreenStateManager.getLastPopupShownAtMinutes(context)
        ScreenStateManager.widgetPromptHistory(context).let { (asks, lastMs) ->
            widgetAsks = asks
            widgetLastAskMs = lastMs
        }
        widgetOnHomeScreen = StreakWidgetPinning.isOnHomeScreen(context)
        dexAnnounced = dexModeStore.isAnnounced || dexModeStore.discovered.value
        referralRedeemed = referralRepository.hasRedeemed
        referralRepository.invitePromptHistory().let { (asks, lastMs) ->
            inviteAsks = asks
            inviteLastAskMs = lastMs
        }
        feedbackRepository.promptHistory().let { (asks, lastMs) ->
            feedbackAsks = asks
            feedbackLastAskMs = lastMs
        }
        feedbackSubmitted = feedbackRepository.hasSubmitted
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
        lastPopupAtMinutes,
        dailyStreak,
        widgetAsks,
        widgetOnHomeScreen,
        dexAnnounced,
        pendingReferralCode,
        referralRedeemed,
        inviteAsks,
        feedbackAsks,
        feedbackSubmitted
    ) {
        if (!isReady) return@LaunchedEffect

        val minutesSinceLastPopup = totalSessionMinutes - lastPopupAtMinutes
        val cooldownPassed = minutesSinceLastPopup >= MIN_GAP_BETWEEN_POPUPS_MINUTES
        val widgetAskDue = widgetAsks == 0 ||
            (widgetAsks < WIDGET_MAX_ASKS && System.currentTimeMillis() - widgetLastAskMs >= WIDGET_REASK_GAP_MS)

        activePopup = when {
            !assetsShown && !originalAssetsEnabled -> HomePopup.Assets

            // A brand-new mode: announced once, without waiting out the popup cooldown.
            !dexAnnounced && slowPopupsUnlocked -> HomePopup.DexMode

            // Came in through a friend's invite link: offer the welcome bonus right away.
            pendingReferralCode != null && !referralRedeemed && !isGuest -> HomePopup.ReferralClaim

            !widgetOnHomeScreen && widgetAskDue &&
                    dailyStreak >= WIDGET_MIN_STREAK &&
                    slowPopupsUnlocked && cooldownPassed -> HomePopup.Widget

            !ratingShown && !isGuest &&
                    slowPopupsUnlocked &&
                    totalSessionMinutes >= RATING_MIN_MINUTES &&
                    cooldownPassed -> HomePopup.Rating

            !isGuest && slowPopupsUnlocked && cooldownPassed &&
                    totalSessionMinutes >= INVITE_MIN_MINUTES &&
                    askDue(inviteAsks, inviteLastAskMs, INVITE_MAX_ASKS, INVITE_REASK_GAP_MS) -> HomePopup.Invite

            !feedbackSubmitted && slowPopupsUnlocked && cooldownPassed &&
                    totalSessionMinutes >= FEEDBACK_MIN_MINUTES &&
                    askDue(feedbackAsks, feedbackLastAskMs, FEEDBACK_MAX_ASKS, FEEDBACK_REASK_GAP_MS) -> HomePopup.Feedback

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

        HomePopup.DexMode -> DexModeAnnouncementDialog(
            onPowerOn = {
                dismissPopup {
                    dexModeStore.markAnnounced()
                    dexAnnounced = true
                }
                onOpenDexMode()
            },
            onLater = {
                dismissPopup {
                    dexModeStore.markAnnounced()
                    dexAnnounced = true
                }
            }
        )

        HomePopup.ReferralClaim -> ReferralClaimDialog(
            code = pendingReferralCode.orEmpty(),
            onClaim = {
                val code = pendingReferralCode
                dismissPopup { referralRedeemed = true }
                onOpenInvite(code)
            },
            onLater = {
                dismissPopup {
                    referralRepository.clearPendingCode()
                    referralRedeemed = true
                }
            }
        )

        HomePopup.Invite -> InvitePromptDialog(
            onInvite = {
                dismissPopup {
                    referralRepository.markInvitePromptShown()
                    inviteAsks += 1
                }
                onOpenInvite(null)
            },
            onLater = {
                dismissPopup {
                    referralRepository.markInvitePromptShown()
                    inviteAsks += 1
                }
            }
        )

        HomePopup.Feedback -> FeedbackPromptDialog(
            onOpen = {
                dismissPopup {
                    feedbackRepository.markPromptShown()
                    feedbackAsks += 1
                }
                onOpenFeedback()
            },
            onLater = {
                dismissPopup {
                    feedbackRepository.markPromptShown()
                    feedbackAsks += 1
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

        HomePopup.Widget -> StreakWidgetPromptDialog(
            streakDays = dailyStreak,
            pinSupported = widgetPinSupported,
            onAdd = {
                dismissPopup {
                    ScreenStateManager.markWidgetPromptShown(context)
                    widgetAsks += 1
                }
                coroutineScope.launch {
                    // If the launcher refuses the system sheet, fall back to telling them how.
                    if (!StreakWidgetPinning.requestPin(context)) showWidgetManualSteps = true
                }
            },
            onDismiss = {
                dismissPopup {
                    ScreenStateManager.markWidgetPromptShown(context)
                    widgetAsks += 1
                }
            }
        )

        HomePopup.None -> {}
    }

    if (showWidgetManualSteps) StreakWidgetManualStepsDialog(onDismiss = { showWidgetManualSteps = false })
}

private fun askDue(asks: Int, lastAskMs: Long, maxAsks: Int, gapMs: Long): Boolean =
    asks == 0 || (asks < maxAsks && System.currentTimeMillis() - lastAskMs >= gapMs)

// ─────────────────────────────────────────────────────────────────────────────
// Force update screen — non-dismissible, blocks the entire app
// Shown when BuildConfig.VERSION_CODE < minVersionCode from Firestore
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun UpdateAvailableDialog(
    latestVersionName: String,
    packageName: String
) {
    val context = LocalContext.current

    // Compulsory — no dismiss button, and back/outside-tap are no-ops. New
    // versions always carry improvements, so there's no "maybe later" here.
    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(24.dp),
        icon = { Icon(Icons.Default.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
        title = {
            Text(
                stringResource(R.string.update_available_title),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                stringResource(R.string.update_available_body, latestVersionName),
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
                Text(stringResource(R.string.update_now), fontWeight = FontWeight.Bold)
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
