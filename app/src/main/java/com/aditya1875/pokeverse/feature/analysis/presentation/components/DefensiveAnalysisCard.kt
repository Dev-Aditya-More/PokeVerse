package com.aditya1875.pokeverse.feature.analysis.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.CARD
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.GREEN
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.RED

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

@Composable
fun DefenseSection(
    weaknesses: Map<String, List<String>>,
    resistances: Map<String, List<String>>,
    teamSize: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CARD),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Text("🛡️ Defensive Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = Color.White)
            HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

            if (weaknesses.isNotEmpty()) {
                Text("VULNERABLE TO", style = MaterialTheme.typography.labelSmall,
                    color = RED.copy(alpha = 0.8f), letterSpacing = 1.5.sp)
                weaknesses.entries.sortedByDescending { it.value.size }.take(6).forEach { (type, pokemon) ->
                    DefenseTypeBar(type = type, count = pokemon.size, total = teamSize, isWeakness = true)
                }
            }

            if (resistances.isNotEmpty()) {
                Text("RESISTS", style = MaterialTheme.typography.labelSmall,
                    color = GREEN.copy(alpha = 0.8f), letterSpacing = 1.5.sp)
                resistances.entries.sortedByDescending { it.value.size }.take(6).forEach { (type, pokemon) ->
                    DefenseTypeBar(type = type, count = pokemon.size, total = teamSize, isWeakness = false)
                }
            }
        }
    }
}

@Composable
private fun DefenseTypeBar(type: String, count: Int, total: Int, isWeakness: Boolean) {
    val barColor = if (isWeakness) RED else GREEN
    val fraction by animateFloatAsState(
        count.toFloat() / total.toFloat().coerceAtLeast(1f),
        tween(600), label = "bar"
    )
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = PokemonTypeData.getTypeColor(type),
            modifier = Modifier.width(82.dp)
        ) {
            Text(
                type.replaceFirstChar { it.uppercase() },
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = Color.White,
                textAlign = TextAlign.Center
            )
        }
        Box(
            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(RoundedCornerShape(4.dp))
                    .background(barColor.copy(alpha = 0.7f))
            )
        }
        Text(
            "$count/$total",
            style = MaterialTheme.typography.labelMedium,
            color = barColor, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp), textAlign = TextAlign.End
        )
    }
}