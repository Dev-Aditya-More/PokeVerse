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
import androidx.compose.foundation.layout.size
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
fun TypeCoverageCard(coverage: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFFFF5722).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚔️",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Offensive Coverage",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Super effective matchups",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            val sortedCoverage = coverage.entries.sortedByDescending { it.value }

            sortedCoverage.forEach { (type, count) ->
                TypeCoverageRow(type = type, count = count)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun TypeCoverageRow(type: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = PokemonTypeData.getTypeColor(type),
            modifier = Modifier.width(90.dp)
        ) {
            Text(
                text = type.replaceFirstChar { it.uppercase() },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0A0A))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((count.toFloat() / 6f).coerceAtMost(1f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            count == 0 -> Color(0xFF333333)
                            count >= 3 -> Color(0xFF00E676)
                            count >= 2 -> Color(0xFF00BCD4)
                            else -> Color(0xFFFF9800)
                        }
                    )
            )
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .background(
                    when {
                        count == 0 -> Color(0xFF333333)
                        count >= 3 -> Color(0xFF00E676).copy(alpha = 0.2f)
                        count >= 2 -> Color(0xFF00BCD4).copy(alpha = 0.2f)
                        else -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = count.toString(),
                color = when {
                    count == 0 -> Color.White.copy(alpha = 0.5f)
                    count >= 3 -> Color(0xFF00E676)
                    count >= 2 -> Color(0xFF00BCD4)
                    else -> Color(0xFFFF9800)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}