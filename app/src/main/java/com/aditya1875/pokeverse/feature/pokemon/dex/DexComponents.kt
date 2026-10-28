package com.aditya1875.pokeverse.feature.pokemon.dex

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import java.text.NumberFormat

private val BallRed = Color(0xFFE53935)
private val BallWhite = Color(0xFFF5F5F5)
private val BallInk = Color(0xFF212121)

/** A tiny Poké Ball, drawn rather than shipped as an asset, used as the "caught" mark. */
@Composable
fun PokeBallIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val d = size.minDimension
        val r = d / 2f
        val c = Offset(r, r)
        val band = d * 0.1f
        // Top half red, bottom half white.
        drawArc(BallRed, 180f, 180f, useCenter = true, topLeft = Offset.Zero, size = Size(d, d))
        drawArc(BallWhite, 0f, 180f, useCenter = true, topLeft = Offset.Zero, size = Size(d, d))
        drawRect(BallInk, topLeft = Offset(0f, r - band / 2f), size = Size(d, band))
        drawCircle(BallInk, radius = d * 0.2f, center = c)
        drawCircle(BallWhite, radius = d * 0.11f, center = c)
        drawCircle(BallInk, radius = r - d * 0.03f, center = c, style = Stroke(d * 0.07f))
    }
}

/** Seen / caught out of the whole national dex, as two progress bars. */
@Composable
fun DexProgressStrip(
    progress: DexProgress,
    modifier: Modifier = Modifier,
    onPlayWildCatch: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PokeBallIcon(Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.dex_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            DexBar(stringResource(R.string.dex_seen), progress.seenCount, DexProgress.TOTAL, MaterialTheme.colorScheme.primary)
            DexBar(stringResource(R.string.dex_caught), progress.caughtCount, DexProgress.CATCHABLE, BallRed)
            // Caught only comes from Wild Catch, so say so (and offer the shortcut).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.dex_catch_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (onPlayWildCatch != null) {
                    TextButton(onClick = onPlayWildCatch) {
                        Text(stringResource(R.string.dex_play), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DexBar(label: String, count: Int, total: Int, color: Color) {
    val fraction by animateFloatAsState(
        targetValue = (count.toFloat() / total).coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "dexBar",
    )
    val nf = NumberFormat.getIntegerInstance()
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${nf.format(count)} / ${nf.format(total)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            if (fraction > 0f) {
                Spacer(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        }
    }
}

/** "New Pokédex entry registered!" toast that slides in from the top of the detail page. */
@Composable
fun DexEntryBanner(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFF101626),
            shadowElevation = 8.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PokeBallIcon(Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.dex_new_entry),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** "Trainer's party": every member of a full team was caught in Wild Catch. Colours are passed in so it sits right on light and dark cards. */
@Composable
fun PartyBadge(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PokeBallIcon(Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.dex_party_badge),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content,
        )
    }
}
