package com.aditya1875.pokeverse.feature.team.presentation.components

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

// Pokémon-region-inspired seed names for Picsum photos.
// Each seed consistently maps to the same high-quality landscape photo
// from Picsum's Unsplash-backed collection.
private val bgSeeds = listOf(
    "pallet-town-viridian",
    "cerulean-ocean-depths",
    "mt-moon-celestial-night",
    "lavender-mystic-fog",
    "celadon-emerald-forest",
    "cinnabar-volcanic-glow",
    "saffron-golden-horizon",
    "fuschia-wild-safari",
    "vermillion-harbor-dawn",
    "pewter-stone-highlands"
)

private fun teamBackgroundUrl(teamName: String): String {
    val seed = bgSeeds[abs(teamName.hashCode()) % bgSeeds.size]
    return "https://picsum.photos/seed/$seed/640/400"
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
                modifier = Modifier.drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer)
                }
            )

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
                                    Intent.createChooser(shareIntent, "Share Team Card")
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
    modifier: Modifier = Modifier
) {
    val backgroundUrl = remember(teamName) { teamBackgroundUrl(teamName) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Layer 1 — landscape photo background
        AsyncImage(
            model = backgroundUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Layer 2 — dark overlay: preserves background visibility while keeping all text legible
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF0A0E1A).copy(alpha = 0.88f),
                            0.35f to Color(0xFF0A0E1A).copy(alpha = 0.55f),
                            0.70f to Color(0xFF0A0E1A).copy(alpha = 0.60f),
                            1.00f to Color(0xFF0A0E1A).copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Layer 3 — card content
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
                        color = Color(0xFFE94560),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Trainer Card",
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
                        text = "Lv.$trainerLevel  •  $totalXp XP",
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

            // Pokémon grid — 2 columns
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
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
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
                modifier = Modifier.size(64.dp)
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
