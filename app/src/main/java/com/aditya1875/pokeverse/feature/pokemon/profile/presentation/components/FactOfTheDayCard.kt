package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.pokemon.profile.domain.FactEngine
import com.aditya1875.pokeverse.feature.pokemon.profile.domain.FactProgressStore
import com.aditya1875.pokeverse.feature.pokemon.profile.domain.PokemonFact
import com.aditya1875.pokeverse.utils.Haptics
import com.aditya1875.pokeverse.utils.SoundManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Display name for a fact category (the JSON key stays English; only what's shown is translated). */
@Composable
private fun factCategoryLabel(category: String): String = when (category) {
    "History" -> stringResource(R.string.fact_category_history)
    "Lore" -> stringResource(R.string.fact_category_lore)
    "Mechanics" -> stringResource(R.string.fact_category_mechanics)
    "Trivia" -> stringResource(R.string.fact_category_trivia)
    "Design" -> stringResource(R.string.fact_category_design)
    else -> category
}

private val categoryColors: Map<String, Color> = mapOf(
    "History" to Color(0xFF42A5F5),
    "Lore" to Color(0xFFAB47BC),
    "Mechanics" to Color(0xFF66BB6A),
    "Trivia" to Color(0xFFFFA726),
    "Design" to Color(0xFF26C6DA)
)

private const val GRID_W = 12
private const val GRID_H = 6
private const val REVEAL_AT = 0.45f

/**
 * Daily Fact as a scratch card: scratch the holographic foil to reveal today's
 * fact (+XP once a day, with a fact streak), react to it, share it, or roll a
 * bonus fact.
 */
@Composable
fun FactOfTheDayCard(
    onRevealed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics: Haptics = koinInject()
    val sound: SoundManager = koinInject()
    val store = remember { FactProgressStore(context) }
    val todays = remember { FactEngine.todaysFact(context) }

    var fact by remember { mutableStateOf(todays) }
    val isBonus = fact != todays
    var revealed by remember { mutableStateOf(store.revealedToday) }
    var reacted by remember { mutableStateOf(store.reactedToday) }
    var streak by remember { mutableIntStateOf(store.streak) }
    var burstKey by remember { mutableIntStateOf(0) }

    val accent = categoryColors[fact.category] ?: MaterialTheme.colorScheme.primary

    fun reveal() {
        if (revealed) return
        revealed = true
        val payXp = store.markRevealed()
        streak = store.streak
        burstKey++
        haptics.play(Haptics.Haptic.POP)
        sound.play(SoundManager.Sound.MATCH_FOUND, volume = 0.7f, haptic = false)
        if (payXp) onRevealed()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box {
            // Category-tinted wash behind everything.
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)))
            )
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FactHeader(fact, accent, streak, isBonus)

                Box(Modifier.fillMaxWidth().heightIn(min = 96.dp)) {
                    AnimatedContent(
                        targetState = fact,
                        transitionSpec = { (fadeIn(tween(250)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(150)) },
                        label = "fact"
                    ) { f ->
                        Text(
                            text = f.fact,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        )
                    }
                    if (!revealed) {
                        ScratchFoil(
                            accent = accent,
                            onScratch = { haptics.play(Haptics.Haptic.TICK) },
                            onRevealThreshold = ::reveal,
                            modifier = Modifier
                                .matchParentSize()
                                .semantics { onClick(label = context.getString(R.string.fact_reveal_a11y)) { reveal(); true } }
                        )
                    }
                    ConfettiBurst(burstKey, accent, Modifier.matchParentSize())
                }

                if (revealed) {
                    if (!isBonus) {
                        ReactionRow(
                            reacted = reacted,
                            learned = store.learned,
                            knew = store.knew,
                            onReact = { newToMe ->
                                store.react(newToMe)
                                reacted = true
                                haptics.play(if (newToMe) Haptics.Haptic.SUCCESS else Haptics.Haptic.CONFIRM)
                            }
                        )
                    }
                    FactActions(
                        fact = fact,
                        onAnother = {
                            haptics.play(Haptics.Haptic.CLICK)
                            fact = FactEngine.randomFact(context, exclude = fact)
                        },
                        onBackToToday = if (isBonus) ({ fact = todays }) else null
                    )
                } else {
                    Text(
                        stringResource(R.string.fact_scratch_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FactHeader(fact: PokemonFact, accent: Color, streak: Int, isBonus: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) { Text("💡", fontSize = 18.sp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(if (isBonus) R.string.fact_bonus else R.string.fact_of_the_day),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (streak > 0 && !isBonus) {
                Text(
                    "🔥 " + stringResource(R.string.fact_streak, streak),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF8A3D),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    stringResource(R.string.fact_refreshes_tomorrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = accent.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
        ) {
            Text(
                factCategoryLabel(fact.category),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

/**
 * Holographic foil you rub away with a finger. Coverage is tracked on a coarse
 * grid; past [REVEAL_AT] the card pops open on its own.
 */
@Composable
private fun ScratchFoil(
    accent: Color,
    onScratch: () -> Unit,
    onRevealThreshold: () -> Unit,
    modifier: Modifier
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val cells = remember { BooleanArray(GRID_W * GRID_H) }
    var scratched by remember { mutableIntStateOf(0) }
    var lastTickCells by remember { mutableIntStateOf(0) }
    val textMeasurer = rememberTextMeasurer()
    val label = stringResource(R.string.fact_scratch_foil)
    val shimmer by rememberInfiniteTransition(label = "foil").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "foil_shift"
    )

    Canvas(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(Unit) {
                val brush = 26.dp.toPx()
                fun mark(p: Offset) {
                    val cw = size.width / GRID_W.toFloat()
                    val ch = size.height / GRID_H.toFloat()
                    for (gx in 0 until GRID_W) for (gy in 0 until GRID_H) {
                        val idx = gy * GRID_W + gx
                        if (cells[idx]) continue
                        val cx = (gx + 0.5f) * cw
                        val cy = (gy + 0.5f) * ch
                        if ((cx - p.x) * (cx - p.x) + (cy - p.y) * (cy - p.y) < brush * brush) {
                            cells[idx] = true
                            scratched++
                        }
                    }
                    // A tick every few freshly-cleared cells feels like scraping foil.
                    if (scratched - lastTickCells >= 3) {
                        lastTickCells = scratched
                        onScratch()
                    }
                    if (scratched >= cells.size * REVEAL_AT) onRevealThreshold()
                }
                detectDragGestures(
                    onDragStart = { p -> current = listOf(p); mark(p) },
                    onDragEnd = { strokes += current; current = emptyList() },
                    onDragCancel = { strokes += current; current = emptyList() }
                ) { change, _ ->
                    current = current + change.position
                    mark(change.position)
                }
            }
    ) {
        // Holographic foil: a moving rainbow sheen over silver, tinted by category.
        val w = size.width
        drawRect(Brush.linearGradient(listOf(Color(0xFFB8BCC6), Color(0xFFE6E9EF), Color(0xFFA9AEBA))))
        val x0 = -w + shimmer * w * 2f
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, accent.copy(alpha = 0.45f), Color(0xFFFF80AB).copy(alpha = 0.35f), Color(0xFF80D8FF).copy(alpha = 0.4f), Color.Transparent),
                start = Offset(x0, 0f), end = Offset(x0 + w, size.height)
            )
        )
        // Sparkle dots.
        val rnd = Random(7)
        repeat(26) {
            drawCircle(Color.White.copy(alpha = 0.55f), radius = 1.2.dp.toPx() + rnd.nextFloat() * 1.5f, center = Offset(rnd.nextFloat() * w, rnd.nextFloat() * size.height))
        }
        val text = textMeasurer.measure(label, TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, color = Color(0xFF4A4F5C)))
        drawText(text, topLeft = Offset((w - text.size.width) / 2f, (size.height - text.size.height) / 2f))

        // Erase where the finger went.
        val stroke = Stroke(width = 52.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        (strokes + listOf(current)).forEach { pts ->
            if (pts.isEmpty()) return@forEach
            if (pts.size == 1) {
                drawCircle(Color.Black, radius = stroke.width / 2f, center = pts[0], blendMode = BlendMode.Clear)
            } else {
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                }
                drawPath(path, Color.Black, style = stroke, blendMode = BlendMode.Clear)
            }
        }
    }
}

/** A quick confetti pop when the fact is revealed. */
@Composable
private fun ConfettiBurst(key: Int, accent: Color, modifier: Modifier) {
    if (key == 0) return
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { progress.animateTo(1f, tween(900)) }
    val colors = listOf(accent, Color(0xFFFFD54F), Color(0xFFFF80AB), Color(0xFF80D8FF), Color.White)
    val pieces = remember(key) {
        val r = Random(key)
        List(28) { Triple(r.nextFloat() * 2f * PI.toFloat(), 0.4f + r.nextFloat() * 0.6f, colors[it % colors.size]) }
    }
    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val c = Offset(size.width / 2f, size.height / 2f)
        pieces.forEach { (angle, speed, color) ->
            val d = size.minDimension * 0.9f * speed * t
            val gravity = size.height * 0.5f * t * t
            val p = Offset(c.x + cos(angle) * d, c.y + sin(angle) * d + gravity)
            drawRect(color.copy(alpha = 1f - t), topLeft = p, size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 3.dp.toPx()))
        }
    }
}

@Composable
private fun ReactionRow(reacted: Boolean, learned: Int, knew: Int, onReact: (Boolean) -> Unit) {
    if (reacted) {
        Text(
            stringResource(R.string.fact_reacted_summary, learned, knew),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        BouncyChoice(stringResource(R.string.fact_react_new), Modifier.weight(1f)) { onReact(true) }
        BouncyChoice(stringResource(R.string.fact_react_knew), Modifier.weight(1f)) { onReact(false) }
    }
}

@Composable
private fun BouncyChoice(text: String, modifier: Modifier, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    FilledTonalButton(
        onClick = {
            scope.launch {
                scale.animateTo(0.88f, tween(70))
                scale.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
            }
            onClick()
        },
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.scale(scale.value)
    ) { Text(text, fontWeight = FontWeight.Bold, maxLines = 1) }
}

@Composable
private fun FactActions(fact: PokemonFact, onAnother: () -> Unit, onBackToToday: (() -> Unit)?) {
    val context = LocalContext.current
    val shareText = stringResource(R.string.fact_share_message, fact.fact, "https://play.google.com/store/apps/details?id=${context.packageName}")
    val chooser = stringResource(R.string.fact_share)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onAnother) {
            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.fact_another), fontWeight = FontWeight.Bold)
        }
        onBackToToday?.let {
            TextButton(onClick = it) { Text(stringResource(R.string.fact_back_to_today)) }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(send, chooser))
        }) {
            Icon(Icons.Default.Share, contentDescription = chooser, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
