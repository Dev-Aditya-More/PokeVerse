package com.aditya1875.pokeverse.feature.analysis.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.aditya1875.pokeverse.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.BLUE
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.CARD
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.GREEN
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.PURPLE
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.RED

@Composable
fun QuickStatsRow(analysis: TeamAnalysis, teamSize: Int) {
    val covered = analysis.offensiveCoverage.values.count { it > 0 }
    val weak = analysis.defensiveWeaknesses.size
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CARD)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)), RoundedCornerShape(20.dp))
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniStat(Modifier.weight(1f), covered, "/18", stringResource(R.string.an_stat_coverage), BLUE)
        StatDivider()
        MiniStat(Modifier.weight(1f), analysis.typeDiversity.uniqueTypes, "", stringResource(R.string.an_stat_types), PURPLE)
        StatDivider()
        MiniStat(Modifier.weight(1f), weak, "", stringResource(R.string.an_stat_weak), if (weak <= 4) GREEN else RED)
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.width(1.dp).fillMaxHeight(0.5f).background(Color.White.copy(alpha = 0.08f)))
}

@Composable
private fun MiniStat(modifier: Modifier, value: Int, suffix: String, label: String, color: Color) {
    val shown = com.aditya1875.pokeverse.utils.rememberCountUp("analysis_stat:$label", value, 1000)
    Column(
        modifier.padding(vertical = 18.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "$shown$suffix",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Light,
            color = color,
        )
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 1.4.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

