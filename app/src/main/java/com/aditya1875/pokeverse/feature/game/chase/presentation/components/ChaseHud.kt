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

/**
 * Score, lives, pause and the Thunderbolt button. Each value is a derived state,
 * so a frame only recomposes the piece whose displayed value actually changed.
 */
@Composable
fun ChaseHud(
    world: State<ChaseWorld>,
    onPause: () -> Unit,
    onThunderbolt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meters by remember { derivedStateOf { world.value.meters.toInt() } }
    val score by remember { derivedStateOf { world.value.score } }
    val lives by remember { derivedStateOf { world.value.lives } }
    val maxLives = world.value.maxLives
    // Quantised so the ring redraws in visible steps, not every frame.
    val charge by remember { derivedStateOf { (world.value.charge * 20).toInt() / 20f } }

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

        ThunderboltButton(
            charge = charge,
            onClick = onThunderbolt,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 8.dp)
        )
    }
}

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
            .background(if (ready) Thunder.copy(alpha = 0.9f) else HudPill)
            .clickable(enabled = ready, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = 5.dp.toPx()
            drawArc(Color.White.copy(alpha = 0.2f), 0f, 360f, false, style = Stroke(stroke))
            drawArc(Thunder, -90f, 360f * charge, false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Icon(
            Icons.Default.Bolt,
            contentDescription = stringResource(R.string.chase_thunderbolt),
            tint = if (ready) Color.Black else Thunder.copy(alpha = 0.7f),
            modifier = Modifier.size(30.dp)
        )
    }
}
