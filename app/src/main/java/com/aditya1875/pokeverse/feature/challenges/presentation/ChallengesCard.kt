package com.aditya1875.pokeverse.feature.challenges.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.graphicsLayer
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
import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeCategory
import com.aditya1875.pokeverse.feature.progression.presentation.ChallengeRewardPills
import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeGoal
import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeRules
import com.aditya1875.pokeverse.feature.challenges.domain.Difficulty
import com.aditya1875.pokeverse.feature.challenges.domain.Region
import com.aditya1875.pokeverse.feature.core.ui.components.CoinIcon
import com.aditya1875.pokeverse.feature.core.ui.components.ShardGem
import com.aditya1875.pokeverse.feature.progression.domain.Cosmetics
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
    ChallengeGoal.CATCH_TOTAL -> stringResource(R.string.challenge_catch_total, target)
    ChallengeGoal.STREAK_EVER -> stringResource(R.string.challenge_streak_ever, target)
    ChallengeGoal.EVOLVED -> stringResource(R.string.challenge_evolved, target)
    ChallengeGoal.DEX_REGION -> stringResource(R.string.challenge_region, regionName(region))
}

@Composable
private fun regionName(region: Region?): String = stringResource(
    when (region) {
        Region.KANTO, null -> R.string.region_kanto
        Region.JOHTO -> R.string.region_johto
        Region.HOENN -> R.string.region_hoenn
        Region.SINNOH -> R.string.region_sinnoh
        Region.UNOVA -> R.string.region_unova
        Region.KALOS -> R.string.region_kalos
        Region.ALOLA -> R.string.region_alola
    }
)

/**
 * The round button on Home that opens the challenges, in the app's primary colours like the rest of the chrome.
 * A gold dot with the count shows when a reward is waiting, and a soft ring pulses around it to say so.
 */
@Composable
fun ChallengesFab(waiting: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (waiting > 0) PulseRing()
        FloatingActionButton(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
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

/** A soft ring that swells and fades behind the button while there is something to claim. */
@Composable
private fun PulseRing() {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "challenge_ring")
    val ring by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "ring",
    )
    Box(
        Modifier
            .size(52.dp)
            .graphicsLayer {
                val s = 1f + ring * 0.6f
                scaleX = s; scaleY = s
                alpha = (1f - ring) * 0.45f
            }
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
    )
}

/**
 * Challenges as a bottom sheet that drags up to full screen, in sections: today's three (with the all-done
 * bonus), then the long-term goals grouped by what they ask for. Every row has the same layout: what to do, how
 * far along, and what it pays. A ready reward is claimed here.
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
    // One chip per section so a single section is on screen at a time instead of one long scroll: Today first,
    // then each kind of long-term goal (only the ones that have something to show).
    val sections: List<ChallengeCategory?> = listOf<ChallengeCategory?>(null) + ChallengeCategory.entries.filter { c ->
        ui.milestones.any { it.challenge.category == c } || ui.milestoneTotals[c] != null
    }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    // A section that has since disappeared (all claimed and gone) falls back to Today.
    val selected: ChallengeCategory? = sections.firstOrNull { it != null && it.name == selectedName }
    fun waitingIn(section: ChallengeCategory?): Int =
        if (section == null) ui.daily.count { it.claimable } + (if (ui.sweepReady) 1 else 0)
        else ui.milestones.count { it.challenge.category == section && it.claimable }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.challenges_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.challenges_today, ui.dailyDone, ui.daily.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (ui.waiting > 0) {
                    Button(
                        onClick = onClaimAll,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF3A2A00)),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(40.dp),
                    ) { Text(stringResource(R.string.challenges_claim_all, ui.waiting), fontWeight = FontWeight.Bold) }
                }
            }

            // The switcher. A gold dot on a chip means a reward in that section is ready to claim.
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sections.forEach { section ->
                    val ready = waitingIn(section)
                    FilterChip(
                        selected = section == selected,
                        onClick = { selectedName = section?.name },
                        label = {
                            Text(
                                if (section == null) stringResource(R.string.challenges_daily) else stringResource(section.titleRes()),
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        trailingIcon = if (ready > 0) {
                            { Box(Modifier.size(8.dp).clip(CircleShape).background(Gold)) }
                        } else null,
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                if (selected == null) {
                    // ── Today ──
                    items(ui.daily, key = { it.challenge.id }) { row -> ChallengePanel(row, showDifficulty = true, onClaim = { onClaim(row) }) }
                    if (ui.sweepReady || ui.sweepClaimed) {
                        item(key = "sweep") { SweepPanel(claimed = ui.sweepClaimed, onClaim = onClaimSweep) }
                    }
                } else {
                    // ── One kind of long-term goal ──
                    val rows = ui.milestones.filter { it.challenge.category == selected }
                    val totals = ui.milestoneTotals[selected]
                    item(key = "h_${selected.name}") {
                        CategoryHeader(
                            title = stringResource(selected.titleRes()),
                            done = totals?.first ?: 0,
                            total = totals?.second ?: 0,
                        )
                    }
                    if (rows.isEmpty()) {
                        item(key = "done_${selected.name}") {
                            Text(
                                stringResource(R.string.challenges_category_complete),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                    items(rows, key = { it.challenge.id }) { row -> ChallengePanel(row, showDifficulty = false, onClaim = { onClaim(row) }) }
                }
            }
        }
    }
}

private fun ChallengeCategory.titleRes(): Int = when (this) {
    ChallengeCategory.POKEDEX -> R.string.challenge_cat_pokedex
    ChallengeCategory.COLLECTION -> R.string.challenge_cat_collection
    ChallengeCategory.REGIONS -> R.string.challenge_cat_regions
    ChallengeCategory.CATCHING -> R.string.challenge_cat_catching
    ChallengeCategory.EVOLUTION -> R.string.challenge_cat_evolution
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null, topSpace: androidx.compose.ui.unit.Dp = 6.dp) {
    Column(Modifier.padding(top = topSpace)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A sub-heading inside the long-term section: the kind of goal, and how many of its goals are done. */
