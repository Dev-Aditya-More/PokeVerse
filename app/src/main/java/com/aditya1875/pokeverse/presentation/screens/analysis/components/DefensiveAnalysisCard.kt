package com.aditya1875.pokeverse.presentation.screens.analysis.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DefensiveAnalysisCard(
    weaknesses: Map<String, List<String>>,
    resistances: Map<String, List<String>>,
    teamWithTypes: List<TeamMemberWithTypes>
) {
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
                            Color(0xFF2196F3).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🛡️",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Defensive Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            if (weaknesses.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "WEAKNESSES",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFF6B6B),
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                val sortedWeaknesses = weaknesses.entries.sortedByDescending { it.value.size }
                sortedWeaknesses.take(5).forEach { (type, pokemonNames) ->
                    DefensiveTypeRow(
                        type = type,
                        count = pokemonNames.size,
                        total = teamWithTypes.size,
                        isWeakness = true
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            if (resistances.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "RESISTANCES",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF00E676),
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                val sortedResistances = resistances.entries.sortedByDescending { it.value.size }
                sortedResistances.take(5).forEach { (type, pokemonNames) ->
                    DefensiveTypeRow(
                        type = type,
                        count = pokemonNames.size,
                        total = teamWithTypes.size,
                        isWeakness = false
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun DefensiveTypeRow(
    type: String,
    count: Int,
    total: Int,
    isWeakness: Boolean
) {
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

        Text(
            text = "$count/$total Pokémon",
            color = if (isWeakness) Color(0xFFFF6B6B) else Color(0xFF00E676),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .background(
                    if (isWeakness) Color(0xFFFF6B6B).copy(alpha = 0.2f) else Color(0xFF00E676).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isWeakness) "⚠️ ${(count * 100 / total)}%" else "✓ ${(count * 100 / total)}%",
                color = if (isWeakness) Color(0xFFFF6B6B) else Color(0xFF00E676),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}