package com.aditya1875.pokeverse.feature.progression.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.challenges.domain.BallReward
import com.aditya1875.pokeverse.feature.core.ui.components.CoinIcon
import com.aditya1875.pokeverse.feature.core.ui.components.ShardGem
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType
import com.aditya1875.pokeverse.feature.game.wildcatch.presentation.components.BallIcon
import com.aditya1875.pokeverse.feature.progression.domain.CosmeticKind
import com.aditya1875.pokeverse.feature.progression.domain.Cosmetics
import com.aditya1875.pokeverse.feature.progression.domain.LevelReward
import com.aditya1875.pokeverse.feature.progression.domain.ShopItems

/**
 * One reward as a small pill: a picture, then what it is and how many. Challenges and the Level Path both list
 * their rewards as these, so a reward looks the same wherever it is shown. No emoji: the pictures are drawn
 * balls, the real item sprites, the coin and the shard gem.
 */
@Composable
fun RewardPill(label: String, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .padding(start = 6.dp, end = 11.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun BallPill(ball: BallType, count: Int) {
    val name = stringResource(if (ball == BallType.GREAT) R.string.item_great_ball else R.string.item_ultra_ball)
    RewardPill("$name x$count") { BallIcon(ball, 18.dp) }
}

@Composable
private fun CoinPill(amount: Int) = RewardPill(stringResource(R.string.challenge_reward_coins, amount)) { CoinIcon(18.dp) }

@Composable
private fun CosmeticPill(id: String) {
    val c = Cosmetics.find(id) ?: return
    RewardPill(stringResource(c.name)) {
        Icon(
            if (c.kind == CosmeticKind.TITLE) Icons.Default.Badge else Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** What a challenge pays, in a fixed order: balls, coins, shards, then the cosmetic. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChallengeRewardPills(reward: BallReward, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (reward.great > 0) BallPill(BallType.GREAT, reward.great)
        if (reward.ultra > 0) BallPill(BallType.ULTRA, reward.ultra)
        if (reward.coins > 0) CoinPill(reward.coins)
        if (reward.shards > 0) RewardPill(stringResource(R.string.challenge_reward_shards, reward.shards)) { ShardGem(16.dp, Color(0xFF4DD0E1)) }
        reward.cosmetic?.let { CosmeticPill(it) }
    }
}

/** What reaching a level pays, in the same order as a challenge reward, then items, then any cosmetics. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LevelRewardPills(reward: LevelReward, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (reward.great > 0) BallPill(BallType.GREAT, reward.great)
        if (reward.ultra > 0) BallPill(BallType.ULTRA, reward.ultra)
        CoinPill(reward.coins)
        if (reward.lures > 0) {
            ShopItems.find(ShopItems.ID_LURE)?.let { lure ->
                RewardPill("${stringResource(lure.name)} x${reward.lures}") { ItemIcon(lure, 20.dp) }
            }
        }
        reward.stone?.let { id ->
            ShopItems.find(id)?.let { stone -> RewardPill(stringResource(stone.name)) { ItemIcon(stone, 20.dp) } }
        }
        if (reward.freezes > 0) {
            RewardPill("${stringResource(R.string.rewards_freeze_title)} x${reward.freezes}") {
                Icon(Icons.Default.AcUnit, contentDescription = null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(18.dp))
            }
        }
        reward.cosmeticIds.forEach { CosmeticPill(it) }
    }
}
