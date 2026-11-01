package com.aditya1875.pokeverse.feature.leaderboard.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.utils.rememberDecorativeMotionEnabled

/*
 * Premium Showcase: how Dexverse Pro trainers stand out on the leaderboard.
 *
 * Goal: free players notice Pro trainers and want the look, while their own rows stay
 * exactly as before (nothing is taken away, nothing pops up uninvited). The only upsell
 * is a small sheet that opens when someone taps a PRO chip themselves.
 *
 * Performance: ONE animation clock for the whole screen ([ProvidePremiumShine]). Every
 * effect reads it inside a draw lambda, so a tick redraws the few premium rows without
 * recomposing anything, and the clock holds still with animations off / Battery Saver.
 */

val PremiumGoldLight = Color(0xFFFFE082)
val PremiumGold = Color(0xFFFFC107)
val PremiumGoldDeep = Color(0xFFFF8F00)
private val PremiumInk = Color(0xFF3E2A00)

/** 0..1 phase of the shared shine loop (frozen at a pleasant still frame when motion is off). */
class PremiumShine(private val phaseState: State<Float>) {
    val phase: Float get() = phaseState.value
}

private val LocalPremiumShine = staticCompositionLocalOf { PremiumShine(mutableFloatStateOf(0.35f)) }

@Composable
fun ProvidePremiumShine(content: @Composable () -> Unit) {
    val motion = rememberDecorativeMotionEnabled()
    val phase = if (motion) {
        rememberInfiniteTransition(label = "premiumShine").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
            label = "phase"
        )
    } else remember { mutableFloatStateOf(0.35f) }
    val shine = remember(phase) { PremiumShine(phase) }
    CompositionLocalProvider(LocalPremiumShine provides shine, content = content)
}

/** Name text in warm gold with a light band sweeping across the letters every few seconds. */
@Composable
fun Modifier.premiumNameShimmer(): Modifier {
    val shine = LocalPremiumShine.current
    return this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            // Band travels from well left of the text to well right of it; idle most of the loop.
            val t = shine.phase * 2.2f - 0.6f
            val cx = size.width * t
            val band = size.height * 2.2f
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent),
                    start = Offset(cx - band, 0f),
                    end = Offset(cx + band, size.height)
                ),
                blendMode = BlendMode.SrcAtop // only lights the glyph pixels
            )
        }
}

/** Gold the name is drawn in, tuned per theme so it stays readable. */
@Composable
fun premiumNameColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) PremiumGoldLight else Color(0xFF9A6B00)

/** Soft gold wash fading across the row, plus a slim gold accent bar on the leading edge. */
@Composable
fun Modifier.premiumRowGlow(): Modifier {
    val shine = LocalPremiumShine.current
    return drawBehind {
        drawRect(
            Brush.horizontalGradient(
                0f to PremiumGold.copy(alpha = 0.16f),
                0.55f to PremiumGold.copy(alpha = 0.05f),
                1f to Color.Transparent
            )
        )
        // The accent bar breathes gently with the shared clock.
        val pulse = 0.65f + 0.35f * kotlin.math.sin(shine.phase * 2f * Math.PI.toFloat()).let { (it + 1f) / 2f }
        val barW = 3.dp.toPx()
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(PremiumGoldLight, PremiumGoldDeep)),
            topLeft = Offset(0f, size.height * 0.18f),
            size = Size(barW, size.height * 0.64f),
            cornerRadius = CornerRadius(barW / 2),
            alpha = pulse
        )
    }
}

/** A gold ring that slowly turns around the avatar. */
@Composable
fun Modifier.premiumRing(width: Dp = 2.5.dp): Modifier {
    val shine = LocalPremiumShine.current
    return drawBehind {
        val stroke = width.toPx()
        rotate(shine.phase * 360f) {
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(PremiumGoldLight, PremiumGoldDeep, Color(0xFFFFF8E1), PremiumGold, PremiumGoldLight),
                    center = center
                ),
                radius = size.minDimension / 2 - stroke / 2,
                style = Stroke(stroke)
            )
        }
    }
}

