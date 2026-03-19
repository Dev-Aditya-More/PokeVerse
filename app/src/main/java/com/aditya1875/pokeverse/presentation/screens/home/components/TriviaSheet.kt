package com.aditya1875.pokeverse.presentation.screens.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.BlendMode
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
    "fire" to Color(0xFFFF6B35), "water" to Color(0xFF4FC3F7),
    "grass" to Color(0xFF66BB6A), "electric" to Color(0xFFFFEE58),
    "psychic" to Color(0xFFEC407A), "ice" to Color(0xFF80DEEA),
    "dragon" to Color(0xFF7E57C2), "dark" to Color(0xFF5D4037),
    "fairy" to Color(0xFFF48FB1), "fighting" to Color(0xFFEF5350),
    "flying" to Color(0xFF90CAF9), "poison" to Color(0xFFAB47BC),
    "ground" to Color(0xFFBCAAA4), "rock" to Color(0xFFBDBDBD),
    "bug" to Color(0xFF9CCC65), "ghost" to Color(0xFF5C6BC0),
    "steel" to Color(0xFF78909C), "normal" to Color(0xFFEEEEEE),
)

private enum class OptionState { Default, Correct, Wrong, Neutral }

@Composable
fun DailyTriviaFab(showBadge: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val glow by rememberInfiniteTransition(label = "fab").animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "g"
    )
    Box(modifier = modifier) {
        FloatingActionButton(onClick = onClick, containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp), modifier = Modifier.size(46.dp)) {
            Text("❓", fontSize = 20.sp)
        }
        if (showBadge) {
            Box(modifier = Modifier.size(14.dp).align(Alignment.TopEnd)
                .offset(x = 3.dp, y = (-3).dp).clip(CircleShape)
                .background(Color(0xFFFF1744)).alpha(glow))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTriviaSheet(state: TriviaUiState, onDismiss: () -> Unit, onAnswer: (correct: Boolean) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        when (state) {
            is TriviaUiState.Loading -> Box(Modifier.fillMaxWidth().height(280.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Loading today's Pokémon…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            is TriviaUiState.Ready -> TriviaContent(trivia = state.trivia, onAnswer = onAnswer, onDismiss = onDismiss)
            is TriviaUiState.Error -> Box(Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center) {
                Text("Couldn't load trivia. Try again later.",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            else -> {}
        }
    }
}

@Composable
private fun TriviaContent(trivia: DailyTriviaState, onAnswer: (Boolean) -> Unit, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 8.dp).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Daily Pokémon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Who's that Pokémon?", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text("Gen ${trivia.generation}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(20.dp))
        if (trivia.isAnswered) {
            RevealedTriviaContent(trivia = trivia, onDismiss = onDismiss)
        } else {
            GuessingTriviaContent(trivia = trivia, onAnswer = onAnswer)
        }
        Spacer(Modifier.height(24.dp))
    }
}


@Composable
private fun GuessingTriviaContent(trivia: DailyTriviaState, onAnswer: (Boolean) -> Unit) {

    var tappedIndex by remember { mutableStateOf<Int?>(null) }
    val answered = tappedIndex != null
    val correctIndex = trivia.options.indexOfFirst { it.equals(trivia.pokemonName, ignoreCase = true) }

    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current).data(trivia.spriteUrl).crossfade(true).build()
    )

    Box(modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Image(painter = painter, contentDescription = null,
            modifier = Modifier.fillMaxSize().padding(24.dp).graphicsLayer {
                colorFilter = if (!answered) ColorFilter.tint(Color.Black, BlendMode.SrcAtop) else null
            }, contentScale = ContentScale.Fit)
        if (!answered) {
            Text("???", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.22f))
        }
    }

    Spacer(Modifier.height(14.dp))

    // Name + types — animated in after answering
    AnimatedVisibility(visible = answered, enter = fadeIn() + expandVertically()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(trivia.pokemonName.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()) { trivia.types.forEach { TypeChip(it) } }
            Spacer(Modifier.height(14.dp))
        }
    }

    // Result banner after answering
    AnimatedVisibility(visible = answered,
        enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) {
        val gotIt = tappedIndex == correctIndex
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            color = if (gotIt) Color(0xFF4CAF50).copy(alpha = 0.13f) else Color(0xFFFF1744).copy(alpha = 0.1f),
            border = BorderStroke(1.dp,
                if (gotIt) Color(0xFF4CAF50).copy(alpha = 0.35f) else Color(0xFFFF1744).copy(alpha = 0.3f))
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (gotIt) "🎉" else "💀", fontSize = 24.sp)
                Column {
                    Text(if (gotIt) "Nailed it!" else "So close!", fontWeight = FontWeight.Black,
                        color = if (gotIt) Color(0xFF4CAF50) else Color(0xFFFF1744))
                    Text(if (gotIt) "Come back tomorrow for a new one."
                    else "It was ${trivia.pokemonName.replaceFirstChar { it.uppercase() }}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Hints always visible — gives players clues before guessing
    TriviaHints(trivia)

    Spacer(Modifier.height(20.dp))

    // 4 option buttons
    val optionLabels = listOf("A", "B", "C", "D")
    trivia.options.forEachIndexed { index, option ->
        val optionState = when {
            !answered               -> OptionState.Default
            index == correctIndex   -> OptionState.Correct       // always highlight correct
            index == tappedIndex    -> OptionState.Wrong         // only if they tapped wrong
            else                    -> OptionState.Neutral       // dim the rest
        }
        OptionButton(
            label = optionLabels.getOrElse(index) { "${index + 1}" },
            text = option.split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
            state = optionState,
            enabled = !answered,
            onClick = {
                tappedIndex = index
                onAnswer(index == correctIndex)
            }
        )
        if (index < trivia.options.lastIndex) Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun OptionButton(label: String, text: String, state: OptionState, enabled: Boolean, onClick: () -> Unit) {
    val bgColor = when (state) {
        OptionState.Default -> MaterialTheme.colorScheme.surface
        OptionState.Correct -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        OptionState.Wrong   -> Color(0xFFFF1744).copy(alpha = 0.12f)
        OptionState.Neutral -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when (state) {
        OptionState.Default -> MaterialTheme.colorScheme.outlineVariant
        OptionState.Correct -> Color(0xFF4CAF50)
        OptionState.Wrong   -> Color(0xFFFF1744)
        OptionState.Neutral -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }
    val labelColor = when (state) {
        OptionState.Correct -> Color(0xFF4CAF50)
        OptionState.Wrong   -> Color(0xFFFF1744)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
        .alpha(if (state == OptionState.Neutral) 0.45f else 1f),
        shape = RoundedCornerShape(14.dp), color = bgColor,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(labelColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Text(label, fontWeight = FontWeight.Black, fontSize = 13.sp, color = labelColor)
            }
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            if (state == OptionState.Correct)
                Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Black, fontSize = 18.sp)
            if (state == OptionState.Wrong)
                Text("✗", color = Color(0xFFFF1744), fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun RevealedTriviaContent(trivia: DailyTriviaState, onDismiss: () -> Unit) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current).data(trivia.spriteUrl).crossfade(true).build()
    )
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = if (trivia.wasCorrect) Color(0xFF4CAF50).copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp,
            if (trivia.wasCorrect) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (trivia.wasCorrect) "✅" else "😅", fontSize = 22.sp)
            Text(if (trivia.wasCorrect) "You guessed it! Come back tomorrow."
            else "Better luck tomorrow!", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(16.dp))
    Image(painter = painter, contentDescription = trivia.pokemonName,
        modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Fit)
    Spacer(Modifier.height(12.dp))
    Text(trivia.pokemonName.replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()) { trivia.types.forEach { TypeChip(it) } }
    Spacer(Modifier.height(16.dp))
    TriviaHints(trivia)
    Spacer(Modifier.height(20.dp))
    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp)) { Text("Close") }
}

