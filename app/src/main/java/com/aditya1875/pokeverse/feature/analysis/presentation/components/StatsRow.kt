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
import androidx.compose.foundation.layout.Arrangement
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStatChip(Modifier.weight(1f), "⚔️", covered, "/18", stringResource(R.string.an_stat_coverage), BLUE, 0)
        MiniStatChip(Modifier.weight(1f), "🎨", analysis.typeDiversity.uniqueTypes, "", stringResource(R.string.an_stat_types), PURPLE, 1)
        MiniStatChip(
            Modifier.weight(1f), "🛡️", weak, "", stringResource(R.string.an_stat_weak),
            if (weak <= 4) GREEN else RED, 2, alarm = weak > 6
        )
    }
}

@Composable
private fun MiniStatChip(
    modifier: Modifier, icon: String, value: Int, suffix: String, label: String, color: Color, index: Int,
    alarm: Boolean = false,
) {
    val shown = rememberCountUp(value, 1000)
    val transition = rememberInfiniteTransition(label = "stat$index")
    val bounce by transition.animateFloat(
        initialValue = 0.92f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1100 + index * 230, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce",
    )
    // A team with a lot of weaknesses gets a nervous red glow on its tile.
    val alarmGlow by transition.animateFloat(
        initialValue = 0.1f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "alarm",
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CARD),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, if (alarm) RED.copy(alpha = alarmGlow) else Color.White.copy(alpha = 0.06f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(icon, fontSize = 22.sp, modifier = Modifier.graphicsLayer { scaleX = bounce; scaleY = bounce })
            Text(
                "$shown$suffix",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun QuickStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}