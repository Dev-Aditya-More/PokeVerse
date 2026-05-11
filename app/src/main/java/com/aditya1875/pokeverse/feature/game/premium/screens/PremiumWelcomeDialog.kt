package com.aditya1875.pokeverse.feature.game.premium.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

private val Gold = Color(0xFFFFD700)
private val GoldDark = Color(0xFFFF8C00)
private val BgTop = Color(0xFF1A1035)
private val BgBottom = Color(0xFF0D0D1A)

private val perks = listOf(
    "Hard mode in all games",
    "Full leaderboard access",
    "Exclusive premium themes",
    "Priority access to new features & games"
)

@Preview(showBackground = true)
@Composable
fun PremiumWelcomeDialog(onDismiss: () -> Unit = {}) {
    val infiniteTransition = rememberInfiniteTransition(label = "premium")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle"
    )

    val perkVisible = remember { mutableStateListOf(false, false, false, false) }
    LaunchedEffect(Unit) {
        perks.indices.forEach { i ->
            delay(350L + i * 130L)
            perkVisible[i] = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(colors = listOf(BgTop, BgBottom)))
                .padding(28.dp)
        ) {
            // Sparkle accents
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Gold.copy(alpha = sparkleAlpha * 0.6f),
                modifier = Modifier.size(18.dp).align(Alignment.TopStart).offset(x = 10.dp, y = 10.dp)
            )
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Gold.copy(alpha = (1f - sparkleAlpha) * 0.6f),
                modifier = Modifier.size(12.dp).align(Alignment.TopEnd).offset(x = (-14).dp, y = 18.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(8.dp))

                // Crown with pulsing glow rings
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Gold.copy(alpha = glowAlpha * 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier
                            .size(70.dp)
                            .offset {
                                IntOffset(
                                    floatOffset.dp.roundToPx(),
                                    floatOffset.dp.roundToPx()
                                )
                            }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "DEXVERSE PREMIUM",
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    color = Gold,
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "You're in. Welcome aboard!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Thanks for supporting the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(22.dp))

                HorizontalDivider(color = Gold.copy(alpha = 0.18f), thickness = 1.dp)

                Spacer(Modifier.height(18.dp))

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    perks.forEachIndexed { i, perk ->
                        AnimatedVisibility(
                            visible = perkVisible.getOrElse(i) { false },
                            enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -50 }
                        ) {
                            FancyPerkRow(perk)
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(colors = listOf(Gold, GoldDark))
                        )
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Let's Go!  🚀",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BgTop
                    )
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun FancyPerkRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}
