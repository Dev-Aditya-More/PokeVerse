package com.aditya1875.pokeverse.presentation.screens.analysis.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.aditya1875.pokeverse.presentation.screens.analysis.AnalysisColors.AMBER
import com.aditya1875.pokeverse.presentation.screens.analysis.AnalysisColors.BLUE
import com.aditya1875.pokeverse.presentation.screens.analysis.AnalysisColors.CARD
import com.aditya1875.pokeverse.presentation.screens.analysis.AnalysisColors.CARD2
import com.aditya1875.pokeverse.presentation.screens.analysis.AnalysisColors.GREEN

@Composable
fun CoverageSection(coverage: Map<String, Int>, teamSize: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CARD),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("⚔️  Offensive Coverage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "How many of your Pokémon can hit each type super-effectively",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f)
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot(CARD2, "None")
                LegendDot(AMBER.copy(alpha = 0.7f), "1")
                LegendDot(BLUE.copy(alpha = 0.7f), "2+")
                LegendDot(GREEN.copy(alpha = 0.8f), "3+")
            }

            // Grid — 3 columns
            val sortedCoverage = coverage.entries
                .sortedByDescending { it.value }
                .toList()

            val rows = sortedCoverage.chunked(3)
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (type, count) ->
                        CoverageCell(
                            modifier = Modifier.weight(1f),
                            type = type, count = count, teamSize = teamSize
                        )
                    }
                    // Fill empty cells in last row
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CoverageCell(modifier: Modifier, type: String, count: Int, teamSize: Int) {
    val cellColor = when {
        count == 0           -> CARD2
        count >= 3           -> GREEN.copy(alpha = 0.25f)
        count >= 2           -> BLUE.copy(alpha = 0.2f)
        else                 -> AMBER.copy(alpha = 0.18f)
    }
    val textColor = when {
        count == 0           -> Color.White.copy(alpha = 0.28f)
        count >= 3           -> GREEN
        count >= 2           -> BLUE
        else                 -> AMBER
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = cellColor
    ) {
        Column(
            Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                type.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = if (count == 0) 0.3f else 0.8f),
                textAlign = TextAlign.Center, maxLines = 1
            )
            Text(
                if (count == 0) "—" else count.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f))
    }
}