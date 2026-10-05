package com.aditya1875.pokeverse.feature.game.chase.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseWorld
import com.aditya1875.pokeverse.feature.game.core.presentation.LivesRow

private val HudPill = Color.Black.copy(alpha = 0.45f)
private val Thunder = Color(0xFFFFEB3B)
private val SnareRed = Color(0xFFE53935)

/** What the Agility button can do right now. */
enum class AgilityAvailability { Ready, Active, EarnWithAd, EarnFree, SpentForRun }

/**
 * Score, lives, pause, and the two powers: Thunderbolt (bottom-left, charged by
 * berries) and Agility (bottom-right, the lifeline). Each value is a derived
 * state, so a frame only recomposes the piece whose displayed value changed.
 */
@Composable
fun ChaseHud(
    world: State<ChaseWorld>,
    agilityMaxExtra: Int,
    isPremium: Boolean,
    onPause: () -> Unit,
    onThunderbolt: () -> Unit,
    onAgility: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meters by remember { derivedStateOf { world.value.meters.toInt() } }
    val score by remember { derivedStateOf { world.value.score } }
    val lives by remember { derivedStateOf { world.value.lives } }
    val maxLives = world.value.maxLives
    // Quantised so the rings redraw in visible steps, not every frame.
    val charge by remember { derivedStateOf { (world.value.charge * 20).toInt() / 20f } }
    val charges by remember { derivedStateOf { world.value.agilityCharges } }
    // Quantised so the ring redraws in visible steps, not every frame.
    val agilityLeft by remember { derivedStateOf { world.value.agility?.let { (1f - it.progress) * 40 }?.toInt()?.div(40f) } }
    val availability by remember(isPremium) {
        derivedStateOf {
            val w = world.value
            when {
                w.isAgile -> AgilityAvailability.Active
                w.agilityCharges > 0 -> AgilityAvailability.Ready
                w.agilityExtrasEarned >= agilityMaxExtra -> AgilityAvailability.SpentForRun
                isPremium -> AgilityAvailability.EarnFree
                else -> AgilityAvailability.EarnWithAd
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(HudPill)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    stringResource(R.string.chase_meters, meters),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    stringResource(R.string.chase_points, score),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            Box(Modifier.weight(1f))
            LivesRow(
                lives = lives,
                maxLives = maxLives,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(HudPill)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            IconButton(
                onClick = onPause,
                modifier = Modifier.clip(CircleShape).background(HudPill)
            ) {
                Icon(Icons.Default.Pause, contentDescription = stringResource(R.string.chase_paused), tint = Color.White)
            }
        }

        val strugglesLeft by remember { derivedStateOf { world.value.snare?.strugglesLeft } }
        strugglesLeft?.let { left ->
            SnarePrompt(
                strugglesLeft = left,
                modifier = Modifier.align(BiasAlignment(horizontalBias = 0f, verticalBias = 0.2f))
            )
        }

        ThunderboltButton(
            charge = charge,
            onClick = onThunderbolt,
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp)
        )

        AgilityButton(
            availability = availability,
            charges = charges,
            timeLeft = agilityLeft,
            onClick = onAgility,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 8.dp)
        )
    }
}

/** "Tap to break free" call-out while Pikachu is tangled in a net. Taps pass straight through it. */
@Composable
private fun SnarePrompt(strugglesLeft: Int, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "snare_pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(220), RepeatMode.Reverse),
        label = "scale"
    )
    Text(
        text = stringResource(R.string.chase_snare_prompt, strugglesLeft),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        color = Color.White,
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(SnareRed.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/** Thunderbolt: the ring fills as berries are collected; tap at full to clear the screen. */
@Composable
private fun ThunderboltButton(charge: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ready = charge >= 1f
    val pulse = rememberInfiniteTransition(label = "thunder_pulse")
    val glow by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (ready) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "glow"
    )
    Box(
        modifier = modifier
            .size(76.dp)
            .scale(glow)
            .clip(CircleShape)
            .background(if (ready) Thunder.copy(alpha = 0.92f) else HudPill)
            .clickable(enabled = ready, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = 5.dp.toPx()
            drawArc(Color.White.copy(alpha = 0.2f), 0f, 360f, false, style = Stroke(stroke))
            drawArc(Thunder, -90f, 360f * charge, false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🌩️", style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.chase_thunderbolt),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (ready) Color.Black else Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Agility lifeline. Glows when ready (with a charge count), shows a draining ring
 * while the dash runs, offers an ad (or a free use for premium) once spent, and
 * greys out when the run's extra uses are gone.
 */
@Composable
private fun AgilityButton(
    availability: AgilityAvailability,
    charges: Int,
    timeLeft: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ready = availability == AgilityAvailability.Ready
    val active = availability == AgilityAvailability.Active
    val pulse = rememberInfiniteTransition(label = "agility_pulse")
    val glow by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (ready || active) 1.1f else 1f,
        animationSpec = infiniteRepeatable(tween(if (active) 160 else 450), RepeatMode.Reverse),
        label = "glow"
    )
    val enabled = availability != AgilityAvailability.Active && availability != AgilityAvailability.SpentForRun

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(glow)
                .clip(CircleShape)
                .background(if (ready || active) Thunder.copy(alpha = 0.92f) else HudPill)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (active && timeLeft != null) {
                Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                    val stroke = 5.dp.toPx()
                    drawArc(Color.Black.copy(alpha = 0.15f), 0f, 360f, false, style = Stroke(stroke))
                    drawArc(Color.Black.copy(alpha = 0.7f), -90f, 360f * timeLeft, false, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = stringResource(R.string.chase_agility),
                    tint = when {
                        ready || active -> Color.Black
                        availability == AgilityAvailability.SpentForRun -> Color.White.copy(alpha = 0.3f)
                        else -> Thunder.copy(alpha = 0.8f)
                    },
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = stringResource(R.string.chase_agility),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (ready || active) Color.Black else Color.White.copy(alpha = 0.7f)
                )
            }
        }
        // Corner badge: charges left, or how to get more.
        val badge = when (availability) {
            AgilityAvailability.Ready -> "×$charges"
            AgilityAvailability.EarnWithAd -> "AD"
            AgilityAvailability.EarnFree -> "+1"
            else -> null
        }
        badge?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(50))
                    .background(if (availability == AgilityAvailability.EarnWithAd) SnareRed else Color(0xFF263238))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
