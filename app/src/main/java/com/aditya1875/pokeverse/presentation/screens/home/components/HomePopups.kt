package com.aditya1875.pokeverse.presentation.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val activePopup = when {
        !assetsBannerSeen && !originalAssetsEnabled -> HomePopup.Assets
        !ratingPromptSeen && totalSessionMinutes >= 20 -> HomePopup.Rating
        ratingPromptSeen && !premiumPromptShown && !isPremium && !isGuest -> HomePopup.Premium
        else -> HomePopup.None
    }

    when (activePopup) {
        HomePopup.Assets -> AssetsOnboardingDialog(
            onEnable = { onEnableAssets(); onDismissAssets() },
            onDismiss = onDismissAssets
        )

        HomePopup.Rating -> RatingPromptDialog(
            onRateNow = { onRateNow(); onDismissRating() },
            onMaybeLater = onDismissRating
        )

        HomePopup.Premium -> PremiumUpsellDialog(
            onGoPremium = onGoPremium,
            onDismiss = onDismissPremium
        )

        HomePopup.None -> {}
    }
}

@Composable
fun AssetsOnboardingDialog(onEnable: () -> Unit = {}, onDismiss: () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("🎨", fontSize = 36.sp) },
        title = {
            Text(
                "Original Visuals Available",
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
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
            Button(
                onClick = onEnable,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("I Understand, Enable", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not Now") } }
    )
}

@Composable
fun RatingPromptDialog(onRateNow: () -> Unit = {}, onMaybeLater: () -> Unit = {}) {
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
                        Text(
                            "⭐",
                            fontSize = 22.sp
                        )
                    }
                }
                Text(
                    "A quick rating helps us reach more Pokémon fans and keeps the app improving.",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRateNow,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Rate Now ⭐", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = onMaybeLater) { Text("Maybe Later") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Preview
@Composable
fun PremiumUpsellDialog(onGoPremium: () -> Unit = {}, onDismiss: () -> Unit = {}) {
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
                    "Dexverse Premium",
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Unlock the full experience", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
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
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black
                )
            ) { Text("Go Premium", fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Maybe Later") } }
    )
}