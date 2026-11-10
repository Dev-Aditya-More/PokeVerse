package com.aditya1875.pokeverse.feature.evolution.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.GpsFixed
import com.aditya1875.pokeverse.feature.core.ui.components.ShardGem
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import androidx.compose.material.icons.outlined.Info
import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionEconomy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.aditya1875.pokeverse.feature.core.ui.components.LoadErrorInline
import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgress
import com.aditya1875.pokeverse.feature.pokemon.dex.EvolveResult
import com.aditya1875.pokeverse.feature.pokemon.onboarding.RevealArt
import com.aditya1875.pokeverse.utils.Haptics
import com.aditya1875.pokeverse.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.math.cos
import kotlin.math.sin

private val ShardColor = Color(0xFF7DD3FC)

/**
 * "Evolve" card on a Pokémon's detail page. Only appears for a caught Pokémon that has a next form;
 * artwork stays a silhouette until Original Assets is on and the form is yours.
 */
@Composable
fun EvolveSection(
    speciesName: String,
    pokemonId: Int,
    artUrl: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onOpenShop: () -> Unit = {},
    viewModel: EvolutionViewModel = koinViewModel(),
) {
    // Alternate forms (Pikachu PhD, Mega forms...) have no evolution line of their own, so there is nothing to load.
    val hasEvolutionLine = pokemonId in 1..DexProgress.TOTAL
    LaunchedEffect(speciesName) { if (hasEvolutionLine) viewModel.load(speciesName, pokemonId, artUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!hasEvolutionLine) return

    // The cinematic outlives the card: once the old form is used up the card disappears (it is no longer on
    // the roster), but the evolution moment still has to play.
    state.moment?.let { moment ->
        EvolutionCinematic(
            moment = moment,
            accent = accent,
            onDone = { viewModel.momentShown() },
        )
    }

    // Nothing at all for a Pokémon you do not have (or have evolved away): not the card, and not an error row either.
    if (!state.caught) return
    if (state.error != null) {
        LoadErrorInline(onRetry = { viewModel.retry() }, kind = state.error, modifier = modifier)
        return
    }
    // A final form has nothing to offer here.
    if (state.loading || state.choices.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.06f))))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.35f)), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.evo_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ShardChip(state.wallet.balance)
        }
        Text(
            stringResource(R.string.evo_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        state.choices.forEach { choice ->
            // A stone or held item pays for the evolution outright; otherwise it costs shards.
            val affordable = if (choice.needsItem) choice.itemHeld > 0 else state.wallet.balance >= choice.shardCost
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Silhouette until you own the form AND have Original Assets on.
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), Color.White.copy(alpha = 0.10f)))),
                    contentAlignment = Alignment.Center,
                ) {
                    RevealArt(
                        url = choice.artUrl,
                        name = choice.toDisplayName,
                        reveal = choice.inDex,
                        modifier = Modifier.size(68.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (choice.inDex) choice.toDisplayName else stringResource(R.string.evo_unknown_form),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    val itemName = choice.item?.let { stringResource(it.name) }
                    Text(
                        if (itemName != null) stringResource(R.string.evo_needs_item, itemName, choice.itemHeld)
                        else stringResource(R.string.evo_cost, choice.shardCost),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (affordable || choice.alreadyHave) ShardColor
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
                when {
                    choice.alreadyHave -> Text(
                        stringResource(R.string.evo_in_roster),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    // No stone in the bag: the button takes you to buy one instead of sitting dead.
                    choice.needsItem && choice.itemHeld == 0 -> OutlinedButton(
                        onClick = onOpenShop,
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(stringResource(R.string.evo_get_item)) }
                    else -> Button(
                        onClick = { viewModel.evolve(choice) },
                        enabled = affordable && !state.busy,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(stringResource(R.string.evo_button)) }
                }
            }
        }

        val shardChoices = state.choices.filter { !it.needsItem && !it.alreadyHave }
        if (shardChoices.isNotEmpty() && state.wallet.balance < shardChoices.minOf { it.shardCost }) {
            Text(
                stringResource(R.string.evo_how_to_earn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }
        when (state.message) {
            EvolveResult.NotEnough -> Text(stringResource(R.string.evo_err_not_enough), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
            EvolveResult.AlreadyHave -> Text(stringResource(R.string.evo_err_have), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
            EvolveResult.NotCaught -> Text(stringResource(R.string.evo_err_not_caught), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
            EvolveResult.NoItem -> Text(stringResource(R.string.evo_err_no_item), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
            else -> {}
        }
    }
}

@Composable
fun ShardChip(balance: Int, modifier: Modifier = Modifier) {
    var help by remember { mutableStateOf(false) }
    if (help) ShardsHelpDialog(onDismiss = { help = false })
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(ShardColor.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, ShardColor.copy(alpha = 0.5f)), CircleShape)
            .padding(start = 12.dp, end = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(12.dp)) {
            val c = center
            val r = size.minDimension / 2
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(c.x, c.y - r); lineTo(c.x + r * 0.7f, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r * 0.7f, c.y); close()
            }
            drawPath(path, ShardColor)
        }
        Text(balance.toString(), color = ShardColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        HelpMark(onClick = { help = true })
    }
}

/** The small "?" that opens the shard guide. Big enough to tap, small enough to sit inline. */
@Composable
fun HelpMark(onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = ShardColor) {
    val label = stringResource(R.string.evo_help_button)
    // 32dp touch target around a soft filled 22dp disc, so it is easy to hit and reads as a button.
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .semantics { contentDescription = label; role = Role.Button }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Outlined.Info,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

/** "+N Evo Shards ?" for the result screens, with the daily-limit note when it applies. */
@Composable
fun ShardGainRow(gained: Int, capped: Boolean, modifier: Modifier = Modifier) {
    if (gained <= 0 && !capped) return
    var help by remember { mutableStateOf(false) }
    if (help) ShardsHelpDialog(onDismiss = { help = false })
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (gained > 0) ShardGem(14.dp, ShardColor)
        Text(
            text = if (gained > 0) stringResource(R.string.evo_shards_gained, gained)
            else stringResource(R.string.evo_capped),
            color = ShardColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
        )
        HelpMark(onClick = { help = true })
    }
}

@Composable
private fun HelpLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = ShardColor, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Plain-language guide: what shards are, how to earn them, the daily limit, where to spend them. */
@Composable
fun ShardsHelpDialog(onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.evo_help_got_it)) }
        },
        title = { Text(stringResource(R.string.evo_help_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.evo_help_what), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.evo_help_earn_title), fontWeight = FontWeight.Bold, color = ShardColor)
                HelpLine(Icons.Default.GpsFixed, stringResource(R.string.evo_help_earn_catch))
                HelpLine(Icons.Default.Shield, stringResource(R.string.evo_help_earn_battle))
                HelpLine(Icons.Default.Timer, stringResource(R.string.evo_help_cap, EvolutionEconomy.DAILY_CAP))
                HelpLine(Icons.Default.AutoAwesome, stringResource(R.string.evo_help_use, EvolutionEconomy.cost(1)))
            }
        },
    )
}

