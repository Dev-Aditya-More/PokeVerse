package com.aditya1875.pokeverse.feature.progression.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.progression.domain.CoinEconomy
import com.aditya1875.pokeverse.feature.progression.domain.WeeklyPrizes

private val CoinGold = Color(0xFFFFC24B)

/**
 * "How do I earn Dex Coins?" Every number comes from [CoinEconomy], so the guide can't drift
 * from what the game actually pays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinGuideSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.guide_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.guide_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            GuideSection(stringResource(R.string.guide_section_daily))
            GuideRow(Icons.Default.CalendarToday, stringResource(R.string.guide_row_login), CoinEconomy.DAILY_LOGIN)
            GuideRow(Icons.Default.SportsEsports, stringResource(R.string.guide_row_first_game), CoinEconomy.FIRST_GAME_OF_DAY)
            GuideRow(Icons.Default.Search, stringResource(R.string.guide_row_explore), CoinEconomy.FIRST_EXPLORATION)
            GuideRow(Icons.Default.Lightbulb, stringResource(R.string.guide_row_fact), CoinEconomy.DAILY_FACT)
            GuideRow(Icons.Default.Extension, stringResource(R.string.guide_row_dex, CoinEconomy.DEX_DAILY_TRIED), CoinEconomy.DEX_DAILY_SOLVED)
            GuideRow(Icons.Default.Flag, stringResource(R.string.guide_row_games, CoinEconomy.COMPLETIONS_PER_DAY), CoinEconomy.COMPLETION)

            GuideSection(stringResource(R.string.guide_section_streak))
            CoinEconomy.STREAK_MILESTONES.forEach { (days, coins) ->
                GuideRow(Icons.Default.LocalFireDepartment, stringResource(R.string.guide_streak_days, days), coins)
            }

            GuideSection(stringResource(R.string.guide_section_weekly))
            GuideRow(Icons.Default.EmojiEvents, stringResource(R.string.guide_weekly_1), WeeklyPrizes.FIRST)
            GuideRow(Icons.Default.EmojiEvents, stringResource(R.string.guide_weekly_2), WeeklyPrizes.SECOND)
            GuideRow(Icons.Default.EmojiEvents, stringResource(R.string.guide_weekly_3), WeeklyPrizes.THIRD)
            GuideRow(Icons.Default.Star, stringResource(R.string.guide_weekly_top10), WeeklyPrizes.TOP_10)
            GuideRow(Icons.Default.SportsEsports, stringResource(R.string.guide_weekly_played), WeeklyPrizes.PLAYED)
            Text(
                stringResource(R.string.guide_weekly_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            GuideSection(stringResource(R.string.guide_section_levels))
            Text(
                stringResource(R.string.guide_levels_body),
                style = MaterialTheme.typography.bodyMedium
            )

            GuideSection(stringResource(R.string.guide_section_tips))
            Text("• " + stringResource(R.string.guide_tip_cap, CoinEconomy.DAILY_MAX), style = MaterialTheme.typography.bodyMedium)
            Text("• " + stringResource(R.string.guide_tip_freeze, CoinEconomy.FREEZE_PRICE), style = MaterialTheme.typography.bodyMedium)
            Text("• " + stringResource(R.string.guide_tip_guest), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(6.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.guide_got_it))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GuideSection(title: String) {
    Text(
        title,
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun GuideRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, coins: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = CoinGold, modifier = Modifier.size(22.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(R.string.guide_amount, coins),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CoinGold
        )
    }
}
