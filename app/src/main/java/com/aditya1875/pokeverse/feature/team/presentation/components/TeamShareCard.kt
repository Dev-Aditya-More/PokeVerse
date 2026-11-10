package com.aditya1875.pokeverse.feature.team.presentation.components

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.team.data.local.entity.TeamMemberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Drawn backgrounds for the share card. They are painted, not downloaded, so the card is always sharp,
 * works offline and never depends on a photo service. Each is a sky, a glow, optional stars and two
 * layers of hills; the team name picks one by default and the dialog lets the trainer change it.
 */
private class ShareTheme(
    val sky: List<Color>,
    val glow: Color,
    val ridges: List<Color>,
    val stars: Boolean,
    val accent: Color,
)

private val shareThemes = listOf(
    // Aurora night
    ShareTheme(listOf(Color(0xFF050B1F), Color(0xFF0B2A4A), Color(0xFF11635F)), Color(0xFF5CFFC8), listOf(Color(0xFF0A2236), Color(0xFF06141F)), true, Color(0xFF5CFFC8)),
    // Ember volcano
    ShareTheme(listOf(Color(0xFF1A0A12), Color(0xFF5A1620), Color(0xFFE8552B)), Color(0xFFFFB36B), listOf(Color(0xFF3A0F14), Color(0xFF1C0709)), false, Color(0xFFFF8A4C)),
    // Ocean depths
    ShareTheme(listOf(Color(0xFF031B34), Color(0xFF0A4B7A), Color(0xFF2FA3C8)), Color(0xFFBFF3FF), listOf(Color(0xFF0A3558), Color(0xFF062238)), false, Color(0xFF59D5FF)),
    // Emerald forest
    ShareTheme(listOf(Color(0xFF08140E), Color(0xFF14502F), Color(0xFF7BCB6F)), Color(0xFFE6FFB0), listOf(Color(0xFF0E3A22), Color(0xFF08210F)), false, Color(0xFFA5F28A)),
    // Electric storm
    ShareTheme(listOf(Color(0xFF0B0B1E), Color(0xFF2B2468), Color(0xFF7D5FFF)), Color(0xFFFFE45C), listOf(Color(0xFF1B1646), Color(0xFF0D0A26)), true, Color(0xFFFFE45C)),
    // Sakura dusk
    ShareTheme(listOf(Color(0xFF1E0F2B), Color(0xFF6B2B6B), Color(0xFFFF9EB5)), Color(0xFFFFE3EC), listOf(Color(0xFF4A1C4E), Color(0xFF260D2A)), false, Color(0xFFFF9EB5)),
    // Royal gold
    ShareTheme(listOf(Color(0xFF0D0A1F), Color(0xFF2C1B5A), Color(0xFFC9962B)), Color(0xFFFFE08A), listOf(Color(0xFF1B1238), Color(0xFF0A0716)), true, Color(0xFFFFD36B)),
    // Mystic fog
    ShareTheme(listOf(Color(0xFF0A0614), Color(0xFF3B1B6B), Color(0xFF9D5CFF)), Color(0xFFE0C7FF), listOf(Color(0xFF260F45), Color(0xFF0F0722)), true, Color(0xFFCDA6FF)),
)

private fun defaultThemeIndex(teamName: String): Int = abs(teamName.hashCode()) % shareThemes.size