/**
 * The evolution moment, in the classic style: the old form glows and shudders, turns to a white
 * silhouette, then flickers between the old and new shapes faster and faster while energy streams in.
 * A flash, and the new form appears with a shockwave. No text plays during the transformation; the
 * result line only appears once it is done.
 */
@Composable
fun EvolutionCinematic(
    moment: EvolutionMoment,
    accent: Color,
    onDone: () -> Unit,
) {
    val sound: SoundManager = koinInject()
    val haptics: Haptics = koinInject()
    val done by rememberUpdatedState(onDone)

    val build = remember { Animatable(0f) }     // 0..1 over the whole build-up
    val flash = remember { Animatable(0f) }     // white-out
    val reveal = remember { Animatable(0f) }    // new form shown in colour
    val burst = remember { Animatable(0f) }     // shockwave ring + rays
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(moment) {
        haptics.play(Haptics.Haptic.CHARGE)
        build.animateTo(1f, tween(3400, easing = LinearEasing))
        flash.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        reveal.snapTo(1f)
        // The evolution jingle plays once the new form has appeared.
        sound.play(SoundManager.Sound.POKEMON_EVOLVE, 1f, haptic = false)
        haptics.play(Haptics.Haptic.LEVEL_UP)
        launch { burst.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
        flash.animateTo(0f, tween(700))
        delay(900)
        finished = true
    }

    Dialog(
        onDismissRequest = { if (finished) done() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = finished, dismissOnClickOutside = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF05060F).copy(alpha = 0.97f)),
            contentAlignment = Alignment.Center,
        ) {
            val t = build.value
            // Which silhouette is showing: the old form alone at first, then a flicker that accelerates.
            val flickerPhase = ((t - 0.28f) / 0.72f).coerceIn(0f, 1f)
            val flips = (flickerPhase * 2f + flickerPhase * flickerPhase * 22f).toInt()
            val showNew = t > 0.28f && flips % 2 == 1
            val whiteness = ((t - 0.1f) / 0.22f).coerceIn(0f, 1f)

            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height * 0.45f)
                val glowR = size.minDimension * (0.22f + 0.7f * t)
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.85f * t), Color.Transparent), c, glowR),
                    glowR, c,
                )
                // Energy streaming in towards the Pokémon while it builds.
                if (reveal.value < 1f) {
                    val n = 34
                    repeat(n) { i ->
                        val p = ((t * 1.6f + i / n.toFloat()) % 1f)
                        val ang = i * 2.399963f // golden angle, so the spread looks natural
                        val r = size.minDimension * 0.75f * (1f - p)
                        val pos = Offset(c.x + cos(ang) * r, c.y + sin(ang) * r)
                        drawCircle(Color.White.copy(alpha = 0.85f * p * t.coerceAtLeast(0.25f)), 2f + 5f * p, pos)
                    }
                }
                if (burst.value > 0f) {
                    drawCircle(
                        Color.White.copy(alpha = 0.6f * (1f - burst.value)),
                        radius = size.maxDimension * 0.65f * burst.value,
                        center = c,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f * (1f - burst.value) + 2f),
                    )
                    val n = 18
                    val len = size.maxDimension * burst.value
                    repeat(n) { i ->
                        val a = (Math.PI * 2 * i / n) + burst.value * 0.6
                        drawLine(
                            Color.White.copy(alpha = 0.6f * (1f - burst.value)),
                            c,
                            Offset(c.x + (cos(a) * len).toFloat(), c.y + (sin(a) * len).toFloat()),
                            strokeWidth = 6f + 10f * (1f - burst.value),
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(300.dp)) {
                    val shudder = if (reveal.value < 1f && t > 0.08f) sin(t * 160f) * 9f * t else 0f
                    val pulse = if (reveal.value < 1f) 1f + 0.10f * t + 0.04f * sin(t * 90f) else 1f
                    val silhouette = ColorFilter.tint(Color.White, androidx.compose.ui.graphics.BlendMode.SrcAtop)
                    // The old form: lights up to white as the energy builds.
                    AsyncImage(
                        model = moment.fromArt,
                        contentDescription = moment.fromName,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = whiteness), androidx.compose.ui.graphics.BlendMode.SrcAtop),
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            translationX = shudder; scaleX = pulse; scaleY = pulse
                            alpha = if (reveal.value >= 1f) 0f else if (flickerPhase > 0f && showNew) 0f else 1f
                        },
                    )
                    // The new form as a white silhouette, flickering against the old one.
                    AsyncImage(
                        model = moment.toArt,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = silhouette,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            translationX = shudder; scaleX = pulse; scaleY = pulse
                            alpha = if (reveal.value >= 1f) 0f else if (showNew) 1f else 0f
                        },
                    )
                    // The new form, revealed in colour (a silhouette unless Original Assets is on).
                    val revealed = Modifier.fillMaxSize().graphicsLayer {
                        alpha = reveal.value
                        val s = 0.75f + 0.25f * reveal.value
                        scaleX = s; scaleY = s
                    }
                    // Straight into full colour at the flash, with no dark in-between.
                    AsyncImage(model = moment.toArt, contentDescription = moment.toName, contentScale = ContentScale.Fit, modifier = revealed)
                }
                Spacer(Modifier.height(20.dp))
                // Nothing is written while it evolves; the result line fades in afterwards.
                Text(
                    text = stringResource(R.string.evo_result, moment.fromName, moment.toName),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .alpha(if (finished) 1f else reveal.value * burst.value.coerceIn(0f, 1f) * (1f - flash.value)),
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { done() },
                    enabled = finished,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.alpha(if (finished) 1f else 0f),
                ) { Text(stringResource(R.string.evo_continue)) }
            }

            // White-out layer.
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
        }
    }
}
