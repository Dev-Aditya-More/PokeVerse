package com.aditya1875.pokeverse.presentation.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// POPUP PRIORITY MANAGER
// Only one popup can show at a time. Priority order:
//   1. Assets onboarding (first launch — highest priority)
//   2. Rating prompt (after 20 min cumulative use)
//   3. Premium upsell (after rating is seen or dismissed, user not premium)
// ─────────────────────────────────────────────────────────────────────────────

enum class HomePopup { None, Assets, Rating, Premium }

@Composable
fun HomePopupOrchestrator(
    assetsBannerSeen: Boolean,
    originalAssetsEnabled: Boolean,
    ratingPromptSeen: Boolean,
    premiumPromptShown: Boolean,
    totalSessionMinutes: Long,
    isGuest: Boolean,
    isPremium: Boolean,
    onEnableAssets: () -> Unit,
    onDismissAssets: () -> Unit,
    onDismissRating: () -> Unit,
    onRateNow: () -> Unit,
    onDismissPremium: () -> Unit,
    onGoPremium: () -> Unit,
) {
    val activePopup = remember(
        assetsBannerSeen, originalAssetsEnabled, ratingPromptSeen,
        premiumPromptShown, totalSessionMinutes, isGuest, isPremium
    ) {
        when {
            // 1. Assets dialog — show on very first home screen entry if not seen
            !assetsBannerSeen && !originalAssetsEnabled -> HomePopup.Assets

            // 2. Rating — after 20 cumulative minutes, hasn't been seen yet
            !ratingPromptSeen && totalSessionMinutes >= 20 -> HomePopup.Rating

            // 3. Premium — only after rating was handled, user is logged in, not premium
            ratingPromptSeen && !premiumPromptShown && !isPremium && !isGuest -> HomePopup.Premium

            else -> HomePopup.None
        }
    }

    val showBanner = !assetsBannerSeen && !originalAssetsEnabled
    when (activePopup) {
        HomePopup.Assets -> AssetsOnboardingBanner(
            visible = showBanner,
            onEnable = onEnableAssets,
            onDismiss = onDismissAssets
        )
        HomePopup.Rating -> RatingPromptDialog(
            onRateNow = {
                onRateNow()
                onDismissRating()
            },
            onMaybeLater = onDismissRating,
            onDontAsk = onDismissRating
        )
        HomePopup.Premium -> PremiumUpsellDialog(
            onGoPremium = onGoPremium,
            onDismiss = onDismissPremium
        )
        HomePopup.None -> {}
    }
}

@Composable
fun RatingPromptDialog(
    onRateNow: () -> Unit,
    onMaybeLater: () -> Unit,
    onDontAsk: () -> Unit,
) {
    var showDontAsk by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onMaybeLater,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("⭐", fontSize = 36.sp) },
        title = {
            Text(
                "Enjoying Dexverse?",
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) {
                        Text("⭐", fontSize = 24.sp)
                    }
                }
                Text(
                    "A quick rating helps us reach more Pokémon fans and keeps the app improving.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRateNow,
                shape = RoundedCornerShape(12.dp)
            ) {
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

@Preview(showBackground = true)
@Composable
fun PremiumUpsellDialog(
    onGoPremium: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val features = listOf(
        "🎯" to "Hard mode in all 4 games",
        "✨" to "Full Dexverse Experience",
        "🔮" to "Exclusive themes & badges",
        "🚀" to "Support Dexverse's growth",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("👑", fontSize = 36.sp) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Pokeverse Premium",
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
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
                onClick = onGoPremium,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black
                )
            ) {
                Text("Go Premium 👑", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}