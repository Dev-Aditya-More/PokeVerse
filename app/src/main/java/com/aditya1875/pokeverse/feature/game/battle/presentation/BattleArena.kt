package com.aditya1875.pokeverse.feature.game.battle.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.fillMaxHeight
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineBattleRules
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEngine
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEvent
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleMath
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleState
import com.aditya1875.pokeverse.feature.game.battle.domain.BotBrain
import com.aditya1875.pokeverse.feature.game.battle.domain.Fighter
import com.aditya1875.pokeverse.feature.game.battle.domain.Who
import com.aditya1875.pokeverse.utils.Haptics
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val Gold = Color(0xFFFFC24B)
private val Cyan = Color(0xFF29C6E0)

private data class FloatText(val id: Long, val text: String, val color: Color, val pos: Offset, val big: Boolean)

/**
 * The battle itself. Everything that happens is narrated and animated in order: the attacker lunges or
 * fires an orb, the blow lands with a burst and a damage number, the HP bar drops at that moment, and a
 * knockout sinks the Pokémon before the next one is sent out. Buttons say what they do and show, live,
 * how well each move works against the Pokémon in front of you.
 */
@Composable
fun BattleArena(
    state: BattleState,
    host: BattleHost,
    onRematch: () -> Unit,
    onChangeTeam: () -> Unit,
    onBack: () -> Unit,
) {
    val battleKey by host.battleKey.collectAsStateWithLifecycle()
    // A new battle starts from a clean slate: no leftover animation or half-played turn.
    key(battleKey) { ArenaBody(state, host, onRematch, onChangeTeam, onBack) }
}

