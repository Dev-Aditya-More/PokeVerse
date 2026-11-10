package com.aditya1875.pokeverse.feature.challenges.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.challenges.domain.BallReward
import com.aditya1875.pokeverse.feature.challenges.domain.Challenge
import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeGoal
import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeRules
import com.aditya1875.pokeverse.feature.challenges.domain.Difficulty
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import com.aditya1875.pokeverse.feature.game.wildcatch.presentation.components.BallIcon

private val Gold = Color(0xFFFFB300)

private fun Difficulty.color(): Color = when (this) {
    Difficulty.EASY -> Color(0xFF66BB6A)
    Difficulty.MEDIUM -> Color(0xFF42A5F5)
    Difficulty.HARD -> Color(0xFFAB47BC)
    Difficulty.MILESTONE -> Gold
}

@Composable
private fun tierLabel(tier: CatchTier): String = stringResource(
    when (tier) {
        CatchTier.COMMON -> R.string.wildcatch_tier_common
        CatchTier.UNCOMMON -> R.string.wildcatch_tier_uncommon
        CatchTier.RARE -> R.string.wildcatch_tier_rare
        CatchTier.EPIC -> R.string.wildcatch_tier_epic
        CatchTier.LEGENDARY -> R.string.legendary
    }
)

@Composable
private fun Challenge.title(): String = when (goal) {
    ChallengeGoal.CATCH_ANY -> stringResource(R.string.challenge_catch_any, target)
    ChallengeGoal.CATCH_NEW -> stringResource(R.string.challenge_catch_new, target)
    ChallengeGoal.CATCH_TIER -> stringResource(R.string.challenge_catch_tier, target, tierLabel(minTier))
    ChallengeGoal.CATCH_STREAK -> stringResource(R.string.challenge_streak, target)
    ChallengeGoal.DEX_TOTAL -> stringResource(R.string.challenge_dex_total, target)
    ChallengeGoal.DEX_TIER -> stringResource(R.string.challenge_dex_tier, target, tierLabel(minTier))
}

/** The balls a reward pays, as drawn balls with a count: no emoji, and the same look as the games. */
@Composable
fun RewardChips(reward: BallReward, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (reward.great > 0) RewardChip(BallType.GREAT, reward.great)
        if (reward.ultra > 0) RewardChip(BallType.ULTRA, reward.ultra)
    }
}

@Composable
private fun RewardChip(ball: BallType, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BallIcon(ball, 18.dp)
        Spacer(Modifier.width(3.dp))
        Text("x$count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/** The round button on Home that opens the challenges. A gold dot with the count shows when a reward is waiting. */
@Composable
fun ChallengesFab(waiting: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        FloatingActionButton(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
            modifier = Modifier.size(52.dp),
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = stringResource(R.string.challenges_title))
        }
        if (waiting > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Gold),
                contentAlignment = Alignment.Center,
            ) {
                Text("$waiting", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3A2A00))
            }
        }
    }
}

/**
 * Challenges as a bottom sheet that drags up to full screen: today's three, the all-done bonus, then the
 * long-term Pokédex goals. A ready reward is claimed here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesSheet(
    ui: ChallengesUi,
    onClaim: (ChallengeRow) -> Unit,
    onClaimSweep: () -> Unit,
    onClaimAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BallIcon(BallType.ULTRA, 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.challenges_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.challenges_today, ui.dailyDone, ui.daily.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (ui.waiting > 0) {
                        Button(
                            onClick = onClaimAll,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF3A2A00)),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(38.dp),
                        ) { Text(stringResource(R.string.challenges_claim_all, ui.waiting), fontWeight = FontWeight.Bold) }
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.challenges_daily)) }
            items(ui.daily, key = { it.challenge.id }) { row -> ChallengePanel(row, onClaim = { onClaim(row) }) }
            if (ui.sweepReady || ui.sweepClaimed) {
                item { SweepRow(claimed = ui.sweepClaimed, onClaim = onClaimSweep) }
            }

            item {
                Column {
                    SectionLabel(stringResource(R.string.challenges_goals))
                    Text(
                        stringResource(R.string.challenges_goals_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(ui.milestones, key = { it.challenge.id }) { row -> ChallengePanel(row, onClaim = { onClaim(row) }) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

/** One challenge as its own raised panel, so the list reads as separate goals rather than a table. */
@Composable
private fun ChallengePanel(row: ChallengeRow, onClaim: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (row.claimed) 0.3f else 0.55f),
        border = if (row.claimable) BorderStroke(1.5.dp, Gold.copy(alpha = 0.8f)) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) { ChallengeRowView(row, onClaim) }
    }
}

@Composable
private fun ChallengeRowView(row: ChallengeRow, onClaim: () -> Unit) {
    val c = row.challenge
    val color = c.difficulty.color()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                c.title(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { if (c.target == 0) 0f else row.progress.toFloat() / c.target },
                    color = color,
                    trackColor = color.copy(alpha = 0.18f),
                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${row.progress}/${c.target}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        when {
            row.claimed -> Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.challenges_claimed), tint = color, modifier = Modifier.size(22.dp))
            row.claimable -> TextButton(onClick = onClaim) {
                Text(stringResource(R.string.challenges_claim), fontWeight = FontWeight.Bold, color = Gold)
            }
            else -> RewardChips(c.reward)
        }
    }
}

@Composable
private fun SweepRow(claimed: Boolean, onClaim: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Gold.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.challenges_sweep), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.challenges_sweep_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (claimed) {
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.challenges_claimed), tint = Gold, modifier = Modifier.size(22.dp))
        } else {
            RewardChips(ChallengeRules.SWEEP_BONUS)
            TextButton(onClick = onClaim) { Text(stringResource(R.string.challenges_claim), fontWeight = FontWeight.Bold, color = Gold) }
        }
    }
}
