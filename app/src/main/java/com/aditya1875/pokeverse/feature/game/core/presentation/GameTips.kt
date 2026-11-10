package com.aditya1875.pokeverse.feature.game.core.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay

/**
 * Short, useful hints shown on the quiet screens between rounds and games (loading, results, waiting
 * for a match). Each game has its own list; the lists are plain string resources so they translate.
 */
object GameTips {

    private val WILD_CATCH_ALL: List<Int> = listOf(
        R.string.tip_catch_categories,
        R.string.tip_catch_timing,
        R.string.tip_catch_dart,
        R.string.tip_catch_streak,
        R.string.tip_catch_run,
        R.string.tip_catch_repeat,
        R.string.tip_catch_lure,
        R.string.tip_catch_evolve,
    )

    private val BATTLE_ALL: List<Int> = listOf(
        R.string.tip_battle_special,
        R.string.tip_battle_guards,
        R.string.tip_battle_types,
        R.string.tip_battle_speed,
        R.string.tip_battle_switch,
        R.string.tip_battle_evolve,
    )

    /**
     * Wild Catch tips. What would help THIS player most goes first: someone with no Great or Ultra Balls
     * hears about the shop before anything else, and someone holding a Master Ball is reminded to save it.
     */
    fun wildCatch(hasGoodBalls: Boolean, hasMasterBall: Boolean): List<Int> = buildList {
        if (!hasGoodBalls) add(R.string.tip_catch_balls)
        if (hasMasterBall) add(R.string.tip_catch_master)
        addAll(WILD_CATCH_ALL)
        if (hasGoodBalls) add(R.string.tip_catch_balls)
        if (!hasMasterBall) add(R.string.tip_catch_master)
    }

    /** Team Battle tips. [online] adds the ones about playing a real trainer. */
    fun battle(online: Boolean): List<Int> = buildList {
        addAll(BATTLE_ALL)
        if (online) {
            add(0, R.string.tip_battle_clock)
            add(R.string.tip_battle_online_rewards)
        }
    }
}

/**
 * One tip at a time on a soft card, rotating every few seconds. The first tip in [tips] is shown first, so
 * the caller decides what matters most; the rest follow in order.
 */
@Composable
fun TipCard(
    tips: List<Int>,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFFFFC24B),
    textColor: Color = Color.White,
    intervalMs: Long = 7_000L,
) {
    if (tips.isEmpty()) return
    var index by remember(tips) { mutableIntStateOf(0) }
    LaunchedEffect(tips) {
        while (tips.size > 1) {
            delay(intervalMs)
            index = (index + 1) % tips.size
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.tip_label).uppercase(),
                color = accent, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.5.sp,
            )
            AnimatedContent(
                targetState = tips[index.coerceIn(0, tips.lastIndex)],
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tip",
                modifier = Modifier.heightIn(min = 40.dp),
            ) { res ->
                Text(
                    stringResource(res),
                    color = textColor.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
