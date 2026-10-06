package com.aditya1875.pokeverse.feature.team.presentation.components

import com.aditya1875.pokeverse.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aditya1875.pokeverse.feature.team.data.local.entity.TeamMemberEntity

private const val MIN_TEAM_SIZE_FOR_ANALYSIS_OR_SHARE = 3

@Composable
fun TeamContent(
    onAnalyze: () -> Unit,
    team: List<TeamMemberEntity>,
    navController: NavController,
    onRemove: (TeamMemberEntity) -> Unit,
    accentColor: Color? = null,
    assetsEnabled: Boolean,
    onShare: (() -> Unit)? = null,
    analysisUsesLeft: Int? = null
) {
    val progressColor = accentColor ?: MaterialTheme.colorScheme.primary
    val meetsMinimumForAnalysisOrShare = team.size >= MIN_TEAM_SIZE_FOR_ANALYSIS_OR_SHARE

    Column(modifier = Modifier.fillMaxSize()) {
        if (team.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.Star,
                title = stringResource(R.string.team_empty_title),
                subtitle = stringResource(R.string.team_empty_subtitle),
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            // Progress Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.team_progress),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = stringResource(R.string.team_size_of_six, team.size),
                            color = progressColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = when {
                            !meetsMinimumForAnalysisOrShare ->
                                stringResource(R.string.team_need_more_to_unlock, MIN_TEAM_SIZE_FOR_ANALYSIS_OR_SHARE - team.size)
                            team.size < 6 -> stringResource(R.string.team_need_more_to_complete, 6 - team.size)
                            else -> stringResource(R.string.team_complete)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    VibrantProgressBar(
                        progress = team.size / 6f,
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = progressColor
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAnalyze,
                    enabled = meetsMinimumForAnalysisOrShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (analysisUsesLeft == 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.tertiary,
                        contentColor = if (analysisUsesLeft == 0)
                            MaterialTheme.colorScheme.onError
                        else
                            MaterialTheme.colorScheme.onTertiary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = if (meetsMinimumForAnalysisOrShare) Icons.Default.Info else Icons.Default.Lock,
                        contentDescription = stringResource(R.string.team_analyze_a11y),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.team_analyze),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!meetsMinimumForAnalysisOrShare) {
                            Text(
                                text = stringResource(R.string.team_need_more, MIN_TEAM_SIZE_FOR_ANALYSIS_OR_SHARE - team.size),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Normal
                            )
                        } else if (analysisUsesLeft != null && analysisUsesLeft >= 0) {
                            Text(
                                text = if (analysisUsesLeft == 0) stringResource(R.string.go_premium) else stringResource(R.string.team_uses_left, analysisUsesLeft),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
                if (onShare != null) {
                    Button(
                        onClick = onShare,
                        enabled = meetsMinimumForAnalysisOrShare,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = if (meetsMinimumForAnalysisOrShare) Icons.Default.IosShare else Icons.Default.Lock,
                            contentDescription = stringResource(R.string.team_share_a11y),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_share),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (!meetsMinimumForAnalysisOrShare) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.team_min_size_hint, MIN_TEAM_SIZE_FOR_ANALYSIS_OR_SHARE),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Team List
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(team, key = { index, it -> "${it.name}_$index" }) { _, pokemon ->
                    ImprovedTeamCard(
                        pokemon = pokemon,
                        navController = navController,
                        onRemove = onRemove,
                        assetsEnabled = assetsEnabled
                    )
                }
            }
        }
    }
}