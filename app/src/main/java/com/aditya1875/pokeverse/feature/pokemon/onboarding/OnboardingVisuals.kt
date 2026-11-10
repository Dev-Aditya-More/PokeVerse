package com.aditya1875.pokeverse.feature.pokemon.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.pokemon.dex.PokeBallIcon
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import kotlinx.coroutines.delay

/**
 * A Pokémon's artwork that starts as a black silhouette and, once [reveal] is true, fades into the
 * real colours: the "it's registered now" moment. Never reveals when [reveal] stays false.
 */
@Composable
fun RevealArt(url: String, name: String, reveal: Boolean, modifier: Modifier = Modifier) {
    val t by animateFloatAsState(
        targetValue = if (reveal) 1f else 0f,
        animationSpec = tween(900, delayMillis = 250),
        label = "reveal",
    )
    Box(modifier) {
        AsyncImage(
            model = url,
            contentDescription = name,
            colorFilter = ColorFilter.tint(Color.Black, androidx.compose.ui.graphics.BlendMode.SrcAtop),
            modifier = Modifier.matchParentSize().graphicsLayer { alpha = 1f - t },
        )
        AsyncImage(
            model = url,
            contentDescription = name,
            modifier = Modifier.matchParentSize().graphicsLayer { alpha = t },
        )
    }
}

/** The mini-visuals that sit under Professor Dex during the feature tour. */

private val Glass = Color.White.copy(alpha = 0.06f)
private val GlassEdge = Color.White.copy(alpha = 0.14f)

private fun Modifier.glassCard(radius: Int = 20): Modifier =
    clip(RoundedCornerShape(radius.dp))
        .background(Glass)
        .border(BorderStroke(1.dp, GlassEdge), RoundedCornerShape(radius.dp))

/** Springs 0 → 1 shortly after the visual appears; [delayMs] staggers siblings. */
@Composable
private fun rememberPop(delayMs: Int = 0): Float {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        go = true
    }
    val v by animateFloatAsState(
        targetValue = if (go) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "pop",
    )
    return v
}

/** Pokédex: the starter, already caught, next to Seen / Caught bars that fill in. */
@Composable
fun TourDexVisual(starter: Starter, assetsEnabled: Boolean, accent: Color, modifier: Modifier = Modifier) {
    val pop = rememberPop()
    val seen = remember { Animatable(0f) }
    val caught = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(250)
        seen.animateTo(0.34f, tween(900))
    }
    LaunchedEffect(Unit) {
        delay(550)
        caught.animateTo(0.12f, tween(900))
    }
    Row(
        modifier = modifier.fillMaxWidth().glassCard().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            RevealArt(
                url = starter.artworkUrl,
                name = starter.displayName,
                reveal = assetsEnabled,
                modifier = Modifier.size(92.dp).scale(0.5f + 0.5f * pop),
            )
            PokeBallIcon(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = pop
                        scaleY = pop
                    },
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DexBar(stringResource(R.string.pro_tour_seen), seen.value, accent)
            DexBar(stringResource(R.string.pro_tour_caught), caught.value, Color(0xFFEE3B33))
        }
    }
}

@Composable
private fun DexBar(label: String, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(color))
        }
    }
}

/** Mini games: four tiles that spring in one after another. */
@Composable
fun TourGamesVisual(modifier: Modifier = Modifier) {
    data class Tile(val icon: ImageVector, val title: Int, val color: Color)

    val tiles = listOf(
        Tile(Icons.Rounded.QuestionMark, R.string.game_name_pokeguess_title, Color(0xFF7C4DFF)),
        Tile(Icons.Rounded.Quiz, R.string.game_name_pokequiz_title, Color(0xFF29C6E0)),
        Tile(Icons.Rounded.Style, R.string.game_name_pokematch_title, Color(0xFFFFB300)),
        Tile(Icons.Rounded.DirectionsRun, R.string.game_name_chase_title, Color(0xFFE05A29)),
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { col, tile ->
                    val pop = rememberPop(delayMs = (row * 2 + col) * 120)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = pop
                                scaleY = pop
                                alpha = pop.coerceIn(0f, 1f)
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(tile.color.copy(alpha = 0.16f))
                            .border(BorderStroke(1.dp, tile.color.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(tile.icon, contentDescription = null, tint = tile.color, modifier = Modifier.size(24.dp))
                        Text(
                            stringResource(tile.title),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Leaderboard: a podium rising with the player's own name on top. */
@Composable
fun TourRankVisual(name: String, accent: Color, modifier: Modifier = Modifier) {
    val heights = listOf(64.dp, 96.dp, 46.dp)
    val ranks = listOf("2", "1", "3")
    Row(
        modifier = modifier.fillMaxWidth().glassCard().padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { i, h ->
            val pop = rememberPop(delayMs = if (i == 1) 350 else i * 120)
            val first = i == 1
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(78.dp),
            ) {
                if (first) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD66B),
                        modifier = Modifier.size(22.dp).graphicsLayer {
                            scaleX = pop
                            scaleY = pop
                        },
                    )
                    Text(
                        name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(h * pop.coerceAtLeast(0.01f))
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(if (first) accent else accent.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        ranks[i],
                        color = if (first) Color.Black.copy(alpha = 0.75f) else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

/** Themes, streak and coins: swatches that take turns being selected, with a streak and wallet beneath. */
@Composable
fun TourStyleVisual(modifier: Modifier = Modifier) {
    val swatches = remember {
        listOf("fire", "water", "grass", "electric", "psychic").map { pokemonTypeColor(it) }
    }
    var selected by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            selected = (selected + 1) % swatches.size
        }
    }
    Column(
        modifier = modifier.fillMaxWidth().glassCard().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            swatches.forEachIndexed { i, color ->
                val on = i == selected
                val s by animateFloatAsState(
                    if (on) 1.3f else 1f,
                    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                    label = "swatch",
                )
                Box(
                    Modifier
                        .size(34.dp)
                        .scale(s)
                        .clip(CircleShape)
                        .background(color)
                        .border(BorderStroke(if (on) 3.dp else 0.dp, Color.White), CircleShape),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFFF7043), modifier = Modifier.size(26.dp))
                Text("7", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.MonetizationOn, null, tint = Color(0xFFFFD66B), modifier = Modifier.size(26.dp))
                Text("120", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(0.dp))
    }
}
