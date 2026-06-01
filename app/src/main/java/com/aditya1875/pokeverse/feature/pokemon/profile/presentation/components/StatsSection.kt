package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile

@Composable
fun StatsSection(profile: UserProfile) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedStatCard(
            modifier = Modifier.weight(1f),
            label = "Total XP",
            value = profile.totalXp.toString(),
            icon = Icons.Default.Star,
            iconTint = Color(0xFFFFD700)
        )
        AnimatedStatCard(
            modifier = Modifier.weight(1f),
            label = "Games",
            value = profile.gamesPlayed.toString(),
            icon = Icons.Default.SportsEsports,
            iconTint = MaterialTheme.colorScheme.primary
        )
        AnimatedStatCard(
            modifier = Modifier.weight(1f),
            label = "Streak",
            value = "${profile.dailyStreak}d",
            icon = Icons.Default.LocalFireDepartment,
            iconTint = Color(0xFFFF6D00)
        )
    }
}

@Composable
fun GameStatsSection(profile: UserProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GameStatRow(
            icon = Icons.Default.FlashOn,
            title = "Who Wins?",
            bestScore = profile.bestDuelScore,
            accentColor = Color(0xFFFF9800)
        )
        GameStatRow(
            icon = Icons.Default.SportsEsports,
            title = "Type Rush",
            bestScore = profile.bestTypeRushScore,
            accentColor = Color(0xFF3F51B5)
        )
        GameStatRow(
            icon = Icons.Default.Quiz,
            title = "Do you know it?",
            bestScore = profile.bestQuizScore,
            accentColor = Color(0xFF2196F3)
        )

        GameStatRow(
            icon = Icons.Default.GridView,
            title = "Match 'Em All",
            bestScore = profile.bestMatchScore,
            accentColor = Color(0xFF4CAF50)
        )

        GameStatRow(
            icon = Icons.Default.Visibility,
            title = "Who's That Monster?",
            bestScore = profile.bestGuessScore,
            accentColor = Color(0xFF9C27B0)
        )
    }
}
@Composable
fun GameStatRow(
    icon: ImageVector,
    title: String,
    bestScore: Int,
    accentColor: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Best Score",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (bestScore > 0) bestScore.toString() else "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
                if (bestScore > 0) {
                    Text(
                        text = "pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = iconTint.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )

            // Animated value — slides up when it increments
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    slideInVertically { it } togetherWith slideOutVertically { -it }
                },
                label = "statValue"
            ) { v ->
                Text(
                    text = v,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = iconTint.copy(alpha = 0.75f)
            )
        }
    }
}