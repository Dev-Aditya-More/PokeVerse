package com.aditya1875.pokeverse.feature.berry.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.feature.berry.data.source.remote.model.BerryUiModel
import com.aditya1875.pokeverse.feature.berry.presentation.viewmodels.BerryDetailState
import com.aditya1875.pokeverse.feature.berry.presentation.viewmodels.BerryViewModel
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components.InfoBlock
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.screens.GlossyCard
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BerryDetailScreen(
    berryName: String,
    onBack: () -> Unit,
    viewModel: BerryViewModel = koinViewModel()
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(berryName) { viewModel.loadBerryDetail(berryName) }

    when (val s = state) {
        BerryDetailState.Loading, BerryDetailState.Idle -> {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(modifier = Modifier.size(95.dp), color = MaterialTheme.colorScheme.primary)
            }
        }

        is BerryDetailState.Success -> BerryDetailContent(
            berry = s.berry,
            onBack = onBack
        )

        BerryDetailState.NotFound -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Berry not found", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BerryDetailContent(berry: BerryUiModel, onBack: () -> Unit) {
    val typeColor = pokemonTypeColor(berry.naturalGiftType)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(berry.displayName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── HERO ────────────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(typeColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Radial glow
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(typeColor.copy(alpha = 0.4f), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                    )
                    AsyncImage(
                        model = berry.spriteUrl,
                        contentDescription = berry.displayName,
                        modifier = Modifier.size(160.dp),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None
                    )
                    // Type badge pinned bottom-end
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = typeColor.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            berry.naturalGiftType.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // ── QUICK STATS STRIP ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatPill(label = "Power", value = "${berry.naturalGiftPower}", color = typeColor, modifier = Modifier.weight(1f))
                    StatPill(label = "Size", value = "${berry.size} mm", color = typeColor, modifier = Modifier.weight(1f))
                    StatPill(label = "Growth", value = "${berry.growthTime}h", color = typeColor, modifier = Modifier.weight(1f))
                }
            }

            // ── PROPERTIES ──────────────────────────────────────────────────────
            item {
                GlossyCard {
                    InfoBlock(title = "Properties", accentColor = typeColor) {
                        BerryInfoRow("Natural Gift Type", berry.naturalGiftType.replaceFirstChar { it.uppercase() })
                        BerryInfoRow("Natural Gift Power", berry.naturalGiftPower.toString())
                        BerryInfoRow("Firmness", berry.firmness.split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
                        BerryInfoRow("Size", "${berry.size} mm")
                        BerryInfoRow("Growth Time", "${berry.growthTime} hours per stage")
                    }
                }
            }

            // ── FLAVOR PROFILE ──────────────────────────────────────────────────
            if (berry.flavorPotencies.isNotEmpty()) {
                item {
                    GlossyCard {
                        InfoBlock(title = "Flavor Profile", accentColor = typeColor) {
                            val flavorOrder = listOf("spicy", "dry", "sweet", "bitter", "sour")
                            val flavorEmoji = mapOf(
                                "spicy" to "🌶", "dry" to "💧", "sweet" to "🍰",
                                "bitter" to "☕", "sour" to "🍋"
                            )
                            val flavorColors = mapOf(
                                "spicy" to Color(0xFFE53935),
                                "dry" to Color(0xFF1E88E5),
                                "sweet" to Color(0xFFEC407A),
                                "bitter" to Color(0xFF43A047),
                                "sour" to Color(0xFFFDD835)
                            )
                            val maxPotency = berry.flavorPotencies.values.maxOrNull()?.coerceAtLeast(1) ?: 1

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                flavorOrder.forEach { flavor ->
                                    val potency = berry.flavorPotencies[flavor] ?: 0
                                    val barColor = flavorColors[flavor] ?: typeColor
                                    val emoji = flavorEmoji[flavor] ?: ""
                                    val label = flavor.replaceFirstChar { it.uppercase() }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(emoji, fontSize = 16.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(48.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            if (potency > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(potency.toFloat() / maxPotency)
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(barColor)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            potency.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (potency > 0) barColor
                                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            modifier = Modifier.width(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── BATTLE TIP ──────────────────────────────────────────────────────
            item {
                GlossyCard {
                    InfoBlock(title = "Battle Use", accentColor = typeColor) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                battleTip(berry),
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BerryInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun battleTip(berry: BerryUiModel): String {
    return when {
        berry.naturalGiftPower >= 90 -> "High-power Natural Gift move (${berry.naturalGiftPower} base power). Use it when you need a reliable ${berry.naturalGiftType}-type hit."
        berry.dominantFlavor == "spicy" -> "Lowers the Pokémon's Atk EVs. Also used in Pokéblocks for conditions."
        berry.dominantFlavor == "dry" -> "Lowers the Pokémon's SpAtk EVs. Also used in Poffins and Pokéblocks."
        berry.dominantFlavor == "sweet" -> "Lowers the Pokémon's Speed EVs. Raises friendship when held."
        berry.dominantFlavor == "bitter" -> "Lowers the Pokémon's SpDef EVs. Also raises friendship."
        berry.dominantFlavor == "sour" -> "Lowers the Pokémon's Def EVs. Useful for EV training."
        else -> "Provides a ${berry.naturalGiftType}-type Natural Gift attack with ${berry.naturalGiftPower} base power."
    }
}