@Composable
private fun CategoryHeader(title: String, done: Int, total: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (total > 0) {
            Text(
                stringResource(R.string.challenges_category_done, done, total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Difficulty.label(): String = stringResource(
    when (this) {
        Difficulty.EASY -> R.string.challenge_diff_easy
        Difficulty.MEDIUM -> R.string.challenge_diff_medium
        Difficulty.HARD -> R.string.challenge_diff_hard
        Difficulty.MILESTONE -> R.string.challenges_goals
    }
)

/**
 * One challenge as its own panel, always laid out the same way: [difficulty tag], the task, a progress bar with
 * its count, then the reward pills. A claimable one gets a gold outline and a Claim button; a claimed one dims and
 * shows a check instead of its reward.
 */
@Composable
private fun ChallengePanel(row: ChallengeRow, showDifficulty: Boolean, onClaim: () -> Unit) {
    val c = row.challenge
    val color = c.difficulty.color()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (row.claimed) 0.3f else 0.55f),
        border = if (row.claimable) BorderStroke(1.5.dp, Gold.copy(alpha = 0.85f)) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (showDifficulty) {
                        Text(
                            c.difficulty.label().uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = color,
                        )
                    }
                    Text(c.title(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                when {
                    row.claimed -> Icon(
                        Icons.Filled.Check, contentDescription = stringResource(R.string.challenges_claimed),
                        tint = color, modifier = Modifier.size(24.dp),
                    )
                    row.claimable -> Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF3A2A00)),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(36.dp),
                    ) { Text(stringResource(R.string.challenges_claim), fontWeight = FontWeight.Bold) }
                }
            }
            if (!row.claimed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { if (c.target == 0) 0f else row.progress.toFloat() / c.target },
                        color = color,
                        trackColor = color.copy(alpha = 0.18f),
                        modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${row.progress} / ${c.target}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ChallengeRewardPills(c.reward)
            }
        }
    }
}

@Composable
private fun SweepPanel(claimed: Boolean, onClaim: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Gold.copy(alpha = if (claimed) 0.08f else 0.14f),
        border = if (!claimed) BorderStroke(1.5.dp, Gold.copy(alpha = 0.85f)) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.challenges_sweep), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.challenges_sweep_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (claimed) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.challenges_claimed), tint = Gold, modifier = Modifier.size(24.dp))
                } else {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF3A2A00)),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(36.dp),
                    ) { Text(stringResource(R.string.challenges_claim), fontWeight = FontWeight.Bold) }
                }
            }
            if (!claimed) ChallengeRewardPills(ChallengeRules.SWEEP_BONUS)
        }
    }
}