/** Warm glow breathing behind a podium avatar (drawn outside the avatar's bounds). */
@Composable
fun Modifier.premiumAura(): Modifier {
    val shine = LocalPremiumShine.current
    return drawBehind {
        val breathe = (kotlin.math.sin(shine.phase * 4f * Math.PI.toFloat()) + 1f) / 2f
        val r = size.minDimension * (0.78f + 0.08f * breathe)
        drawCircle(
            brush = Brush.radialGradient(
                0.45f to PremiumGold.copy(alpha = 0.42f + 0.18f * breathe),
                1f to Color.Transparent,
                center = center, radius = r
            ),
            radius = r
        )
    }
}

/** The small PRO pill next to a premium trainer's name. Tapping it explains what Pro is. */
@Composable
fun ProChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(PremiumGoldLight, PremiumGold, PremiumGoldDeep)))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.leaderboard_pro_chip),
            color = PremiumInk,
            // No font padding: the glyphs alone set the pill's height, so it sits like a badge
            // next to the name instead of a button.
            style = TextStyle(
                fontSize = 8.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

/** Gold crown drawn over the #1 podium spot (replaces the old emoji). */
@Composable
fun PodiumCrown(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 30.dp, height = 22.dp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val crown = Path().apply {
                    moveTo(0.06f * w, 0.92f * h)
                    lineTo(0f, 0.28f * h)
                    lineTo(0.3f * w, 0.55f * h)
                    lineTo(0.5f * w, 0.02f * h)
                    lineTo(0.7f * w, 0.55f * h)
                    lineTo(w, 0.28f * h)
                    lineTo(0.94f * w, 0.92f * h)
                    close()
                }
                drawPath(crown, Brush.verticalGradient(listOf(PremiumGoldLight, PremiumGoldDeep)))
                drawPath(crown, PremiumInk.copy(alpha = 0.35f), style = Stroke(1.dp.toPx()))
                listOf(0f to 0.28f, 0.5f to 0.02f, 1f to 0.28f).forEach { (x, y) ->
                    drawCircle(PremiumGoldLight, radius = 2.2.dp.toPx(), center = Offset(x * w, y * h))
                }
            }
    )
}

/**
 * Opens when anyone taps a PRO chip. Free viewers see what Pro adds and a single, optional
 * button to the plans; Pro viewers just see their own perks celebrated. Never shown unprompted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProTrainerSheet(
    trainerName: String,
    viewerIsPremium: Boolean,
    onSeePlans: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .premiumAura()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PremiumGoldLight, PremiumGoldDeep))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WorkspacePremium, null, tint = PremiumInk, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.leaderboard_pro_sheet_title, trainerName.split(" ").firstOrNull().orEmpty()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    if (viewerIsPremium) R.string.leaderboard_pro_sheet_body_member
                    else R.string.leaderboard_pro_sheet_body
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PerkRow(Icons.Default.AutoAwesome, stringResource(R.string.leaderboard_pro_perk_flair))
                PerkRow(Icons.Default.Palette, stringResource(R.string.leaderboard_pro_perk_looks))
                PerkRow(Icons.Default.Block, stringResource(R.string.leaderboard_pro_perk_ads))
            }
            Spacer(Modifier.height(22.dp))
            if (viewerIsPremium) {
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.leaderboard_pro_sheet_member_cta))
                }
            } else {
                Button(
                    onClick = onSeePlans,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, contentColor = PremiumInk)
                ) {
                    Text(stringResource(R.string.leaderboard_pro_sheet_cta), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.leaderboard_pro_sheet_later))
                }
            }
        }
    }
}

@Composable
private fun PerkRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(PremiumGold.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = PremiumGoldDeep, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