@Composable
private fun ArenaBody(
    state: BattleState,
    host: BattleHost,
    onRematch: () -> Unit,
    onChangeTeam: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val hud by host.hud.collectAsStateWithLifecycle()
    val rivalName = hud.opponentName ?: stringResource(R.string.battle_opponent)
    val sound: SoundManager = koinInject()
    val haptics: Haptics = koinInject()
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(state)

    // What is on screen right now. During a turn this lags the real state and catches up event by event.
    var shownPlayer by remember { mutableStateOf(state.player.current) }
    var shownFoe by remember { mutableStateOf(state.foe.current) }
    var logLine by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(true) }
    var showIntro by remember { mutableStateOf(true) }
    var showBench by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    val pLunge = remember { Animatable(0f) }; val fLunge = remember { Animatable(0f) }
    val pShake = remember { Animatable(0f) }; val fShake = remember { Animatable(0f) }
    val pFlash = remember { Animatable(0f) }; val fFlash = remember { Animatable(0f) }
    val pFaint = remember { Animatable(0f) }; val fFaint = remember { Animatable(0f) }
    val pEnter = remember { Animatable(0f) }; val fEnter = remember { Animatable(0f) }
    val pShield = remember { Animatable(0f) }; val fShield = remember { Animatable(0f) }
    val screenShake = remember { Animatable(0f) }
    val screenFlash = remember { Animatable(0f) }
    val projT = remember { Animatable(0f) }
    val burstT = remember { Animatable(1f) }
    var projFrom by remember { mutableStateOf(Offset.Zero) }
    var projTo by remember { mutableStateOf(Offset.Zero) }
    var projColor by remember { mutableStateOf(Color.White) }
    var projBig by remember { mutableStateOf(false) }
    var burstAt by remember { mutableStateOf(Offset.Zero) }
    var burstColor by remember { mutableStateOf(Color.White) }
    var burstBig by remember { mutableStateOf(false) }
    val floats = remember { mutableStateListOf<FloatText>() }
    var floatId by remember { mutableLongStateOf(0L) }

    val idle = rememberInfiniteTransition(label = "idle")
    val bob by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val aura by idle.animateFloat(0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "aura")

    // First battle ever: show the how-to automatically, once.
    LaunchedEffect(Unit) {
        if (!ScreenStateManager.isBattleTutorialSeen(context)) {
            showHelp = true
            ScreenStateManager.markBattleTutorialSeen(context)
        }
    }

    // ── Scene geometry (set from the measured size below) ──────────────────────────────────────────
    var foePos by remember { mutableStateOf(Offset.Zero) }
    var playerPos by remember { mutableStateOf(Offset.Zero) }
    var unit by remember { mutableStateOf(1f) } // px per dp

    fun posOf(who: Who) = if (who == Who.PLAYER) playerPos else foePos
    fun lunge(who: Who) = if (who == Who.PLAYER) pLunge else fLunge
    fun shake(who: Who) = if (who == Who.PLAYER) pShake else fShake
    fun flash(who: Who) = if (who == Who.PLAYER) pFlash else fFlash
    fun faint(who: Who) = if (who == Who.PLAYER) pFaint else fFaint
    fun enter(who: Who) = if (who == Who.PLAYER) pEnter else fEnter
    fun shield(who: Who) = if (who == Who.PLAYER) pShield else fShield
    fun other(who: Who) = if (who == Who.PLAYER) Who.FOE else Who.PLAYER

    fun popText(text: String, color: Color, at: Offset, big: Boolean = false) {
        floats += FloatText(floatId++, text, color, at, big)
    }

    suspend fun burst(at: Offset, color: Color, big: Boolean) {
        burstAt = at; burstColor = color; burstBig = big
        burstT.snapTo(0f)
        burstT.animateTo(1f, tween(if (big) 620 else 380, easing = LinearOutSlowInEasing))
    }

    suspend fun playHit(e: BattleEvent.Hit) {
        val target = other(e.by)
        val color = pokemonTypeColor(e.attackerType)
        val from = posOf(e.by); val to = posOf(target)
        logLine = context.getString(
            if (e.special) R.string.battle_log_special else R.string.battle_log_attack, e.attackerName, e.damage
        )
        coroutineScope {
            if (e.special) {
                // A charge-up, then an orb that crosses the arena.
                launch { burst(from, color, big = false) }
                delay(260)
                projFrom = from; projTo = to; projColor = color; projBig = true
                projT.snapTo(0f)
                projT.animateTo(1f, tween(380, easing = LinearEasing))
                projT.snapTo(0f)
            } else {
                // A quick strike: the attacker dashes at its target and back.
                launch { lunge(e.by).animateTo(1f, tween(130, easing = FastOutSlowInEasing)); lunge(e.by).animateTo(0f, tween(210)) }
                delay(130)
            }
            // The blow lands: numbers change now, not before.
            if (target == Who.FOE) shownFoe = shownFoe.copy(hp = e.defenderHpAfter, energy = e.defenderEnergyAfter)
            else shownPlayer = shownPlayer.copy(hp = e.defenderHpAfter, energy = e.defenderEnergyAfter)
            if (e.by == Who.FOE) shownFoe = shownFoe.copy(energy = e.attackerEnergyAfter)
            else shownPlayer = shownPlayer.copy(energy = e.attackerEnergyAfter)

            val heavy = e.special || e.effectiveness >= 2f || e.critical
            sound.play(SoundManager.Sound.RUSH_CLICK, if (heavy) 0.9f else 0.6f, haptic = false)
            when {
                e.damage == 0 -> {}
                heavy -> haptics.play(Haptics.Haptic.HEAVY)
                else -> haptics.play(Haptics.Haptic.CLICK)
            }
            if (e.damage > 0) {
                launch { for (x in listOf(12f, -10f, 7f, -5f, 2f, 0f)) shake(target).animateTo(x, tween(38)) }
                launch { flash(target).animateTo(0.85f, tween(50)); flash(target).animateTo(0f, tween(220)) }
                if (heavy) launch {
                    for (x in listOf(8f, -7f, 5f, -3f, 0f)) screenShake.animateTo(x, tween(34))
                }
                if (e.critical) launch { screenFlash.animateTo(0.5f, tween(50)); screenFlash.animateTo(0f, tween(240)) }
                popText("-${e.damage}", if (target == Who.FOE) Color(0xFFFF8A80) else Color(0xFFFFCDD2), to, big = heavy)
            } else {
                popText(context.getString(R.string.battle_eff_none), Color(0xFFB0BEC5), to)
            }
            burst(to, color, big = heavy)
        }
        delay(120)
        val note = when {
            e.effectiveness == 0f -> R.string.battle_log_immune
            e.effectiveness >= 2f -> R.string.battle_log_super
            e.effectiveness < 1f -> R.string.battle_log_resisted
            else -> null
        }
        if (note != null) {
            logLine = context.getString(note)
            popText(
                context.getString(note), when {
                    e.effectiveness >= 2f -> Color(0xFF69F0AE)
                    e.effectiveness == 0f -> Color(0xFFB0BEC5)
                    else -> Color(0xFFFFB74D)
                },
                to + Offset(0f, -48f * unit), big = false
            )
            delay(640)
        }
        if (e.critical && e.damage > 0) { logLine = context.getString(R.string.battle_log_crit); delay(520) }
    }

    // Plays each turn back, one beat at a time.
    LaunchedEffect(Unit) {
        // The versus splash.
        sound.play(SoundManager.Sound.MATCH_FOUND, 0.8f, haptic = false)
        haptics.play(Haptics.Haptic.CHARGE)
        launch { pEnter.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        launch { delay(200); fEnter.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        delay(1700)
        showIntro = false
        haptics.play(Haptics.Haptic.POP)
        logLine = context.getString(R.string.battle_intro_foe_sends, shownFoe.name)
        delay(700)
        logLine = null
        playing = false
        // The arena is idle: an online battle starts the first turn clock now.
        host.playbackFinished()

        host.events.collect { events ->
            playing = true
            for (e in events) {
                when (e) {
                    is BattleEvent.Hit -> playHit(e)
                    is BattleEvent.Guarded -> {
                        logLine = context.getString(R.string.battle_log_guard, e.name)
                        haptics.play(Haptics.Haptic.TICK)
                        if (e.by == Who.PLAYER) shownPlayer = shownPlayer.copy(energy = e.energyAfter)
                        else shownFoe = shownFoe.copy(energy = e.energyAfter)
                        shield(e.by).snapTo(0f)
                        shield(e.by).animateTo(1f, tween(480))
                        shield(e.by).animateTo(0f, tween(240))
                    }

                    is BattleEvent.Switched -> {
                        logLine = context.getString(R.string.battle_log_switch, e.name)
                        // The new one arrives in a flash.
                        if (e.who == Who.PLAYER) shownPlayer = e.fighter else shownFoe = e.fighter
                        faint(e.who).snapTo(0f)
                        enter(e.who).snapTo(0f)
                        sound.play(SoundManager.Sound.CARD_FLIP, 0.8f, haptic = false)
                        haptics.play(Haptics.Haptic.POP)
                        launch { burst(posOf(e.who), Color.White, big = false) }
                        enter(e.who).animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                        delay(250)
                    }

                    is BattleEvent.Fainted -> {
                        logLine = context.getString(R.string.battle_log_faint, e.name)
                        sound.play(if (e.who == Who.FOE) SoundManager.Sound.CORRECT_ANSWER else SoundManager.Sound.WRONG_ANSWER, 0.8f, haptic = false)
                        haptics.play(if (e.who == Who.FOE) Haptics.Haptic.CONFIRM else Haptics.Haptic.REJECT)
                        faint(e.who).animateTo(1f, tween(620, easing = FastOutSlowInEasing))
                        delay(250)
                    }

                    is BattleEvent.EnergyReady -> {
                        if (e.who == Who.PLAYER) {
                            logLine = context.getString(R.string.battle_log_energy, shownPlayer.name)
                            haptics.play(Haptics.Haptic.POP)
                            launch { burst(posOf(Who.PLAYER), Gold, big = false) }
                            delay(500)
                        }
                    }

                    is BattleEvent.Won -> {
                        sound.play(if (e.who == Who.PLAYER) SoundManager.Sound.GAME_WIN else SoundManager.Sound.GAME_LOSE, haptic = false)
                        haptics.play(if (e.who == Who.PLAYER) Haptics.Haptic.SUCCESS else Haptics.Haptic.HEAVY)
                        delay(450)
                    }
                }
            }
            // Whatever the animation did, end up exactly on the real state.
            shownPlayer = latest.player.current
            shownFoe = latest.foe.current
            logLine = null
            playing = false
            showBench = false
            host.playbackFinished()
        }
    }

    Column(Modifier.fillMaxSize()) {
        // ── Scene ───────────────────────────────────────────────────────────────────────────────────
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer { translationX = screenShake.value * unit }
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val w = with(density) { maxWidth.toPx() }
            val h = with(density) { maxHeight.toPx() }
            unit = density.density
            // Sprite sizes follow the width; the foe sits top-right and yours bottom-left, and the two info
            // plates take the opposite corners, so on any screen nothing is drawn on top of anything else.
            val foeSize = minOf(124.dp, maxWidth * 0.32f)
            val playerSize = minOf(176.dp, maxWidth * 0.44f, maxHeight * 0.36f)
            foePos = Offset(w - (foeSize.value / 2f + 14f) * unit, maxOf(100f * unit, h * 0.26f))
            playerPos = Offset((playerSize.value / 2f + 10f) * unit, minOf(h - 110f * unit, h * 0.74f))
            val foeColor = pokemonTypeColor(shownFoe.primaryType)
            val myColor = pokemonTypeColor(shownPlayer.primaryType)

            // Backdrop: the foe's type tints the sky, yours tints the ground.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(lerp(Color(0xFF0B1020), foeColor, 0.30f), Color(0xFF0E1424), lerp(Color(0xFF0E1424), myColor, 0.28f))))
            )
            // Platforms, the shield bubbles, the energy aura and the effects, drawn between the Pokémon.
            Canvas(Modifier.fillMaxSize()) {
                fun platform(c: Offset, rx: Float, tint: Color) {
                    drawOval(
                        brush = Brush.radialGradient(listOf(tint.copy(alpha = 0.55f), tint.copy(alpha = 0.05f)), center = c, radius = rx),
                        topLeft = Offset(c.x - rx, c.y - rx * 0.28f), size = Size(rx * 2f, rx * 0.56f)
                    )
                    drawOval(
                        color = tint.copy(alpha = 0.5f), topLeft = Offset(c.x - rx * 0.78f, c.y - rx * 0.20f),
                        size = Size(rx * 1.56f, rx * 0.40f), style = Stroke(width = 1.5f * unit)
                    )
                }
                platform(Offset(foePos.x, foePos.y + 52f * unit), 84f * unit, foeColor)
                platform(Offset(playerPos.x, playerPos.y + 70f * unit), 116f * unit, myColor)

                // Energy aura around a player Pokémon with a full meter.
                if (shownPlayer.energyReady && !shownPlayer.fainted) {
                    drawCircle(Gold.copy(alpha = 0.18f * aura), 98f * unit, playerPos)
                    drawCircle(Gold.copy(alpha = 0.7f * aura), 98f * unit, playerPos, style = Stroke(3f * unit))
                }
                // Guard bubbles.
                listOf(Who.PLAYER to pShield.value, Who.FOE to fShield.value).forEach { (who, v) ->
                    if (v > 0.01f) {
                        val c = posOf(who); val r = (if (who == Who.PLAYER) 100f else 76f) * unit * (0.85f + 0.2f * v)
                        drawCircle(Cyan.copy(alpha = 0.22f * v), r, c)
                        drawCircle(Cyan.copy(alpha = 0.85f * v), r, c, style = Stroke(4f * unit))
                    }
                }
                // The orb of a special attack.
                if (projT.value > 0.001f && projT.value < 0.999f) {
                    val p = projT.value
                    for (i in 0..5) {
                        val tp = (p - i * 0.05f).coerceAtLeast(0f)
                        val c = Offset(projFrom.x + (projTo.x - projFrom.x) * tp, projFrom.y + (projTo.y - projFrom.y) * tp)
                        drawCircle(projColor.copy(alpha = 0.5f * (1f - i / 6f)), (24f - i * 2.5f) * unit, c)
                    }
                    val head = Offset(projFrom.x + (projTo.x - projFrom.x) * p, projFrom.y + (projTo.y - projFrom.y) * p)
                    drawCircle(Brush.radialGradient(listOf(Color.White, projColor, Color.Transparent), center = head, radius = 34f * unit), 34f * unit, head)
                }
                // Impact burst.
                if (burstT.value < 0.999f) {
                    val t = burstT.value
                    val reach = (if (burstBig) 120f else 70f) * unit
                    drawCircle(burstColor.copy(alpha = (1f - t) * 0.8f), reach * t, burstAt, style = Stroke((6f - 4f * t) * unit))
                    for (i in 0 until (if (burstBig) 18 else 10)) {
                        val a = i / (if (burstBig) 18f else 10f) * 2f * PI.toFloat()
                        val d = reach * (0.35f + 0.75f * t)
                        drawLine(
                            burstColor.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                            Offset(burstAt.x + cos(a) * d * 0.6f, burstAt.y + sin(a) * d * 0.6f),
                            Offset(burstAt.x + cos(a) * d, burstAt.y + sin(a) * d),
                            strokeWidth = 3.5f * unit, cap = StrokeCap.Round
                        )
                    }
                }
            }

            // The Pokémon.
            FighterArt(foeSize, foePos, shownFoe, Who.FOE, fLunge.value, playerPos, fShake.value, fFlash.value, fFaint.value, fEnter.value, bob)
            FighterArt(playerSize, playerPos, shownPlayer, Who.PLAYER, pLunge.value, foePos, pShake.value, pFlash.value, pFaint.value, pEnter.value, bob)

            // Info plates.
            NamePlate(
                fighter = shownFoe, team = state.foe.team, guardsLeft = state.foe.guardsLeft, label = rivalName,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).fillMaxWidth(0.5f).widthIn(max = 280.dp),
            )
            NamePlate(
                fighter = shownPlayer, team = state.player.team, guardsLeft = state.player.guardsLeft, label = null,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).fillMaxWidth(0.5f).widthIn(max = 280.dp),
            )

            // Help.
            IconButton(onClick = { showHelp = true }, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                Icon(Icons.Default.Info, contentDescription = stringResource(R.string.battle_help_button), tint = Color.White.copy(alpha = 0.8f))
            }

            // Floating damage and effectiveness notes.
            floats.forEach { f ->
                key(f.id) {
                    FloatingNote(f, unit) { floats.remove(f) }
                }
            }

            // Versus splash.
            val introAlpha by animateFloatAsState(if (showIntro) 1f else 0f, tween(300), label = "intro")
            if (introAlpha > 0.01f) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = introAlpha }) { VersusSplash(shownPlayer, shownFoe) }
            }
            // A full-screen flash for criticals.
            if (screenFlash.value > 0.01f) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = screenFlash.value)))
        }

        // ── Online status: the turn clock and what the rival is doing ───────────────────────────────
        if (hud.online && !state.over) OnlineStatusBar(hud = hud, rivalName = rivalName)

        // ── Narration ───────────────────────────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .height(44.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val idleLine = when {
                !hud.online -> stringResource(R.string.battle_your_turn)
                hud.opponentAway && hud.waiting -> stringResource(R.string.battle_online_away, rivalName)
                state.foeMustSwitch && !state.playerMustSwitch -> stringResource(R.string.battle_online_opponent_choosing, rivalName)
                hud.waiting -> stringResource(R.string.battle_online_waiting, rivalName)
                state.playerMustSwitch -> stringResource(R.string.battle_choose_next)
                hud.opponentLocked -> stringResource(R.string.battle_online_opponent_ready, rivalName)
                else -> stringResource(R.string.battle_your_turn)
            }
            AnimatedContent(
                targetState = logLine ?: idleLine,
                transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(80)) },
                label = "log"
            ) { line ->
                Text(line, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2)
            }
        }

        // ── Controls ────────────────────────────────────────────────────────────────────────────────
        Box(Modifier.padding(horizontal = 14.dp)) {
            Controls(
                state = state,
                me = shownPlayer,
                foe = shownFoe,
                // Online, your buttons also stay off once your choice is in and you are waiting on the rival.
                enabled = !playing && !state.over && !hud.waiting,
                showBench = showBench || state.playerMustSwitch,
                onToggleBench = { showBench = !showBench },
                onAct = { host.act(it) },
                onReplace = { host.chooseReplacement(it) },
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    // Result, once the last blow has played out.
    AnimatedVisibility(
        visible = state.over && !playing,
        enter = fadeIn(tween(250)) + scaleIn(spring(Spring.DampingRatioMediumBouncy)),
    ) {
        val shards by host.shardsGained.collectAsStateWithLifecycle()
        val shardsCapped by host.shardsCapped.collectAsStateWithLifecycle()
        ResultCard(
            won = state.winner == Who.PLAYER,
            turns = state.turn,
            shards = shards,
            shardsCapped = shardsCapped,
            hud = hud,
            rivalName = rivalName,
            onRematch = onRematch,
            onChangeTeam = onChangeTeam,
            onBack = onBack,
        )
    }

    if (showHelp) BattleHelpDialog(onDismiss = { showHelp = false })
}

// ── Online status ───────────────────────────────────────────────────────────────────────────────────

/**
 * A slim strip between the scene and the narration for online battles: a draining bar for your turn clock,
 * and a chip when your rival has locked in or looks offline. Hidden when there is nothing to say.
 */
@Composable
private fun OnlineStatusBar(hud: ArenaHud, rivalName: String) {
    val left = hud.secondsLeft
    val urgent = left != null && left <= 10
    val barColor by animateColorAsState(if (urgent) Color(0xFFFF6B6B) else Gold, label = "clock")
    val progress by animateFloatAsState(
        (left ?: 0) / OnlineBattleRules.TURN_SECONDS.toFloat(),
        animationSpec = tween(900, easing = LinearEasing), label = "clock_progress"
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            // A warning that one more missed turn is a disqualification.
            hud.myMissedTurns > 0 -> StatusChip(
                stringResource(R.string.battle_online_missed_chip, hud.myMissedTurns, OnlineBattleRules.MAX_MISSED_TURNS),
                Color(0xFFFF8A65)
            )
            hud.opponentAway -> StatusChip(stringResource(R.string.battle_online_away_chip), Color(0xFFFFB74D))
            hud.opponentLocked && !hud.waiting -> StatusChip(stringResource(R.string.battle_online_ready_chip, rivalName), Color(0xFF69F0AE))
            else -> Spacer(Modifier.width(0.dp))
        }
        if (left != null) {
            Box(
                Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).clip(CircleShape).background(barColor))
            }
            Text(
                stringResource(R.string.battle_online_clock, left),
                color = barColor, fontWeight = FontWeight.Black, fontSize = 13.sp,
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(max = 200.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.5f)), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

// ── Pieces ──────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun FighterArt(
    size: androidx.compose.ui.unit.Dp,
    center: Offset,
    fighter: Fighter,
    who: Who,
    lunge: Float,
    towards: Offset,
    shake: Float,
    flash: Float,
    faint: Float,
    enter: Float,
    bob: Float,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val px = with(density) { size.toPx() }
    val dx = towards.x - center.x; val dy = towards.y - center.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val reach = 46f * density.density
    AsyncImage(
        model = fighter.spriteUrl,
        contentDescription = fighter.name,
        contentScale = ContentScale.Fit,
        colorFilter = if (flash > 0.02f) ColorFilter.tint(Color.White.copy(alpha = flash), BlendMode.SrcAtop) else null,
        modifier = Modifier
            .offset { IntOffset((center.x - px / 2f).toInt(), (center.y - px / 2f).toInt()) }
            .size(size)
            .graphicsLayer {
                // Idle breathing, the lunge toward the target, the hit shake, then the faint and the send-out.
                translationX = dx / len * reach * lunge + shake * density.density
                translationY = dy / len * reach * lunge - 6f * density.density * bob + 40f * density.density * faint
                val s = (0.3f + 0.7f * enter) * (1f - 0.12f * faint)
                scaleX = (if (who == Who.PLAYER) -s else s)
                scaleY = s
                alpha = (1f - faint) * enter.coerceIn(0f, 1f)
            }
    )
}