/** Paints [theme] over the whole draw area. Everything is deterministic, so the card never changes between renders. */
private fun DrawScope.paintShareTheme(theme: ShareTheme) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(theme.sky))

    // Sun / moon glow, upper right.
    val gc = Offset(w * 0.78f, h * 0.22f)
    drawCircle(Brush.radialGradient(listOf(theme.glow.copy(alpha = 0.55f), Color.Transparent), gc, w * 0.55f), w * 0.55f, gc)
    drawCircle(theme.glow.copy(alpha = 0.9f), w * 0.045f, gc)

    // Soft light rays fanning from the glow.
    for (k in 0 until 7) {
        val a = Math.toRadians(100.0 + k * 14.0)
        val end = Offset(gc.x + (kotlin.math.cos(a) * w * 1.4f).toFloat(), gc.y + (kotlin.math.sin(a) * w * 1.4f).toFloat())
        drawLine(theme.glow.copy(alpha = 0.05f), gc, end, strokeWidth = w * 0.06f)
    }

    if (theme.stars) {
        var seed = 11L
        repeat(60) {
            seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
            val x = (seed % 1000) / 1000f * w
            seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
            val y = (seed % 1000) / 1000f * h * 0.7f
            drawCircle(Color.White.copy(alpha = 0.18f + (seed % 6) * 0.07f), 1f + (seed % 3) * 0.8f, Offset(x, y))
        }
    }

    // Two layers of hills, the far one lighter, with a faint rim of the glow colour along the top edge.
    theme.ridges.forEachIndexed { layer, color ->
        val base = h * (0.80f + layer * 0.08f)
        val amp = h * (0.07f - layer * 0.02f)
        val path = Path().apply {
            moveTo(0f, h)
            var x = 0f
            while (x <= w + 8f) {
                val y = base - amp * kotlin.math.sin(x / w * (5.0 + layer * 2.0) + layer * 1.7).toFloat() -
                    amp * 0.5f * kotlin.math.sin(x / w * (11.0 + layer) + 0.6).toFloat()
                lineTo(x, y)
                x += 8f
            }
            lineTo(w, h)
            close()
        }
        drawPath(path, color)
        drawPath(path, theme.glow.copy(alpha = 0.10f), style = Stroke(width = 2f))
    }
}

@Composable
fun TeamShareDialog(
    team: List<TeamMemberEntity>,
    teamName: String,
    trainerName: String,
    trainerLevel: Int,
    totalXp: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var isSharing by remember { mutableStateOf(false) }
    var themeIndex by remember(teamName) { mutableIntStateOf(defaultThemeIndex(teamName)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TeamShareCard(
                team = team,
                teamName = teamName,
                trainerName = trainerName,
                trainerLevel = trainerLevel,
                totalXp = totalXp,
                themeIndex = themeIndex,
                modifier = Modifier.drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer)
                }
            )

            // Pick a background: tap a swatch.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                shareThemes.forEachIndexed { index, t ->
                    val selected = index == themeIndex
                    Box(
                        modifier = Modifier
                            .size(if (selected) 34.dp else 28.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(t.sky))
                            .border(if (selected) 2.5.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable { themeIndex = index }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_close))
                }

                Button(
                    onClick = {
                        scope.launch {
                            isSharing = true
                            try {
                                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                val file = withContext(Dispatchers.IO) {
                                    File(context.cacheDir, "team_card_${System.currentTimeMillis()}.png")
                                        .also { f ->
                                            FileOutputStream(f).use { out ->
                                                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                                            }
                                        }
                                }
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, context.getString(R.string.team_share_card_chooser))
                                )
                                onDismiss()
                            } finally {
                                isSharing = false
                            }
                        }
                    },
                    enabled = !isSharing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.IosShare,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_share))
                    }
                }
            }
        }
    }
}

@Composable
fun TeamShareCard(
    team: List<TeamMemberEntity>,
    teamName: String,
    trainerName: String,
    trainerLevel: Int,
    totalXp: Int,
    modifier: Modifier = Modifier,
    themeIndex: Int = defaultThemeIndex(teamName),
) {
    val theme = shareThemes[themeIndex.coerceIn(0, shareThemes.lastIndex)]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .drawBehind { paintShareTheme(theme) }
    ) {
        // A light scrim keeps text legible while the scene still shows through.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF0A0E1A).copy(alpha = 0.55f),
                            0.30f to Color(0xFF0A0E1A).copy(alpha = 0.18f),
                            0.75f to Color(0xFF0A0E1A).copy(alpha = 0.22f),
                            1.00f to Color(0xFF0A0E1A).copy(alpha = 0.60f)
                        )
                    )
                )
        )

        // Layer 3, card content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "DEXVERSE",
                        color = theme.accent,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = stringResource(R.string.team_trainer_card),
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 10.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = trainerName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.team_card_level_xp, trainerLevel, totalXp),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            // Team name
            Text(
                text = teamName,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 0.5.sp
            )

            // Pokémon grid, 2 columns
            val rows = team.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { pokemon ->
                            PokemonShareCell(
                                pokemon = pokemon,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Watermark
            Spacer(Modifier.height(2.dp))
            Text(
                text = "dexverse.app",
                color = Color.White.copy(alpha = 0.28f),
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun PokemonShareCell(
    pokemon: TeamMemberEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AsyncImage(
                model = pokemon.imageUrl,
                contentDescription = pokemon.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(84.dp)
            )
            Text(
                text = pokemon.name.replaceFirstChar { it.uppercaseChar() },
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
