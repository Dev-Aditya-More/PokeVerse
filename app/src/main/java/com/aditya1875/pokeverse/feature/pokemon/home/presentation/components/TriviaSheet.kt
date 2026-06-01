package com.aditya1875.pokeverse.feature.pokemon.home.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aditya1875.pokeverse.R
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.aditya1875.pokeverse.feature.pokemon.home.domain.model.DailyTriviaState
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.TriviaUiState
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.pokemonTypeColors
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private enum class OptionState { Default, Correct, Wrong, Neutral }

@Composable
fun DailyTriviaFab(showBadge: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val glow by rememberInfiniteTransition(label = "fab").animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "g"
    )
    Box(modifier = modifier) {
        FloatingActionButton(onClick = onClick, containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxSize()) {
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        when (state) {
            is TriviaUiState.Loading -> Box(
                Modifier.fillMaxWidth().height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.trivia_loading), style = MaterialTheme.typography.bodyMedium)
                }
            }
            is TriviaUiState.Ready -> TriviaContent(trivia = state.trivia, onAnswer = onAnswer, onDismiss = onDismiss)
            is TriviaUiState.Error -> Box(
                Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.trivia_error),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            else -> {}
        }
    }
}

@Composable
private fun TriviaContent(trivia: DailyTriviaState, onAnswer: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var showConfetti by remember { mutableStateOf(false) }
    val confettiComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val confettiProgress by animateLottieCompositionAsState(
        composition = confettiComposition,
        isPlaying = showConfetti,
        speed = 1.4f,
        restartOnPlay = false
    )

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(2800)
            showConfetti = false
        }
    }

    Box(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.trivia_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.trivia_subtitle), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(stringResource(R.string.trivia_generation, trivia.generation),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))

            if (trivia.isAnswered) {
                RevealedTriviaContent(trivia = trivia, onDismiss = onDismiss)
            } else {
                GuessingTriviaContent(
                    trivia = trivia,
                    onAnswer = { correct ->
                        if (correct && !showConfetti) showConfetti = true
                        onAnswer(correct)
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // Confetti overlay — pointer events pass through naturally
        AnimatedVisibility(
            visible = showConfetti,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(600)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LottieAnimation(
                composition = confettiComposition,
                progress = { confettiProgress },
                modifier = Modifier.fillMaxWidth().height(380.dp),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

@Composable
private fun GuessingTriviaContent(trivia: DailyTriviaState, onAnswer: (Boolean) -> Unit) {
    var tappedIndex by remember { mutableStateOf<Int?>(null) }
    val answered = tappedIndex != null
    val correctIndex = trivia.options.indexOfFirst { it.equals(trivia.pokemonName, ignoreCase = true) }

    val haptic = LocalHapticFeedback.current
    val soundManager: SoundManager = koinInject()

    // Container background: surfaceVariant → type-color wash on answer
    val typeAccent = pokemonTypeColors[trivia.types.firstOrNull()?.lowercase() ?: "normal"]
        ?: MaterialTheme.colorScheme.surfaceVariant
    val containerColor by animateColorAsState(
        targetValue = if (answered) typeAccent.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(700),
        label = "container_color"
    )

    // "???" text breathes while waiting for the guess
    val breathScale by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0.90f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )

    // Sprite spring-bounce into view on reveal (starts at 0.88 while hidden, pops to 1.0)
    val revealScale by animateFloatAsState(
        targetValue = if (answered) 1f else 0.88f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "reveal_scale"
    )

    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current).data(trivia.spriteUrl).crossfade(true).build()
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    colorFilter = if (!answered) ColorFilter.tint(Color.Black, BlendMode.SrcAtop) else null
                    scaleX = if (answered) revealScale else 1f
                    scaleY = if (answered) revealScale else 1f
                },
            contentScale = ContentScale.Fit
        )
        if (!answered) {
            Text(
                "???",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.22f),
                modifier = Modifier.graphicsLayer { scaleX = breathScale; scaleY = breathScale }
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    // Pokémon name + types spring-slide up on reveal
    AnimatedVisibility(
        visible = answered,
        enter = slideInVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) { it / 2 }
                + fadeIn(tween(200))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                trivia.pokemonName.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) { trivia.types.forEach { TypeChip(it) } }
            Spacer(Modifier.height(14.dp))
        }
    }

    // Result banner — bounces in
    AnimatedVisibility(
        visible = answered,
        enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) + fadeIn(tween(150))
    ) {
        val gotIt = tappedIndex == correctIndex
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (gotIt) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.13f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
            border = BorderStroke(1.dp,
                if (gotIt) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (gotIt) "🎉" else "💀", fontSize = 24.sp)
                Column {
                    Text(
                        if (gotIt) stringResource(R.string.trivia_nailed_it) else stringResource(R.string.trivia_so_close),
                        fontWeight = FontWeight.Black,
                        color = if (gotIt) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        if (gotIt) stringResource(R.string.trivia_come_back_tomorrow)
                        else stringResource(R.string.trivia_it_was, trivia.pokemonName.replaceFirstChar { it.uppercase() }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    TriviaHints(trivia)
    Spacer(Modifier.height(20.dp))

    // Option buttons
    val optionLabels = listOf("A", "B", "C", "D")
    trivia.options.forEachIndexed { index, option ->
        val optionState = when {
            !answered             -> OptionState.Default
            index == correctIndex -> OptionState.Correct
            index == tappedIndex  -> OptionState.Wrong
            else                  -> OptionState.Neutral
        }
        OptionButton(
            label = optionLabels.getOrElse(index) { "${index + 1}" },
            text = option.split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
            state = optionState,
            enabled = !answered,
            onClick = {
                tappedIndex = index
                val correct = index == correctIndex
                onAnswer(correct)
                if (correct) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    soundManager.play(SoundManager.Sound.CORRECT_ANSWER)
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    soundManager.play(SoundManager.Sound.WRONG_ANSWER)
                }
            }
        )
        if (index < trivia.options.lastIndex) Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun OptionButton(
    label: String,
    text: String,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when (state) {
        OptionState.Default -> MaterialTheme.colorScheme.surface
        OptionState.Correct -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        OptionState.Wrong   -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        OptionState.Neutral -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when (state) {
        OptionState.Default -> MaterialTheme.colorScheme.outlineVariant
        OptionState.Correct -> MaterialTheme.colorScheme.tertiary
        OptionState.Wrong   -> MaterialTheme.colorScheme.error
        OptionState.Neutral -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }
    val labelColor = when (state) {
        OptionState.Correct -> MaterialTheme.colorScheme.tertiary
        OptionState.Wrong   -> MaterialTheme.colorScheme.error
        else                -> MaterialTheme.colorScheme.primary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "btn_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
            .alpha(if (state == OptionState.Neutral) 0.45f else 1f),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(labelColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontWeight = FontWeight.Black, fontSize = 13.sp, color = labelColor)
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (state == OptionState.Correct)
                Text("✓", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Black, fontSize = 18.sp)
            if (state == OptionState.Wrong)
                Text("✗", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun RevealedTriviaContent(trivia: DailyTriviaState, onDismiss: () -> Unit) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current).data(trivia.spriteUrl).crossfade(true).build()
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (trivia.wasCorrect) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp,
            if (trivia.wasCorrect) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (trivia.wasCorrect) "✅" else "😅", fontSize = 22.sp)
            Text(
                if (trivia.wasCorrect) stringResource(R.string.trivia_guessed_correct)
                else stringResource(R.string.trivia_better_luck),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Image(
        painter = painter,
        contentDescription = trivia.pokemonName,
        modifier = Modifier.fillMaxWidth().height(180.dp),
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
    ) { trivia.types.forEach { TypeChip(it) } }
    Spacer(Modifier.height(16.dp))
    TriviaHints(trivia)
    Spacer(Modifier.height(20.dp))
    OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) { Text(stringResource(R.string.action_close)) }
}

@Composable
private fun TriviaHints(trivia: DailyTriviaState) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.trivia_hints_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HintChip("📏 ${trivia.height / 10.0}m")
                HintChip("⚖️ ${trivia.weight / 10.0}kg")
                HintChip(stringResource(R.string.trivia_generation, trivia.generation))
            }
            val statNames = mapOf(
                "hp" to "HP", "attack" to "ATK", "defense" to "DEF",
                "special-attack" to "SpA", "special-defense" to "SpD", "speed" to "SPD"
            )
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
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun StatBar(label: String, value: Int) {
    val w by animateFloatAsState((value / 255f).coerceIn(0f, 1f), tween(600), label = "s")
    val barColor = when {
        value >= 120 -> MaterialTheme.colorScheme.tertiary
        value >= 80  -> MaterialTheme.colorScheme.primary
        value >= 50  -> MaterialTheme.colorScheme.secondary
        else         -> MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(36.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(w).clip(RoundedCornerShape(4.dp))
                    .background(barColor)
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
    val c = pokemonTypeColors[type.lowercase()] ?: MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = c.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, c.copy(alpha = 0.5f))
    ) {
        Text(
            type.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = c
        )
    }
}