@Composable
private fun NamePlate(fighter: Fighter, team: List<Fighter>, guardsLeft: Int, label: String?, modifier: Modifier) {
    val hp by animateFloatAsState(fighter.hpFraction, tween(450), label = "hp")
    val hpColor = when {
        hp > 0.5f -> Color(0xFF43A047)
        hp > 0.2f -> Color(0xFFFFB300)
        else -> Color(0xFFE53935)
    }
    val pulse by rememberInfiniteTransition(label = "p").animateFloat(0.5f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pp")
    val specialReady = fighter.energyReady
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC0E1424))
            .border(
                BorderStroke(
                    if (specialReady) 2.dp else 1.dp,
                    if (specialReady) Gold.copy(alpha = pulse) else Color.White.copy(alpha = 0.14f)
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (label != null) Text(label.uppercase(), color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, letterSpacing = 1.5.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(fighter.name, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(6.dp))
            Text("Lv${BattleMath.LEVEL}", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { fighter.types.forEach { TypeTag(it) } }
        // HP, with the number beside the bar instead of on a row of its own
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("HP", color = Color(0xFFFFB300), fontWeight = FontWeight.Black, fontSize = 10.sp)
            Spacer(Modifier.width(5.dp))
            Box(Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.15f))) {
                Box(Modifier.fillMaxWidth(hp.coerceIn(0f, 1f)).height(9.dp).clip(RoundedCornerShape(50)).background(hpColor))
            }
            Spacer(Modifier.width(5.dp))
            Text("${fighter.hp.coerceAtLeast(0)}", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        // Special meter: it fills as the Pokémon attacks, guards and gets hit, and the heavy move unlocks at 100%.
        val ready = fighter.energyReady
        val energy by animateFloatAsState((fighter.energy / BattleMath.FULL_ENERGY.toFloat()).coerceIn(0f, 1f), tween(450), label = "energy")
        val percent = (energy * 100).toInt()
        val energyLabel = stringResource(R.string.battle_energy_label)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { contentDescription = "$energyLabel $percent%" }
        ) {
            Text(energyLabel.uppercase(), color = if (ready) Gold else Cyan, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.width(5.dp))
            Box(
                Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(energy)
                        .height(10.dp).clip(RoundedCornerShape(50))
                        .background(if (ready) Gold.copy(alpha = pulse) else Cyan)
                )
                // Quarter marks, so "how far to go" can be read at a glance.
                androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                    for (q in 1..3) {
                        val x = size.width * q / 4f
                        drawLine(Color.Black.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
                    }
                }
            }
            Spacer(Modifier.width(5.dp))
            // "READY" takes the place of the percentage, so a full meter needs no extra line of its own.
            Text(
                if (ready) stringResource(R.string.battle_ready_short) else "$percent%",
                color = if (ready) Gold else Color.White.copy(alpha = 0.75f), fontSize = 10.sp, fontWeight = FontWeight.Black
            )
        }
        // Guards left (a side only gets a few for the whole battle) and the team, on one row.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Shield, contentDescription = stringResource(R.string.battle_guards_left, guardsLeft), tint = Cyan, modifier = Modifier.size(12.dp))
            repeat(BattleEngine.MAX_GUARDS) { i ->
                Box(Modifier.size(width = 12.dp, height = 5.dp).background(if (i < guardsLeft) Cyan else Color.White.copy(alpha = 0.16f), RoundedCornerShape(50)))
            }
            Spacer(Modifier.weight(1f))
            team.forEach { m -> Box(Modifier.size(7.dp).background(if (m.fainted) Color.White.copy(alpha = 0.18f) else Gold, CircleShape)) }
        }
    }
}

