package com.aditya1875.pokeverse.presentation.screens.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.aditya1875.pokeverse.domain.trivia.DailyTriviaState
import com.aditya1875.pokeverse.presentation.ui.viewmodel.TriviaUiState

private val typeColors = mapOf(
    "fire" to Color(0xFFFF6B35),
    "water" to Color(0xFF4FC3F7),
    "grass" to Color(0xFF66BB6A),
    "electric" to Color(0xFFFFEE58),
    "psychic" to Color(0xFFEC407A),
    "ice" to Color(0xFF80DEEA),
    "dragon" to Color(0xFF7E57C2),
    "dark" to Color(0xFF5D4037),
    "fairy" to Color(0xFFF48FB1),
    "fighting" to Color(0xFFEF5350),
    "flying" to Color(0xFF90CAF9),
    "poison" to Color(0xFFAB47BC),
    "ground" to Color(0xFFBCAAA4),
    "rock" to Color(0xFFBDBDBD),
    "bug" to Color(0xFF9CCC65),
    "ghost" to Color(0xFF5C6BC0),
    "steel" to Color(0xFF78909C),
    "normal" to Color(0xFFEEEEEE),
)

@Composable
fun DailyTriviaFab(
    showBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow"
    )

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.size(46.dp)
        ) {
            Text(
                text = "❓",
                fontSize = 20.sp
            )
        }

        if (showBadge) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF1744))
                    .alpha(glow)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTriviaSheet(
    state: TriviaUiState,
    onDismiss: () -> Unit,
    onAnswer: (correct: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        when (state) {
            is TriviaUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading today's Pokémon...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            is TriviaUiState.Ready -> {
                TriviaContent(
                    trivia = state.trivia,
                    onAnswer = onAnswer,
                    onDismiss = onDismiss
                )
            }

            is TriviaUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Couldn't load trivia. Try again later.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TriviaContent(
    trivia: DailyTriviaState,
    onAnswer: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Daily Pokémon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Who's that Pokémon?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    "Gen ${trivia.generation}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (trivia.isAnswered) {
            RevealedTriviaContent(trivia = trivia, onDismiss = onDismiss)
        } else {
            SilhouetteTriviaContent(trivia = trivia, onAnswer = onAnswer)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SilhouetteTriviaContent(
    trivia: DailyTriviaState,
    onAnswer: (Boolean) -> Unit
) {
    var revealed by remember { mutableStateOf(false) }

    // Silhouette image
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(trivia.spriteUrl)
                .crossfade(true)
                .build()
        )
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    colorFilter = if (!revealed) ColorFilter.tint(
                        Color.Black, blendMode = BlendMode.SrcAtop
                    ) else null
                },
            contentScale = ContentScale.Fit
        )

        if (!revealed) {
            Text(
                "???",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }

    Spacer(Modifier.height(20.dp))

    if (!revealed) {
        // Hint stats — visible even before reveal to help advanced players
        TriviaHints(trivia)

        Spacer(Modifier.height(20.dp))

        // Reveal button
        Button(
            onClick = { revealed = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Reveal Pokémon", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    } else {
        // Name revealed — did you know?
        Text(
            text = trivia.pokemonName.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Types
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            trivia.types.forEach { type ->
                TypeChip(type)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Did you know? — simple stat display
        TriviaHints(trivia)

        Spacer(Modifier.height(24.dp))

        // Self-assessment buttons
        Text(
            "Did you know it?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onAnswer(false) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Nope 😅", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onAnswer(true) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Got it! 🚀", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Already answered — show full info + XP gained feedback
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RevealedTriviaContent(
    trivia: DailyTriviaState,
    onDismiss: () -> Unit
) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(trivia.spriteUrl)
            .crossfade(true)
            .build()
    )

    // Result banner
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (trivia.wasCorrect)
            Color(0xFF4CAF50).copy(alpha = 0.15f)
        else
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (trivia.wasCorrect) "✅" else "😅", fontSize = 22.sp)
            Text(
                if (trivia.wasCorrect) "You got it! Come back tomorrow." else "Better luck tomorrow!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Image(
        painter = painter,
        contentDescription = trivia.pokemonName,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentScale = ContentScale.Fit
    )

    Spacer(Modifier.height(12.dp))

    Text(
        trivia.pokemonName.replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        trivia.types.forEach { TypeChip(it) }
    }

    Spacer(Modifier.height(16.dp))

    TriviaHints(trivia)

    Spacer(Modifier.height(20.dp))

    OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Close")
    }
}

@Composable
private fun TriviaHints(trivia: DailyTriviaState) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Hints", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

            // Height + weight
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HintChip("📏 ${trivia.height / 10.0}m")
                HintChip("⚖️ ${trivia.weight / 10.0}kg")
                HintChip("Gen ${trivia.generation}")
            }

            // Base stats bar chart (simplified)
            val statNames = mapOf(
                "hp" to "HP",
                "attack" to "ATK",
                "defense" to "DEF",
                "special-attack" to "SpA",
                "special-defense" to "SpD",
                "speed" to "SPD",
            )
            trivia.baseStats.forEach { (key, value) ->
                val label = statNames[key] ?: return@forEach
                StatBar(label = label, value = value)
            }
        }
    }
}

@Composable
private fun HintChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun StatBar(label: String, value: Int) {
    val maxStat = 255f
    val animatedWidth by animateFloatAsState(
        targetValue = (value / maxStat).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "stat_$label"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(36.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedWidth)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            value >= 120 -> Color(0xFF4CAF50)
                            value >= 80 -> Color(0xFF2196F3)
                            value >= 50 -> Color(0xFFFF9800)
                            else -> Color(0xFFEF5350)
                        }
                    )
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            value.toString(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TypeChip(type: String) {
    val color = typeColors[type.lowercase()] ?: MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = type.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}