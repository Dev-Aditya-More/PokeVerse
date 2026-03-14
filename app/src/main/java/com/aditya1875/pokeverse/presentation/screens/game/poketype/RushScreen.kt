package com.aditya1875.pokeverse.presentation.screens.game.poketype

import androidx.activity.compose.BackHandler
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
import com.aditya1875.pokeverse.domain.xp.XPResult
import com.aditya1875.pokeverse.presentation.screens.game.GameResultLayout
import com.aditya1875.pokeverse.presentation.screens.game.poketype.components.TypeRushDifficulty
import com.aditya1875.pokeverse.presentation.screens.game.poketype.components.TypeRushState
import com.aditya1875.pokeverse.presentation.screens.leaderboard.components.XPOverlay
import com.aditya1875.pokeverse.presentation.ui.viewmodel.TypeRushViewModel
import org.koin.androidx.compose.koinViewModel

private val TYPE_COLORS = mapOf(
    "normal" to Color(0xFFAAA67F), "fire" to Color(0xFFF57D31),
    "water" to Color(0xFF6493EB), "electric" to Color(0xFFF9CF30),
    "grass" to Color(0xFF74CB48), "ice" to Color(0xFF9AD6DF),
    "fighting" to Color(0xFFC12239), "poison" to Color(0xFFA43E9E),
    "ground" to Color(0xFFDEC16B), "flying" to Color(0xFFA891EC),
    "psychic" to Color(0xFFFB5584), "bug" to Color(0xFFA7B723),
    "rock" to Color(0xFFB69E31), "ghost" to Color(0xFF70559B),
    "dragon" to Color(0xFF7037FF), "dark" to Color(0xFF75574C),
    "steel" to Color(0xFFB7B9D0), "fairy" to Color(0xFFE69EAC),
)

private fun typeColor(type: String) = TYPE_COLORS[type.lowercase()] ?: Color.Gray

@Composable
fun TypeRushScreen(
    difficulty: TypeRushDifficulty,
    onBack: () -> Unit,
    viewModel: TypeRushViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    var pendingXp by remember { mutableStateOf<XPResult?>(null) }
    LaunchedEffect(Unit) { viewModel.xpResult.collect { pendingXp = it } }

    BackHandler { showExitDialog = true }
    LaunchedEffect(difficulty) { viewModel.startGame(difficulty) }
    DisposableEffect(Unit) { onDispose { viewModel.resetGame() } }

    XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                    )
                )
                .navigationBarsPadding()
        ) {
            when (val s = state) {
                is TypeRushState.Idle -> {}
                is TypeRushState.Loading -> LoadingContent()
                is TypeRushState.Playing -> PlayingContent(
                    state = s,
                    difficulty = difficulty,
                    onTypeTapped = { viewModel.onTypeTapped(it) },
                    onBack = { showExitDialog = true }
                )

                is TypeRushState.RoundResult -> RoundResultContent(
                    state = s,
                    onNext = { viewModel.nextRound() }
                )

                is TypeRushState.Finished -> TypeRushResultScreen(
                    state = s,
                    onPlayAgain = { viewModel.startGame(difficulty) },
                    onBack = onBack
                )
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit game?") },
            text = { Text("Your progress will be lost.") },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; viewModel.resetGame(); onBack() }) {
                    Text("Exit", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PlayingContent(
    state: TypeRushState.Playing,
    difficulty: TypeRushDifficulty,
    onTypeTapped: (String) -> Unit,
    onBack: () -> Unit,
) {
    val timerColor by animateColorAsState(
        targetValue = when {
            state.timeRemaining <= 3 -> Color(0xFFFF1744)
            state.timeRemaining <= 5 -> Color(0xFFFF9800)
            else -> MaterialTheme.colorScheme.primary
        },
        label = "timerColor"
    )

    Scaffold(
        Modifier
            .fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, null)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${state.questionIndex + 1} / ${state.totalQuestions}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Score: ${state.score}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        null,
                        tint = timerColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${state.timeRemaining}s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = timerColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { state.timeRemaining.toFloat() / difficulty.timePerRound },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = timerColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "What type is this Pokémon?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.question.correctTypes.size > 1) {
                Text(
                    text = "It has ${state.question.correctTypes.size} types. tap both!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                val painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(state.question.spriteUrl)
                        .crossfade(true)
                        .build()
                )
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            colorFilter = if (!difficulty.showName && !state.isLocked)
                                ColorFilter.tint(Color.Black, BlendMode.SrcAtop)
                            else null
                        },
                    contentScale = ContentScale.Fit
                )
            }

            if (difficulty.showName) {
                Text(
                    text = state.question.pokemonName
                        .split("-")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "???",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            TypeBubbleGrid(
                options = state.question.options,
                correctTypes = state.question.correctTypes,
                selectedTypes = state.selectedTypes,
                isLocked = state.isLocked,
                onTypeTapped = onTypeTapped,
            )
        }
    }
}