@Composable
private fun TypeTag(type: String) {
    val color = pokemonTypeColor(type)
    Text(
        localizedTypeName(type), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.3f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.85f)), RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

@Composable
private fun FloatingNote(f: FloatText, unit: Float, onDone: () -> Unit) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(1000, easing = LinearOutSlowInEasing))
        onDone()
    }
    Text(
        f.text,
        color = f.color,
        fontWeight = FontWeight.Black,
        fontSize = if (f.big) 30.sp else 20.sp,
        modifier = Modifier
            .offset { IntOffset((f.pos.x - 40f * unit).toInt(), (f.pos.y - 50f * unit).toInt()) }
            .graphicsLayer {
                translationY = -70f * unit * t.value
                alpha = if (t.value < 0.7f) 1f else (1f - (t.value - 0.7f) / 0.3f)
                val s = 0.7f + 0.5f * (1f - t.value).coerceAtLeast(0.4f)
                scaleX = s; scaleY = s
            }
    )
}

@Composable
private fun VersusSplash(me: Fighter, foe: Fighter) {
    val slide = remember { Animatable(0f) }
    LaunchedEffect(Unit) { slide.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) }
    Box(Modifier.fillMaxSize().background(Color(0xE60B1020)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f).graphicsLayer { translationX = -400f * (1f - slide.value); alpha = slide.value },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(me.spriteUrl, me.name, Modifier.size(110.dp).graphicsLayer { scaleX = -1f })
                    Text(me.name, color = Color.White, fontWeight = FontWeight.Black)
                }
                Text(
                    stringResource(R.string.battle_intro_vs), color = Gold, fontSize = 40.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.graphicsLayer { scaleX = 0.5f + 0.5f * slide.value; scaleY = 0.5f + 0.5f * slide.value }
                )
                Column(
                    Modifier.weight(1f).graphicsLayer { translationX = 400f * (1f - slide.value); alpha = slide.value },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(foe.spriteUrl, foe.name, Modifier.size(110.dp))
                    Text(foe.name, color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// ── Controls ────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun Controls(
    state: BattleState,
    me: Fighter,
    foe: Fighter,
    enabled: Boolean,
    showBench: Boolean,
    onToggleBench: () -> Unit,
    onAct: (BattleAction) -> Unit,
    onReplace: (Int) -> Unit,
) {
    val benchAlive = state.player.team.withIndex().filter { (i, f) -> i != state.player.active && !f.fainted }

    if (showBench && benchAlive.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.playerMustSwitch) Text(stringResource(R.string.battle_choose_next), color = Gold, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                benchAlive.forEach { (i, f) ->
                    Column(
                        Modifier
                            .width(108.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), RoundedCornerShape(14.dp))
                            .clickable(enabled = enabled || state.playerMustSwitch) {
                                if (state.playerMustSwitch) onReplace(i) else onAct(BattleAction.Switch(i))
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(f.spriteUrl, f.name, Modifier.size(56.dp))
                        Text(f.name, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${(f.hpFraction * 100).toInt()}% HP", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
            }
            if (!state.playerMustSwitch) TextButton(onClick = onToggleBench) { Text(stringResource(R.string.cancel)) }
        }
        return
    }

    val canSwitch = benchAlive.isNotEmpty()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.45f)
    ) {
        // Four tiles of the same size, two by two.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActionTile(
                stringResource(R.string.battle_attack), stringResource(R.string.battle_hint_attack),
                me.primaryType, pokemonTypeColor(me.primaryType), enabled, false, Modifier.weight(1f)
            ) { onAct(BattleAction.Attack) }
            ActionTile(
                label = stringResource(R.string.battle_special),
                hint = if (me.energyReady) stringResource(R.string.battle_hint_special)
                else stringResource(R.string.battle_special_charging, me.energy.coerceIn(0, BattleMath.FULL_ENERGY)),
                type = me.specialType,
                accent = pokemonTypeColor(me.specialType),
                enabled = enabled && me.energyReady,
                highlight = me.energyReady,
                modifier = Modifier.weight(1f),
                onClick = { onAct(BattleAction.Special) },
                progress = (me.energy / BattleMath.FULL_ENERGY.toFloat()).coerceIn(0f, 1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val guards = state.player.guardsLeft
            ActionTile(
                stringResource(R.string.battle_guard),
                if (guards > 0) stringResource(R.string.battle_hint_guard_left, guards) else stringResource(R.string.battle_guard_none),
                null, Cyan, enabled && guards > 0, false, Modifier.weight(1f)
            ) { onAct(BattleAction.Guard) }
            ActionTile(
                stringResource(R.string.battle_switch), stringResource(R.string.battle_hint_switch),
                null, Color(0xFF90A4AE), enabled && canSwitch, false, Modifier.weight(1f), onClick = onToggleBench
            )
        }
    }
}

