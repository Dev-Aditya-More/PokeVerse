package com.aditya1875.pokeverse.screens.analysis.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TypeCoverageCard(
    coverage: Map<String, Int>,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Offensive Type Coverage",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "How many Pokemon can hit each type super effectively",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Sort by coverage count
            val sortedCoverage = coverage.entries.sortedByDescending { it.value }

            sortedCoverage.forEach { (type, count) ->
                TypeCoverageRow(
                    type = type,
                    count = count,
                    maxCount = 6,
                    accentColor = accentColor
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}


@Composable
fun TypeCoverageRow(
    type: String,
    count: Int,
    maxCount: Int,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type badge
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = PokemonTypeData.getTypeColor(type),
            modifier = Modifier.width(80.dp)
        ) {
            Text(
                text = type.replaceFirstChar { it.uppercase() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.width(12.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((count.toFloat() / maxCount).coerceAtMost(1f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            count == 0 -> Color(0xFF555555)
                            count >= 3 -> Color(0xFF4CAF50)
                            count >= 2 -> Color(0xFFFFC107)
                            else -> accentColor
                        }
                    )
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.End
        )
    }
}