@Composable
private fun TypeBubbleGrid(
    options: List<String>,
    correctTypes: List<String>,
    selectedTypes: Set<String>,
    isLocked: Boolean,
    onTypeTapped: (String) -> Unit,
) {
    val rows = options.chunked(3)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { type ->
                    val isSelected = type in selectedTypes
                    val isCorrect = type in correctTypes

                    // State-driven visual
                    val bubbleColor = typeColor(type)
                    val bgAlpha = when {
                        isLocked && isCorrect -> 0.9f
                        isLocked && isSelected -> 0.3f
                        isSelected -> 0.7f
                        else -> 0.15f
                    }
                    val borderAlpha = if (isSelected || (isLocked && isCorrect)) 1f else 0.3f
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.06f else 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy),
                        label = "bubbleScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bubbleColor.copy(alpha = bgAlpha))
                            .border(
                                width = if (isSelected || (isLocked && isCorrect)) 2.dp else 1.dp,
                                color = bubbleColor.copy(alpha = borderAlpha),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !isLocked) { onTypeTapped(type) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isLocked && isCorrect) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else if (isLocked && isSelected) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (bgAlpha > 0.4f) Color.White
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RoundResultContent(
    state: TypeRushState.RoundResult,
    onNext: () -> Unit,
) {
    val result = state.result
    val resultColor = when {
        result.isFullyCorrect -> Color(0xFF4CAF50)
        result.isPartiallyCorrect -> Color(0xFFFF9800)
        else -> Color(0xFFFF1744)
    }
    val emoji = when {
        result.isFullyCorrect -> "✅"
        result.isPartiallyCorrect -> "😐"
        else -> "❌"
    }
    val label = when {
        result.isFullyCorrect -> "Perfect!"
        result.isPartiallyCorrect -> "Partial"
        else -> "Wrong!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = resultColor
        )

        Spacer(Modifier.height(16.dp))

        // Pokémon name
        Text(
            result.question.pokemonName
                .split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        // Correct types shown
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            result.question.correctTypes.forEach { type ->
                val color = typeColor(type)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = color.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
                ) {
                    Text(
                        type.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Points breakdown
        if (result.pointsEarned > 0) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = resultColor.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "+${result.pointsEarned}", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black, color = resultColor
                        )
                        Text(
                            "Points", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (result.timeBonus > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "+${result.timeBonus}", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black, color = Color(0xFFFFD700)
                            )
                            Text(
                                "Time Bonus", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${state.score}", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Total", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            val isLast = state.questionIndex >= state.totalQuestions - 1
            Text(
                if (isLast) "See Results" else "Next Pokémon →",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TypeRushResultScreen(
    state: TypeRushState.Finished,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
) {
    val pct = state.correctRounds.toFloat() / state.totalRounds
    val stars = when {
        pct >= 0.9f -> 3; pct >= 0.7f -> 2; pct >= 0.5f -> 1; else -> 0
    }

    GameResultLayout(
        title = "Type Rush",
        subtitle = "Speed typing challenge",
        score = state.score.toString(),
        scoreLabel = "Final Score",
        heroColor = MaterialTheme.colorScheme.primary,
        onPlayAgain = onPlayAgain,
        onBack = onBack,
        heroContent = {

            Text(
                "★".repeat(stars) + "☆".repeat(3 - stars),
                fontSize = 36.sp
            )
        },
        statsContent = {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                ResultStatCard(
                    Modifier.weight(1f),
                    "Correct",
                    "${state.correctRounds}/${state.totalRounds}"
                )

                ResultStatCard(
                    Modifier.weight(1f),
                    "Accuracy",
                    "${(pct * 100).toInt()}%"
                )

                ResultStatCard(
                    Modifier.weight(1f),
                    "Difficulty",
                    state.difficulty.label
                )
            }
        }
    )
}

@Composable
private fun ResultStatCard(modifier: Modifier, label: String, value: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Loading Pokémon types...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}