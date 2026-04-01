package com.aditya1875.pokeverse.feature.item.presentation.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aditya1875.pokeverse.feature.item.presentation.viewmodels.ItemDetailState
import com.aditya1875.pokeverse.feature.item.presentation.viewmodels.ItemViewModel
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components.InfoBlock
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.screens.GlossyCard
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.ItemDetailScreen(
    itemName: String,
    onBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: ItemViewModel = koinViewModel()
) {
    val state by viewModel.detailState.collectAsState()

    LaunchedEffect(itemName) {
        viewModel.loadItemDetail(itemName)
    }

    when (state) {

        is ItemDetailState.Loading -> {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(
                    modifier = Modifier.size(95.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        is ItemDetailState.Success -> {

            val item = (state as ItemDetailState.Success).item
            val key = "item-${item.id}"
            val bgColor = Color(item.categoryColor)

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = item.displayName,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            }
                        }
                    )
                }
            ) { padding ->

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            bgColor.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = key),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val context = LocalContext.current

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.spriteUrl)
                                    .size(512)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.displayName,
                                modifier = Modifier.size(140.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    item {
                        GlossyCard {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Basic Info",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(8.dp))

                                InfoRow("Cost", "${item.cost}")
                                InfoRow("Category", item.categoryDisplay)
                                InfoRow("Fling Power", item.flingPower?.toString() ?: "—")
                            }
                        }
                    }

                    if (item.effect.isNotBlank()) {
                        item {
                            GlossyCard {
                                InfoBlock(
                                    title = "Effect",
                                    accentColor = bgColor
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(bgColor.copy(alpha = 0.15f))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            simplifyEffect(item.effect),
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── DESCRIPTION ─────────────────────────────
                    if (item.flavorText.isNotBlank()) {
                        item {
                            GlossyCard {
                                InfoBlock(
                                    title = "Description",
                                    accentColor = bgColor
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(bgColor.copy(alpha = 0.12f))
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                item.flavorText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── ATTRIBUTES ─────────────────────────────
                    if (item.attributes.isNotEmpty()) {
                        item {
                            GlossyCard {
                                InfoBlock(
                                    title = "Attributes",
                                    accentColor = bgColor
                                ) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item.attributes.forEach {
                                            AssistChip(
                                                onClick = {},
                                                label = {
                                                    Text(mapAttribute(it))
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = bgColor.copy(alpha = 0.2f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        GlossyCard {
                            InfoBlock(
                                title = "Best Use",
                                accentColor = bgColor
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💡", fontSize = 18.sp)

                                    Spacer(Modifier.width(6.dp))

                                    Text(
                                        when {
                                            item.category.contains("ball") ->
                                                "Use to catch Pokémon efficiently"

                                            item.category.contains("heal") ->
                                                "Use during or after battles to recover"

                                            item.category.contains("held") ->
                                                "Give to Pokémon for passive battle effects"

                                            else ->
                                                "Useful in specific situations"
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    if (item.heldByPokemon.isNotEmpty()) {
                        item {
                            GlossyCard {
                                InfoBlock(
                                    title = "Held By Pokémon",
                                    accentColor = bgColor
                                ) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        item.heldByPokemon.take(10).forEach {
                                            AssistChip(
                                                onClick = {},
                                                label = {
                                                    Text(it.replaceFirstChar { c -> c.uppercase() })
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        is ItemDetailState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Failed to load item")
            }
        }

        else -> Unit
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

fun simplifyEffect(effect: String): String {
    return when {
        effect.contains("catch", ignoreCase = true) ->
            "Always catches a Pokémon"

        effect.contains("heal", ignoreCase = true) ->
            "Restores HP or status"

        effect.contains("sleep", ignoreCase = true) ->
            "Wakes up a Pokémon"

        else -> effect
    }
}

fun mapAttribute(attr: String): String {
    return when (attr) {
        "countable" -> "Can carry multiple"
        "consumable" -> "Used once"
        "usable-in-battle" -> "Usable in battle"
        "holdable" -> "Can be held"
        else -> attr
    }
}