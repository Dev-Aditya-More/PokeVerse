package com.aditya1875.pokeverse.screens.analysis.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WeaknessesCard(
    weaknesses: Map<String, List<String>>,
    teamWithTypes: List<TeamMemberWithTypes>
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Team Weaknesses",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Sort by severity (most Pokemon affected)
            val sortedWeaknesses = weaknesses.entries.sortedByDescending { it.value.size }

            sortedWeaknesses.forEach { (type, pokemonNames) ->
                WeaknessRow(
                    type = type,
                    affectedCount = pokemonNames.size,
                    totalCount = teamWithTypes.size
                )
            }
        }
    }
}

@Composable
fun WeaknessRow(type: String, affectedCount: Int, totalCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        Text(
            text = "$affectedCount/$totalCount Pokemon weak",
            color = when {
                affectedCount >= 4 -> Color(0xFFFF5252)
                affectedCount >= 3 -> Color(0xFFFFC107)
                else -> Color.White.copy(alpha = 0.7f)
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