/** One of the four action buttons. They are all the same size so the grid reads as a set. */
@Composable
private fun ActionTile(
    label: String,
    hint: String,
    type: String?,
    accent: Color,
    enabled: Boolean,
    highlight: Boolean,
    modifier: Modifier,
    /** 0..1 fill along the bottom edge (the Special's charge), or null for none. */
    progress: Float? = null,
    onClick: () -> Unit,
) {
    val pulse by rememberInfiniteTransition(label = "btn").animateFloat(0.4f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "bp")
    Column(
        modifier
            .height(68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    if (highlight) listOf(Gold.copy(alpha = 0.34f), Gold.copy(alpha = 0.14f))
                    else listOf(accent.copy(alpha = if (enabled) 0.32f else 0.12f), accent.copy(alpha = if (enabled) 0.12f else 0.06f))
                )
            )
            .drawBehind {
                if (progress != null) {
                    val h = 6.dp.toPx()
                    drawRect(
                        color = if (highlight) Gold else accent.copy(alpha = 0.9f),
                        topLeft = Offset(0f, size.height - h),
                        size = androidx.compose.ui.geometry.Size(size.width * progress, h),
                    )
                }
            }
            .border(BorderStroke(if (highlight) 2.dp else 1.5.dp, if (highlight) Gold.copy(alpha = 0.5f + 0.5f * pulse) else accent.copy(alpha = 0.75f)), RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1)
            if (type != null) Text(localizedTypeName(type), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))
        Text(hint, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2)
    }
}

// ── How to battle ───────────────────────────────────────────────────────────────────────────────────

@Composable
fun BattleHelpDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Card(
            shape = RoundedCornerShape(24.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF141B2E)),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.battle_help_title), color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.battle_help_goal), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
                HelpRow(R.string.battle_attack, Color(0xFF64B5F6), R.string.battle_help_attack)
                HelpRow(R.string.battle_special, Gold, R.string.battle_help_special)
                HelpRow(R.string.battle_guard, Cyan, R.string.battle_help_guard)
                HelpRow(R.string.battle_switch, Color(0xFF90A4AE), R.string.battle_help_switch)
                Text(stringResource(R.string.battle_help_types), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                androidx.compose.material3.Button(
                    onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                ) { Text(stringResource(R.string.battle_help_got_it), fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun HelpRow(label: Int, color: Color, body: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.22f))
                .border(BorderStroke(1.5.dp, color.copy(alpha = 0.8f)), RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(label), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1)
        }
        Text(stringResource(body), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
