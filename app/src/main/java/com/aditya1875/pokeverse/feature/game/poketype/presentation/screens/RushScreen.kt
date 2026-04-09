package com.aditya1875.pokeverse.feature.game.poketype.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushDifficulty
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushState
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.game.poketype.presentation.components.TypeRushResultScreen
import com.aditya1875.pokeverse.feature.game.poketype.presentation.viewmodels.TypeRushViewModel
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) { paddingValues ->
            when (val s = state) {
                is TypeRushState.Idle -> {}
                is TypeRushState.Loading -> TypeRushLoadingContent()
                is TypeRushState.Playing -> PlayingContent(
                    state = s, difficulty = difficulty,
                    onTypeTapped = { viewModel.onTypeTapped(it) },
                    onBack = { showExitDialog = true },
                    modifier = Modifier.padding(paddingValues)
                )
                is TypeRushState.RoundResult -> RoundResultContent(
                    state = s, onNext = { viewModel.nextRound() }
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
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PlayingContent(
    state: TypeRushState.Playing,
    difficulty: TypeRushDifficulty,
    onTypeTapped: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier
) {
    val timerFraction = state.timeRemaining.toFloat() / difficulty.timePerRound
    val timerColor by animateColorAsState(
        targetValue = when {
            state.timeRemaining <= 3 -> Color(0xFFFF1744)
            state.timeRemaining <= 5 -> Color(0xFFFF9800)
            else -> MaterialTheme.colorScheme.primary
        }, label = "tc"
    )
    val glowColor = typeColor(state.question.correctTypes.first())

    val context = LocalContext.current

    val request = remember(state.question.spriteUrl) {
        ImageRequest.Builder(context)
            .data(state.question.spriteUrl)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .size(Size.ORIGINAL)
            .build()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(glowColor.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.28f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.5f, size.height * 0.28f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
        ) {
            // HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                // Dot progress
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(state.totalQuestions) { i ->
                        val w by animateDpAsState(
                            if (i == state.questionIndex) 18.dp else 6.dp, label = "dot"
                        )
                        Box(
                            modifier = Modifier.width(w).height(6.dp).clip(CircleShape)
                                .background(
                                    if (i <= state.questionIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${state.score}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Timer bar
            LinearProgressIndicator(
                progress = { timerFraction },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = timerColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )

            Spacer(Modifier.height(22.dp))

            // Prompt
            Text(
                text = if (state.question.correctTypes.size > 1) "What are this Pokémon's types?"
                else "What type is this Pokémon?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            )
            if (state.question.correctTypes.size > 1) {
                Text(
                    "Tap both ✦",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Sprite
            Box(modifier = Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(175.dp).clip(CircleShape)
                        .background(Brush.radialGradient(
                            listOf(glowColor.copy(alpha = 0.2f), Color.Transparent)
                        ))
                )
                val painter = rememberAsyncImagePainter(request)

                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(165.dp).graphicsLayer {
                        colorFilter = if (!difficulty.showName && !state.isLocked)
                            ColorFilter.tint(Color.Black, BlendMode.SrcAtop) else null
                    },
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = if (difficulty.showName)
                    state.question.pokemonName.split("-")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                else "???",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = if (difficulty.showName) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

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
    val soundManager: SoundManager = koinInject()
    val rows = options.chunked(3)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { type ->
                    val isSelected = type in selectedTypes
                    val isCorrect = type in correctTypes
                    val isWrong = isLocked && isSelected && !isCorrect
                    val tc = typeColor(type)

                    val bgAlpha by animateFloatAsState(
                        when { isLocked && isCorrect -> 0.85f; isWrong -> 0.18f; isSelected -> 0.65f; else -> 0.12f },
                        label = "bg"
                    )
                    val scale by animateFloatAsState(
                        when { isLocked && isCorrect -> 1.04f; isSelected -> 1.06f; else -> 1f },
                        spring(Spring.DampingRatioMediumBouncy), label = "sc"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f).scale(scale)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tc.copy(alpha = bgAlpha))
                            .border(
                                if (isSelected || (isLocked && isCorrect)) 2.dp else 1.dp,
                                if (isWrong) Color(0xFFFF1744).copy(alpha = 0.6f)
                                else tc.copy(alpha = if (isSelected || (isLocked && isCorrect)) 1f else 0.28f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !isLocked) {
                                soundManager.play(SoundManager.Sound.RUSH_CLICK); onTypeTapped(type)
                            }
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isLocked && isCorrect)
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            else if (isWrong)
                                Icon(Icons.Default.Close, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(13.dp))
                            Text(
                                type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (bgAlpha > 0.35f) Color.White
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RoundResultContent(state: TypeRushState.RoundResult, onNext: () -> Unit) {
    val result = state.result
    val isCorrect = result.isFullyCorrect
    val isPartial = result.isPartiallyCorrect && !isCorrect

    val accentColor = when { isCorrect -> Color(0xFF4CAF50); isPartial -> Color(0xFFFF9800); else -> Color(0xFFFF1744) }
    val emoji = when { isCorrect -> "✅"; isPartial -> "😐"; else -> "❌" }
    val headlineText = when { isCorrect -> "Perfect!"; isPartial -> "Partial!"; else -> "Wrong!" }

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, accentColor.copy(alpha = 0.08f))))
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.6f))

            Text(emoji, fontSize = 72.sp, modifier = Modifier.scale(entrance.value))
            Spacer(Modifier.height(12.dp))

            Text(
                headlineText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = accentColor,
                modifier = Modifier.alpha(entrance.value)
                    .offset(y = ((1f - entrance.value) * 20f).dp)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                result.question.pokemonName.split("-")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                result.question.correctTypes.forEach { type ->
                    val tc = typeColor(type)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = tc.copy(alpha = 0.18f),
                        border = BorderStroke(1.5.dp, tc.copy(alpha = 0.55f))
                    ) {
                        Text(
                            type.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = tc
                        )
                    }
                }
            }

            Spacer(Modifier.weight(0.5f))

            if (result.pointsEarned > 0 || result.timeBonus > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = accentColor.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (result.pointsEarned > 0)
                            PointPill("+${result.pointsEarned}", "Points", accentColor)
                        if (result.timeBonus > 0)
                            PointPill("+${result.timeBonus}", "Speed", Color(0xFFFFD700))
                        PointPill("${state.score}", "Total", MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Text(
                    if (state.questionIndex >= state.totalQuestions - 1) "See Results →" else "Next Pokémon →",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PointPill(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TypeRushLoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚡", fontSize = 52.sp)
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Text("Loading types…", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}