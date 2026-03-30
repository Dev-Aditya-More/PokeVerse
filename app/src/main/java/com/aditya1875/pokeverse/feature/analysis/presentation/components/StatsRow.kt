package com.aditya1875.pokeverse.feature.analysis.presentation.components

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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStatChip(
            Modifier.weight(1f), "⚔️",
            "${analysis.offensiveCoverage.values.count { it > 0 }}/18",
            "Coverage", BLUE
        )
        MiniStatChip(
            Modifier.weight(1f), "🎨",
            "${analysis.typeDiversity.uniqueTypes}",
            "Types", PURPLE
        )
        MiniStatChip(
            Modifier.weight(1f), "🛡️",
            "${analysis.defensiveWeaknesses.size}",
            "Weaknesses",
            if (analysis.defensiveWeaknesses.size <= 4) GREEN else RED
        )
    }
}

@Composable
private fun MiniStatChip(modifier: Modifier, icon: String, value: String, label: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CARD),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 20.sp)
            Text(value, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
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