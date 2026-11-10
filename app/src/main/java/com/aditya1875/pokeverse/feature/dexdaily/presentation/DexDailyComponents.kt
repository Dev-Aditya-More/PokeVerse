package com.aditya1875.pokeverse.feature.dexdaily.presentation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyCalendar
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyRecord
import com.aditya1875.pokeverse.utils.rememberDecorativeMotionEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val DexGreen = Color(0xFF43A047)
internal val DexRed = Color(0xFFE53935)
private val ShadowInk = Color(0xFF15101C)
private val StageCenter = Color(0xFFFFF3C4)
private val StageMid = Color(0xFFFFC94D)

/**
 * The classic "who's that" stage: a bright sunburst so a black silhouette reads in light and
 * dark themes alike. Rays turn slowly (held still with animations off / Battery Saver).
 * [revealed] cross-fades the real artwork in over the shadow.
 */
@Composable
fun ShadowStage(
    artworkUrl: String,
    revealed: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    animate: Boolean = true,
) {
    val motion = rememberDecorativeMotionEnabled() && animate
    val spin = if (motion) {
        rememberInfiniteTransition(label = "rays").animateFloat(
            0f, 360f, infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart), label = "spin"
        )
    } else remember { mutableFloatStateOf(0f) }
    val reveal = remember { Animatable(if (revealed) 1f else 0f) }
    LaunchedEffect(revealed) { reveal.animateTo(if (revealed) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 220f)) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(StageCenter, StageMid, Color(0xFFFF8F00)), center, this.size.minDimension / 1.4f))
                rotate(spin.value) {
                    val r = this.size.maxDimension
                    for (i in 0 until 14) {
                        val a0 = (i * 2 * PI / 14).toFloat()
                        val a1 = a0 + (PI / 14).toFloat()
                        drawPath(
                            Path().apply {
                                moveTo(center.x, center.y)
                                lineTo(center.x + r * cos(a0), center.y + r * sin(a0))
                                lineTo(center.x + r * cos(a1), center.y + r * sin(a1))
                                close()
                            },
                            Color.White.copy(alpha = 0.16f)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val request = remember(artworkUrl) { ImageRequest.Builder(context).data(artworkUrl).build() }
        val pop = 1f + 0.08f * reveal.value * (1f - reveal.value) * 4f
        AsyncImage(
            model = request,
            contentDescription = null,
            colorFilter = ColorFilter.tint(ShadowInk),
            modifier = Modifier
                .fillMaxSize(0.8f)
                .graphicsLayer { alpha = 1f - reveal.value.coerceIn(0f, 1f) }
        )
        if (reveal.value > 0.01f) {
            AsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .graphicsLayer { alpha = reveal.value.coerceIn(0f, 1f); scaleX = pop; scaleY = pop }
            )
        }
    }
}

/** Round check / cross for a finished puzzle (share card, sheet header). */
@Composable
fun ResultBadge(solved: Boolean, size: Dp = 28.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (solved) DexGreen else DexRed),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (solved) Icons.Default.Check else Icons.Default.Close, null,
            tint = Color.White, modifier = Modifier.size(size * 0.62f)
        )
    }
}

@Composable
fun StreakChip(streak: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFFF7043).copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF7043), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            stringResource(R.string.dex_daily_streak, streak),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF7043),
            maxLines = 1,
            softWrap = false
        )
    }
}

/** "5h 12m", refreshed every 30 s. */
@Composable
fun rememberNextPuzzleCountdown(): String {
    val left = remember { mutableLongStateOf(DexDailyCalendar.msUntilNextPuzzle()) }
    LaunchedEffect(Unit) {
        while (true) {
            left.longValue = DexDailyCalendar.msUntilNextPuzzle()
            delay(30_000)
        }
    }
    val totalMin = (left.longValue / 60_000).coerceAtLeast(1)
    return if (totalMin >= 60) "${totalMin / 60}h ${totalMin % 60}m" else "${totalMin}m"
}

/**
 * The image players share: today's shadow (never the answer), their result and streak.
 * Drawn off-screen into [layer] by the caller, then saved as a PNG.
 */