@Composable
private fun TriviaHints(trivia: DailyTriviaState) {
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Hints", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HintChip("📏 ${trivia.height / 10.0}m")
                HintChip("⚖️ ${trivia.weight / 10.0}kg")
                HintChip("Gen ${trivia.generation}")
            }
            val statNames = mapOf("hp" to "HP", "attack" to "ATK", "defense" to "DEF",
                "special-attack" to "SpA", "special-defense" to "SpD", "speed" to "SPD")
            trivia.baseStats.forEach { (key, value) ->
                val label = statNames[key] ?: return@forEach
                StatBar(label, value)
            }
        }
    }
}

@Composable
private fun HintChip(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatBar(label: String, value: Int) {
    val w by animateFloatAsState((value / 255f).coerceIn(0f,1f), tween(600), label = "s")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(w).clip(RoundedCornerShape(4.dp))
                .background(when { value >= 120 -> Color(0xFF4CAF50); value >= 80 -> Color(0xFF2196F3)
                    value >= 50 -> Color(0xFFFF9800); else -> Color(0xFFEF5350) }))
        }
        Spacer(Modifier.width(8.dp))
        Text(value.toString(), style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun TypeChip(type: String) {
    val c = typeColors[type.lowercase()] ?: MaterialTheme.colorScheme.primary
    Surface(shape = RoundedCornerShape(20.dp), color = c.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, c.copy(alpha = 0.5f))) {
        Text(type.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = c)
    }
}