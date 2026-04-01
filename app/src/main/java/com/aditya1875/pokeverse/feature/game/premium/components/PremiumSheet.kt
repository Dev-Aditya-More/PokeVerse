package com.aditya1875.pokeverse.feature.game.premium.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.feature.game.core.data.billing.PremiumPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumBottomSheet(
    onDismiss: () -> Unit,
    onSubscribeMonthly: () -> Unit,
    onSubscribeYearly: () -> Unit,
    onSubscribeLifetime: () -> Unit,
    monthlyPrice: String,
    yearlyPrice: String,
    lifetimePrice: String,
    isSubscribeEnabled: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPlan by remember { mutableStateOf(PremiumPlan.YEARLY) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.3f),
                                Color(0xFFFF8C00).copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Dexverse Premium",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Unlock the full Dexverse experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            val features = listOf(
                Triple(Icons.Default.EmojiEvents, "Hard Mode", "You get to play the ultimate challenge"),
                Triple(Icons.Default.CatchingPokemon, "Item Exploration", "Explore all the items in the game"),
                Triple(Icons.Default.Palette, "Exclusive themes", "Get all the themes and features"),
            )

            features.forEach { (icon, title, subtitle) ->
                PremiumFeatureRow(
                    icon = icon,
                    title = title,
                    subtitle = subtitle
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPlan == PremiumPlan.MONTHLY,
                    onClick = { selectedPlan = PremiumPlan.MONTHLY },
                    label = { Text("Monthly 🚀") },
                    border = BorderStroke(
                        1.dp,
                        if (selectedPlan == PremiumPlan.MONTHLY)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant
                    )
                )
                FilterChip(
                    selected = selectedPlan == PremiumPlan.YEARLY,
                    onClick = { selectedPlan = PremiumPlan.YEARLY },
                    label = {
                        Row {
                            Text("Yearly • Save 33%")
                            Spacer(Modifier.width(6.dp))
                            Text("BEST", color = Color(0xFFFF9800), fontSize = 10.sp)
                        }
                    },
                    border = BorderStroke(
                        1.dp,
                        if (selectedPlan == PremiumPlan.YEARLY)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant
                    )
                )
                FilterChip(
                    selected = selectedPlan == PremiumPlan.LIFETIME,
                    onClick = { selectedPlan = PremiumPlan.LIFETIME },
                    label = { Text("Lifetime • Save 40% 🔥") },
                    border = BorderStroke(
                        1.dp,
                        if (selectedPlan == PremiumPlan.LIFETIME)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                val price = when (selectedPlan) {
                    PremiumPlan.MONTHLY -> monthlyPrice
                    PremiumPlan.YEARLY -> yearlyPrice
                    PremiumPlan.LIFETIME -> lifetimePrice
                }

                Text(
                    text = price.ifEmpty { "Loading..." },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.width(4.dp))

                val suffix = when (selectedPlan) {
                    PremiumPlan.MONTHLY -> "/ month"
                    PremiumPlan.YEARLY -> "/ year"
                    PremiumPlan.LIFETIME -> "one-time"
                }

                Text(text = suffix)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    when (selectedPlan) {
                        PremiumPlan.MONTHLY -> {
                            onSubscribeMonthly()
                        }
                        PremiumPlan.YEARLY -> {
                            onSubscribeYearly()
                        }
                        PremiumPlan.LIFETIME -> {
                            onSubscribeLifetime()
                        }
                    }
                },
                enabled = isSubscribeEnabled,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Unlock Premium 🚀")
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onDismiss) {
                Text(
                    text = "Maybe later",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "🔒 Payment processed securely by Google Play.\nYour details are never shared with us.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }

        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PremiumFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFFD700).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }

        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
    }
}