@Composable
fun DexDailyShareCard(
    number: Int,
    artworkUrl: String,
    record: DexDailyRecord,
    streak: Int,
    modifier: Modifier = Modifier,
) {
    val accent = if (record.solved) Color(0xFF69F0AE) else Color(0xFFFF8A80)
    val accentDeep = if (record.solved) DexGreen else DexRed
    Box(
        modifier = modifier
            .width(320.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(
                1.5.dp,
                Brush.verticalGradient(listOf(StageMid.copy(alpha = 0.9f), accent.copy(alpha = 0.25f), StageMid.copy(alpha = 0.5f))),
                RoundedCornerShape(28.dp)
            )
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF3A2160), Color(0xFF181028), Color(0xFF0B0712))))
                // A glow in the result's colour behind the stage.
                drawCircle(
                    Brush.radialGradient(
                        listOf(accentDeep.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.36f),
                        radius = size.width * 0.8f
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width / 2f, size.height * 0.36f)
                )
                // A scatter of stars; positions are fixed so the card looks the same every time.
                var seed = 7L
                repeat(46) {
                    seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                    val x = (seed % 1000) / 1000f * size.width
                    seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                    val y = (seed % 1000) / 1000f * size.height
                    drawCircle(Color.White.copy(alpha = 0.10f + (seed % 5) * 0.05f), radius = 1.2f + (seed % 3), center = Offset(x, y))
                }
                // A big faint Poké Ball bleeding off the bottom corner.
                clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height) {
                    val bc = Offset(size.width * 0.92f, size.height * 0.97f)
                    val br = size.width * 0.34f
                    drawCircle(Color.White.copy(alpha = 0.05f), br, bc)
                    drawCircle(Color.Black.copy(alpha = 0.18f), br, bc, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f))
                    drawLine(Color.Black.copy(alpha = 0.18f), Offset(bc.x - br, bc.y), Offset(bc.x + br, bc.y), strokeWidth = 10f)
                    drawCircle(Color.Black.copy(alpha = 0.18f), br * 0.22f, bc, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f))
                }
            }
            .padding(horizontal = 22.dp, vertical = 22.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("DEXVERSE", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 13.sp)
                Text(
                    stringResource(R.string.dex_daily_label, number),
                    color = Color(0xFF1B1226), fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(StageMid)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
            // The stage sits in a glowing ring.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(222.dp)
                    .border(
                        3.dp,
                        Brush.sweepGradient(listOf(StageMid, accent, StageCenter, accent, StageMid)),
                        CircleShape
                    )
                    .padding(8.dp)
            ) {
                ShadowStage(artworkUrl = artworkUrl, revealed = false, size = 200.dp, animate = false)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                stringResource(if (record.solved) R.string.dex_daily_got_it else R.string.dex_daily_missed).uppercase(),
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(
                        if (record.solved) listOf(Color(0xFFB9F6CA), Color(0xFFFFF59D), Color(0xFF69F0AE))
                        else listOf(Color(0xFFFFCDD2), Color(0xFFFF8A80))
                    ),
                    fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 2.sp
                )
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (record.solved) {
                    Text(
                        stringResource(if (record.hintsUsed) R.string.dex_daily_with_hints else R.string.dex_daily_no_hints),
                        color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (streak > 1) StreakChip(streak)
            }
            Spacer(Modifier.height(22.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.14f)))
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.dex_daily_share_footer),
                color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
            Text("dexverse", color = StageMid, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
        }
    }
}

/** Saves the recorded card and opens the share sheet with an image + a short line of text. */
suspend fun shareDexDaily(context: Context, layer: GraphicsLayer, number: Int, record: DexDailyRecord) {
    val bitmap = layer.toImageBitmap().asAndroidBitmap()
    val file = withContext(Dispatchers.IO) {
        File(context.cacheDir, "dex_daily_$number.png").also { f ->
            FileOutputStream(f).use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val score = when {
        !record.solved -> context.getString(R.string.dex_daily_missed)
        record.hintsUsed -> context.getString(R.string.dex_daily_got_it) + " · " + context.getString(R.string.dex_daily_with_hints)
        else -> context.getString(R.string.dex_daily_got_it) + " · " + context.getString(R.string.dex_daily_no_hints)
    }
    val text = context.getString(
        R.string.dex_daily_share_text, number, score,
        "https://play.google.com/store/apps/details?id=${context.packageName}"
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.dex_daily_share_chooser)))
}

