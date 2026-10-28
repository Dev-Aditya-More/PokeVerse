package com.aditya1875.pokeverse.feature.dexmode.presentation

import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgress
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaPlayer
import android.speech.RecognizerIntent
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.dexmode.data.DexModeStore
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components.TTSManager
import com.aditya1875.pokeverse.utils.localizedTypeName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// ─── Device palette ───────────────────────────────────────────────────────────

private val RedLight = Color(0xFFE85A5A)
private val Red = Color(0xFFD32F2F)
private val RedDark = Color(0xFF9E1B1B)
private val RedDeep = Color(0xFF6E1010)
// Printed labels on the red shell: RedDeep on Red was nearly invisible, so they use a warm off-white.
private val ShellLabel = Color(0xFFFFE9DA)
private val BezelLight = Color(0xFFECECEC)
private val BezelShade = Color(0xFFC4C4C4)
private val Plastic = Color(0xFF2B2B2B)
private val PlasticLight = Color(0xFF454545)

/** LCD colour schemes: the classic four-shade green, or a colour backlight (SELECT toggles). */
private data class LcdPalette(
    val bg: Color,
    val bgDark: Color,
    val ink: Color,
    val inkSoft: Color,
    val light: Color,
    val isColor: Boolean
)

private val ClassicGreen = LcdPalette(
    bg = Color(0xFF9BBC0F),
    bgDark = Color(0xFF8BAC0F),
    ink = Color(0xFF0F380F),
    inkSoft = Color(0xFF306230),
    light = Color(0xFFCADC9F),
    isColor = false
)

private val Backlit = LcdPalette(
    bg = Color(0xFF0E1A24),
    bgDark = Color(0xFF16283A),
    ink = Color(0xFFDDF6FF),
    inkSoft = Color(0xFF7FA7BF),
    light = Color(0xFFDDF6FF),
    isColor = true
)

private val LcdFont = FontFamily.Monospace

private enum class Pad { UP, DOWN, LEFT, RIGHT }

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun DexModeScreen(
    onBack: () -> Unit,
    viewModel: DexModeViewModel = koinViewModel()
) {
    val store: DexModeStore = koinInject()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val handheldSeen by store.seen.collectAsStateWithLifecycle()
    val pokedex: PokedexRepository = koinInject()
    val dex by pokedex.progress.collectAsStateWithLifecycle(DexProgress.EMPTY)
    // The handheld and the main app share one Pokédex: either one having seen it counts.
    val seen = remember(handheldSeen, dex.seen) { handheldSeen + dex.seen }
    val backlit by store.backlit.collectAsStateWithLifecycle()
    val muted by store.muted.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val sfx = remember { DexSfx() }
    val tts = remember { TTSManager.getInstance(context) }
    val ttsSpeaking by tts.isSpeaking.collectAsStateWithLifecycle()
    val cryPlayer = remember { MediaPlayer() }
    var cryPlaying by remember { mutableStateOf(false) }
    var pressPulse by remember { mutableIntStateOf(0) }
    var replayKey by remember { mutableIntStateOf(0) }
    var showTextSearch by remember { mutableStateOf(false) }
    var bootProgress by remember { mutableFloatStateOf(if (state.screen == DexScreen.BOOT) 0f else 1f) }
    val movesState = rememberLazyListState()

    val palette = if (backlit) Backlit else ClassicGreen

    fun stopAudio() {
        tts.stop()
        runCatching { cryPlayer.reset() }
        cryPlaying = false
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            sfx.release()
            runCatching { cryPlayer.release() }
        }
    }

    fun feedback(sound: DexSfx.Sfx? = DexSfx.Sfx.BLIP) {
        if (sound != null && !muted) sfx.play(sound)
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        pressPulse++
    }

    // Boot sequence: chime, LED sweep, system-check bar, then into the list.
    LaunchedEffect(Unit) {
        if (state.screen != DexScreen.BOOT) return@LaunchedEffect
        delay(250)
        if (!muted) sfx.play(DexSfx.Sfx.BOOT)
        val steps = 12
        repeat(steps) {
            delay(130)
            bootProgress = (it + 1f) / steps
        }
        delay(350)
        viewModel.finishBoot()
    }

    // Opening an entry: the cry plays, then the Dex reads its entry aloud.
    val detail = state.detail
    LaunchedEffect(detail?.id, muted, replayKey) {
        stopAudio()
        if (detail == null || muted) return@LaunchedEffect
        delay(300)
        val text = buildString {
            append(spokenName(detail.name)).append(". ")
            detail.genus?.let { append("The ").append(it).append(". ") }
            detail.flavor?.let { append(it) }
        }
        val cry = detail.cryUrl
        if (cry == null) {
            tts.speak(text, withBeep = false)
            return@LaunchedEffect
        }
        runCatching {
            cryPlayer.reset()
            cryPlayer.setDataSource(cry)
            cryPlayer.setOnPreparedListener { it.start(); cryPlaying = true }
            cryPlayer.setOnCompletionListener { cryPlaying = false; tts.speak(text, withBeep = false) }
            cryPlayer.setOnErrorListener { _, _, _ -> cryPlaying = false; tts.speak(text, withBeep = false); true }
            cryPlayer.prepareAsync()
        }.onFailure { tts.speak(text, withBeep = false) }
    }

    LaunchedEffect(detail?.id) { movesState.scrollToItem(0) }

    LaunchedEffect(state.searchMiss) {
        if (state.searchMiss != null) {
            delay(2400)
            viewModel.clearSearchMiss()
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        if (heard.isNotEmpty()) feedback(if (viewModel.search(heard)) DexSfx.Sfx.SELECT else DexSfx.Sfx.ERROR)
    }
    val searchPrompt = stringResource(R.string.dex_search_prompt)

    fun startSearch() {
        stopAudio()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PROMPT, searchPrompt)
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            showTextSearch = true
        }
    }

    fun onPad(dir: Pad) {
        val s = state
        if (s.screen == DexScreen.BOOT) { viewModel.finishBoot(); return }
        val moved = when (s.screen) {
            DexScreen.LIST -> when (dir) {
                Pad.UP -> viewModel.moveCursor(-1)
                Pad.DOWN -> viewModel.moveCursor(1)
                Pad.LEFT -> viewModel.moveCursor(-10)
                Pad.RIGHT -> viewModel.moveCursor(10)
            }
            DexScreen.ENTRY -> when (dir) {
                Pad.LEFT -> viewModel.changeTab(-1)
                Pad.RIGHT -> viewModel.changeTab(1)
                Pad.UP, Pad.DOWN -> if (s.tab == DexTab.MOVES) {
                    val step = if (dir == Pad.UP) -4 else 4
                    scope.launch { movesState.scrollToItem((movesState.firstVisibleItemIndex + step).coerceAtLeast(0)) }
                    true
                } else viewModel.moveCursor(if (dir == Pad.UP) -1 else 1)
            }
            DexScreen.BOOT -> false
        }
        feedback(if (moved) DexSfx.Sfx.BLIP else null)
    }

    fun onA() {
        when (state.screen) {
            DexScreen.BOOT -> viewModel.finishBoot()
            DexScreen.LIST -> viewModel.open()
            DexScreen.ENTRY -> if (state.detailError) viewModel.open() else replayKey++
        }
        feedback(DexSfx.Sfx.SELECT)
    }

    fun onB() {
        feedback(DexSfx.Sfx.BACK)
        if (!viewModel.back()) onBack()
    }

    BackHandler { onB() }

    DexDevice(
        talking = cryPlaying || ttsSpeaking,
        busy = state.screen == DexScreen.BOOT || state.listLoading || state.detailLoading,
        booting = state.screen == DexScreen.BOOT,
        pressPulse = pressPulse,
        muted = muted,
        onToggleMute = { store.toggleMuted(); view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) },
        onPowerOff = { feedback(DexSfx.Sfx.BACK); onBack() },
        onPad = ::onPad,
        onA = ::onA,
        onB = ::onB,
        onStart = { feedback(DexSfx.Sfx.SELECT); startSearch() },
        onSelect = { feedback(DexSfx.Sfx.BLIP); store.toggleBacklit() }
    ) {
        DexLcd(
            state = state,
            seen = seen,
            caught = dex.caught,
            palette = palette,
            bootProgress = bootProgress,
            movesState = movesState,
            onRowTap = { index ->
                if (index == state.cursor) onA() else { viewModel.setCursor(index); feedback() }
            },
            onRetryList = { viewModel.loadEntries() }
        )
    }

    if (showTextSearch) {
        TextSearchDialog(
            onSearch = { query ->
                showTextSearch = false
                feedback(if (viewModel.search(listOf(query))) DexSfx.Sfx.SELECT else DexSfx.Sfx.ERROR)
            },
            onDismiss = { showTextSearch = false }
        )
    }
}

// ─── Device body ──────────────────────────────────────────────────────────────

@Composable
private fun DexDevice(
    talking: Boolean,
    busy: Boolean,
    booting: Boolean,
    pressPulse: Int,
    muted: Boolean,
    onToggleMute: () -> Unit,
    onPowerOff: () -> Unit,
    onPad: (Pad) -> Unit,
    onA: () -> Unit,
    onB: () -> Unit,
    onStart: () -> Unit,
    onSelect: () -> Unit,
    screen: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RedLight, Red, RedDark)))
    ) {
        // Moulded-plastic sheen
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, Color.Black.copy(alpha = 0.10f)),
                    start = Offset.Zero, end = Offset(size.width, size.height)
                )
            )
        }

        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Lens(talking = talking, booting = booting, modifier = Modifier.size(64.dp))
                Spacer(Modifier.width(14.dp))
                StatusLeds(busy = busy, booting = booting, pressPulse = pressPulse)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleMute) {
                    Icon(
                        if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(if (muted) R.string.dex_cd_unmute else R.string.dex_cd_mute),
                        tint = Color.White.copy(alpha = 0.85f)
                    )
                }
                IconButton(onClick = onPowerOff) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = stringResource(R.string.dex_cd_power_off),
                        tint = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Hinge(Modifier.fillMaxWidth().height(22.dp))

            Bezel(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)) { screen() }

            Controls(
                onPad = onPad, onA = onA, onB = onB, onStart = onStart, onSelect = onSelect,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 8.dp, bottom = 10.dp)
            )
        }
    }
}

@Composable
private fun Lens(talking: Boolean, booting: Boolean, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "lens").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(220), RepeatMode.Reverse), label = "lens_pulse"
    )
    val glow by animateFloatAsState(if (talking || booting) pulse else 0f, tween(150), label = "lens_glow")

    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = center
        if (glow > 0f) {
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF7FE0FF).copy(alpha = 0.55f * glow), Color.Transparent), c, r * 1.25f),
                r * 1.25f, c
            )
        }
        drawCircle(Color.White, r, c)
        drawCircle(Color(0xFF1B1B1B), r * 0.86f, c)
        drawCircle(
            Brush.radialGradient(
                listOf(
                    lerpColor(Color(0xFF7FD3FF), Color(0xFFB8F0FF), glow),
                    Color(0xFF1E88E5),
                    Color(0xFF0D3C78)
                ),
                center = Offset(c.x - r * 0.2f, c.y - r * 0.2f), radius = r * 0.9f
            ),
            r * 0.78f, c
        )
        drawOval(
            Color.White.copy(alpha = 0.7f),
            topLeft = Offset(c.x - r * 0.5f, c.y - r * 0.55f),
            size = Size(r * 0.42f, r * 0.28f)
        )
    }
}

@Composable
private fun StatusLeds(busy: Boolean, booting: Boolean, pressPulse: Int) {
    val blink by rememberInfiniteTransition(label = "leds").animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(320), RepeatMode.Reverse), label = "led_blink"
    )
    val press = remember { Animatable(0f) }
    LaunchedEffect(pressPulse) {
        if (pressPulse == 0) return@LaunchedEffect
        press.snapTo(1f)
        press.animateTo(0f, tween(220))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.offset(y = (-14).dp)) {
        Led(Color(0xFFFF3B3B), if (booting) blink else 0.25f + 0.75f * press.value)
        Led(Color(0xFFFFD600), if (busy) blink else 0.25f)
        Led(Color(0xFF3DDC4A), if (booting) 1f - blink * 0.7f else if (busy) 0.25f else 1f)
    }
}

@Composable
private fun Led(color: Color, intensity: Float) {
    Canvas(Modifier.size(14.dp)) {
        val r = size.minDimension / 2f
        drawCircle(Color.Black.copy(alpha = 0.35f), r, center)
        drawCircle(lerpColor(color.copy(alpha = 1f).darken(0.55f), color, intensity), r * 0.8f, center)
        if (intensity > 0.5f) drawCircle(color.copy(alpha = 0.35f * intensity), r * 1.0f, center, style = Stroke(r * 0.4f))
        drawCircle(Color.White.copy(alpha = 0.6f), r * 0.22f, Offset(center.x - r * 0.25f, center.y - r * 0.25f))
    }
}

@Composable
private fun Hinge(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, h * 0.3f)
            lineTo(w * 0.44f, h * 0.3f)
            lineTo(w * 0.54f, h * 0.8f)
            lineTo(w, h * 0.8f)
        }
        drawPath(path, RedDeep, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        val highlight = Path().apply { addPath(path, Offset(0f, 2.dp.toPx())) }
        drawPath(highlight, RedLight.copy(alpha = 0.6f), style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun Bezel(modifier: Modifier, screen: @Composable () -> Unit) {
    val shape = CutCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 44.dp)
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(BezelLight, BezelShade)))
            .border(2.dp, Color.Black.copy(alpha = 0.18f), shape)
    ) {
        Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(2) {
                    Box(Modifier.padding(horizontal = 6.dp).size(7.dp).clip(CircleShape).background(Color(0xFFD32F2F)))
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .border(3.dp, Color(0xFF3A3A3A), RoundedCornerShape(4.dp))
                    .padding(3.dp)
            ) { screen() }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.padding(start = 18.dp).size(14.dp).clip(CircleShape).background(Color(0xFFD32F2F)).border(1.dp, RedDeep, CircleShape))
                Spacer(Modifier.weight(1f))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(4) { Box(Modifier.width(30.dp).height(2.dp).background(Color(0xFF5A5A5A))) }
                }
            }
        }
    }
}

// ─── Controls ─────────────────────────────────────────────────────────────────

@Composable
private fun Controls(
    onPad: (Pad) -> Unit,
    onA: () -> Unit,
    onB: () -> Unit,
    onStart: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        // Scale the whole control deck to the device width so it fits 360dp phones.
        val unit = min(maxWidth / 360f, 1.15.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DPad(size = unit * 118, onPad = onPad)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(unit * 10)) {
                        PillButton(stringResource(R.string.dex_btn_select), unit, onSelect)
                        PillButton(stringResource(R.string.dex_btn_start), unit, onStart)
                    }
                }
                Box(Modifier.width(unit * 112).height(unit * 104)) {
                    RoundButton("B", unit * 50, onB, Modifier.align(Alignment.BottomStart))
                    RoundButton("A", unit * 50, onA, Modifier.align(Alignment.TopEnd))
                }
            }
            Spacer(Modifier.height(unit * 8))
            Text(
                "DEXVERSE · DX-01",
                color = ShellLabel.copy(alpha = 0.9f),
                fontFamily = LcdFont,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun DPad(size: Dp, onPad: (Pad) -> Unit) {
    var pressed by remember { mutableStateOf<Pad?>(null) }
    Box(Modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val arm = this.size.width / 3f
            val w = this.size.width
            val corner = androidx.compose.ui.geometry.CornerRadius(arm * 0.18f)
            // Drop shadow
            drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(arm, 4f), Size(arm, w), corner)
            drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(0f, arm + 4f), Size(w, arm), corner)
            drawRoundRect(Plastic, Offset(arm, 0f), Size(arm, w), corner)
            drawRoundRect(Plastic, Offset(0f, arm), Size(w, arm), corner)
            val glow = PlasticLight
            when (pressed) {
                Pad.UP -> drawRect(glow, Offset(arm, 0f), Size(arm, arm))
                Pad.DOWN -> drawRect(glow, Offset(arm, arm * 2), Size(arm, arm))
                Pad.LEFT -> drawRect(glow, Offset(0f, arm), Size(arm, arm))
                Pad.RIGHT -> drawRect(glow, Offset(arm * 2, arm), Size(arm, arm))
                null -> {}
            }
            drawCircle(Color.Black.copy(alpha = 0.35f), arm * 0.28f, center)
            val t = arm * 0.22f
            val ink = Color.White.copy(alpha = 0.35f)
            fun tri(a: Offset, b: Offset, c: Offset) = drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }, ink)
            val m = w / 2f
            tri(Offset(m, arm * 0.3f), Offset(m - t, arm * 0.3f + t), Offset(m + t, arm * 0.3f + t))
            tri(Offset(m, w - arm * 0.3f), Offset(m - t, w - arm * 0.3f - t), Offset(m + t, w - arm * 0.3f - t))
            tri(Offset(arm * 0.3f, m), Offset(arm * 0.3f + t, m - t), Offset(arm * 0.3f + t, m + t))
            tri(Offset(w - arm * 0.3f, m), Offset(w - arm * 0.3f - t, m - t), Offset(w - arm * 0.3f - t, m + t))
        }
        val arm = size / 3f
        val padMods = listOf(
            Pad.UP to Alignment.TopCenter,
            Pad.DOWN to Alignment.BottomCenter,
            Pad.LEFT to Alignment.CenterStart,
            Pad.RIGHT to Alignment.CenterEnd
        )
        padMods.forEach { (dir, align) ->
            Box(
                Modifier
                    .align(align)
                    .size(arm)
                    .holdToRepeat(
                        onPress = { onPad(dir) },
                        onPressedChange = { down -> pressed = if (down) dir else null }
                    )
            )
        }
    }
}

@Composable
private fun RoundButton(label: String, size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var down by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (down) 0.9f else 1f, tween(60), label = "btn")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF5A5A5A), Plastic)))
                .border(2.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
                .holdToRepeat(repeat = false, onPress = onClick, onPressedChange = { down = it }),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Black, fontSize = 18.sp, fontFamily = LcdFont)
        }
    }
}

@Composable
private fun PillButton(label: String, unit: Dp, onClick: () -> Unit) {
    var down by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .rotate(-22f)
                .width(unit * 44)
                .height(unit * 13)
                .clip(RoundedCornerShape(50))
                .background(if (down) PlasticLight else Color(0xFF3A3A3A))
                .holdToRepeat(repeat = false, onPress = onClick, onPressedChange = { down = it })
        )
        Spacer(Modifier.height(unit * 6))
        Text(label, color = ShellLabel, fontFamily = LcdFont, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

/** Press fires immediately; holding repeats like a real D-pad. */
private fun Modifier.holdToRepeat(
    repeat: Boolean = true,
    onPress: () -> Unit,
    onPressedChange: (Boolean) -> Unit
): Modifier = composed {
    val latestPress by rememberUpdatedState(onPress)
    val latestPressed by rememberUpdatedState(onPressedChange)
    pointerInput(repeat) {
        coroutineScope {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                latestPressed(true)
                latestPress()
                val repeater = if (repeat) launch {
                    delay(380)
                    while (isActive) {
                        latestPress()
                        delay(85)
                    }
                } else null
                waitForUpOrCancellation()
                repeater?.cancel()
                latestPressed(false)
            }
        }
    }
}

// ─── LCD ──────────────────────────────────────────────────────────────────────

@Composable
private fun DexLcd(
    state: DexUiState,
    seen: Set<Int>,
    caught: Set<Int>,
    palette: LcdPalette,
    bootProgress: Float,
    movesState: LazyListState,
    onRowTap: (Int) -> Unit,
    onRetryList: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(palette.bg)) {
        when (state.screen) {
            DexScreen.BOOT -> BootLcd(palette, bootProgress)
            DexScreen.LIST -> ListLcd(state, seen, caught, palette, onRowTap, onRetryList)
            DexScreen.ENTRY -> EntryLcd(state, palette, movesState)
        }
        state.searchMiss?.let { miss ->
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(palette.ink).padding(6.dp)
            ) {
                LcdText(stringResource(R.string.dex_lcd_not_found, miss.uppercase()), palette.bg, 12.sp, maxLines = 1)
            }
        }
        Scanlines(palette)
    }
}

@Composable
private fun Scanlines(palette: LcdPalette) {
    Canvas(Modifier.fillMaxSize()) {
        val step = 3.dp.toPx()
        val line = if (palette.isColor) Color.Black.copy(alpha = 0.22f) else palette.ink.copy(alpha = 0.06f)
        var y = 0f
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
        drawRect(
            Brush.radialGradient(
                0.6f to Color.Transparent,
                1f to Color.Black.copy(alpha = if (palette.isColor) 0.35f else 0.18f),
                center = center,
                radius = size.maxDimension * 0.75f
            )
        )
        drawRect(
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                start = Offset.Zero, end = Offset(size.width * 0.6f, size.height * 0.5f)
            )
        )
    }
}

@Composable
private fun LcdText(
    text: String,
    color: Color,
    size: TextUnit = 13.sp,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Bold,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text,
        modifier = modifier,
        style = TextStyle(fontFamily = LcdFont, fontWeight = weight, fontSize = size, color = color, letterSpacing = 0.5.sp),
        maxLines = maxLines,
        overflow = TextOverflow.Clip
    )
}

@Composable
private fun BootLcd(palette: LcdPalette, progress: Float) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PixelBall(palette.ink, Modifier.size(56.dp).graphicsLayer { rotationZ = progress * 360f })
        Spacer(Modifier.height(14.dp))
        LcdText("DEXVERSE", palette.ink, 28.sp, weight = FontWeight.Black)
        LcdText(stringResource(R.string.dex_lcd_national), palette.inkSoft, 11.sp)
        Spacer(Modifier.height(22.dp))
        LcdText(stringResource(R.string.dex_lcd_system_check), palette.ink, 12.sp)
        Spacer(Modifier.height(6.dp))
        val blocks = 12
        val filled = (progress * blocks).toInt()
        LcdText("[" + "█".repeat(filled) + "·".repeat(blocks - filled) + "]", palette.ink, 14.sp)
        if (progress >= 1f) {
            Spacer(Modifier.height(10.dp))
            LcdText("OK!", palette.ink, 14.sp, weight = FontWeight.Black)
        }
    }
}

@Composable
private fun PixelBall(ink: Color, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val s = 3.dp.toPx()
        drawCircle(ink, r - s / 2, center, style = Stroke(s))
        drawLine(ink, Offset(center.x - r, center.y), Offset(center.x + r, center.y), s)
        drawCircle(ink, r * 0.3f, center, style = Stroke(s))
        drawArc(ink, 180f, 180f, true, topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2), alpha = 0.35f)
    }
}

@Composable
private fun ListLcd(
    state: DexUiState,
    seen: Set<Int>,
    caught: Set<Int>,
    palette: LcdPalette,
    onRowTap: (Int) -> Unit,
    onRetryList: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LcdText(stringResource(R.string.dex_lcd_national), palette.ink, 11.sp)
            Spacer(Modifier.weight(1f))
            LcdText(
                stringResource(R.string.dex_lcd_seen_count, seen.size, DexModeViewModel.NATIONAL_DEX_SIZE),
                palette.ink, 11.sp
            )
            Spacer(Modifier.width(8.dp))
            LcdText(stringResource(R.string.dex_lcd_caught_count, caught.size), palette.ink, 11.sp)
        }
        Spacer(Modifier.height(4.dp))

        val current = state.current
        if (state.listLoading) {
            LcdCentered(palette) { BlinkingText(stringResource(R.string.dex_lcd_loading), palette) }
            return@Column
        }
        if (state.listError || current == null) {
            LcdCentered(palette, Modifier.clickable(onClick = onRetryList)) {
                LcdText(stringResource(R.string.dex_lcd_no_signal), palette.ink, 16.sp, weight = FontWeight.Black)
                LcdText(stringResource(R.string.dex_lcd_tap_retry), palette.inkSoft, 11.sp)
            }
            return@Column
        }

        val isSeen = current.id in seen
        val isCaught = current.id in caught
        Row(
            Modifier.fillMaxWidth().background(palette.bgDark).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(84.dp).border(2.dp, palette.ink).background(palette.bg),
                contentAlignment = Alignment.Center
            ) {
                DexSprite(dexSpriteUrl(current.id), isSeen, palette, Modifier.fillMaxSize().padding(4.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                LcdText("No.%04d".format(current.id), palette.inkSoft, 12.sp)
                LcdText(dexName(current.name), palette.ink, 17.sp, weight = FontWeight.Black, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                LcdText(
                    stringResource(
                        when {
                            isCaught -> R.string.dex_lcd_caught
                            isSeen -> R.string.dex_lcd_seen
                            else -> R.string.dex_lcd_not_seen
                        }
                    ),
                    palette.ink, 11.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        DottedRule(palette)

        val listState = rememberLazyListState()
        LaunchedEffect(state.cursor) {
            val visible = listState.layoutInfo.visibleItemsInfo
            val first = visible.firstOrNull()?.index ?: 0
            val last = visible.lastOrNull()?.index ?: 0
            if (visible.isEmpty() || state.cursor <= first || state.cursor >= last) {
                listState.scrollToItem((state.cursor - (visible.size / 2).coerceAtLeast(2)).coerceAtLeast(0))
            }
        }
        val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
            initialValue = 1f, targetValue = 0.2f,
            animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "cursor_blink"
        )
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(state.entries, key = { _, e -> e.id }) { index, entry ->
                val selected = index == state.cursor
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .background(if (selected) palette.bgDark else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onRowTap(index) }
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LcdText(
                        if (selected) "▶" else " ",
                        palette.ink.copy(alpha = if (selected) blink else 1f),
                        13.sp,
                        Modifier.width(16.dp)
                    )
                    LcdText("%04d".format(entry.id), palette.inkSoft, 13.sp, Modifier.width(48.dp))
                    LcdText(dexName(entry.name), palette.ink, 13.sp, Modifier.weight(1f), maxLines = 1)
                    when {
                        entry.id in caught -> MiniBall(palette.ink)
                        entry.id in seen -> LcdText("•", palette.ink, 13.sp)
                        else -> LcdText("-", palette.inkSoft, 13.sp)
                    }
                }
            }
        }
        DottedRule(palette)
        LcdText(stringResource(R.string.dex_lcd_hint_list), palette.inkSoft, 10.sp, Modifier.padding(top = 3.dp), maxLines = 1)
    }
}

@Composable
private fun EntryLcd(state: DexUiState, palette: LcdPalette, movesState: LazyListState) {
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
        TabStrip(state.tab, palette)
        Spacer(Modifier.height(4.dp))
        DottedRule(palette)
        Spacer(Modifier.height(4.dp))

        val detail = state.detail
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.detailLoading -> LcdCentered(palette) {
                    PixelBallSpinner(palette)
                    Spacer(Modifier.height(8.dp))
                    BlinkingText(stringResource(R.string.dex_lcd_loading), palette)
                }
                state.detailError || detail == null -> LcdCentered(palette) {
                    LcdText(stringResource(R.string.dex_lcd_no_signal), palette.ink, 16.sp, weight = FontWeight.Black)
                    LcdText(stringResource(R.string.dex_lcd_press_a_retry), palette.inkSoft, 11.sp)
                }
                else -> when (state.tab) {
                    DexTab.INFO -> InfoPage(detail, palette)
                    DexTab.STATS -> StatsPage(detail, palette)
                    DexTab.MOVES -> MovesPage(detail, palette, movesState)
                }
            }
        }
        DottedRule(palette)
        LcdText(
            stringResource(if (state.tab == DexTab.MOVES) R.string.dex_lcd_hint_moves else R.string.dex_lcd_hint_entry),
            palette.inkSoft, 10.sp, Modifier.padding(top = 3.dp), maxLines = 1
        )
    }
}

@Composable
private fun TabStrip(tab: DexTab, palette: LcdPalette) {
    val labels = listOf(
        DexTab.INFO to stringResource(R.string.dex_lcd_tab_info),
        DexTab.STATS to stringResource(R.string.dex_lcd_tab_stats),
        DexTab.MOVES to stringResource(R.string.dex_lcd_tab_moves)
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LcdText("◀", palette.inkSoft, 12.sp)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            labels.forEach { (t, label) ->
                val on = t == tab
                LcdText(
                    label,
                    if (on) palette.bg else palette.ink,
                    12.sp,
                    Modifier.background(if (on) palette.ink else Color.Transparent).padding(horizontal = 6.dp, vertical = 1.dp),
                    weight = FontWeight.Black
                )
            }
        }
        LcdText("▶", palette.inkSoft, 12.sp)
    }
}

@Composable
private fun InfoPage(detail: DexEntryDetail, palette: LcdPalette) {
    // Seen-for-the-first-time flash, like the old "registered" moment.
    val flash = remember(detail.id) { Animatable(1f) }
    LaunchedEffect(detail.id) { flash.animateTo(0f, tween(450)) }

    val flavor = detail.flavor ?: stringResource(R.string.dex_lcd_no_data)
    var typed by remember(detail.id) { mutableIntStateOf(0) }
    LaunchedEffect(detail.id) {
        delay(350)
        while (typed < flavor.length) {
            delay(24)
            typed++
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.size(104.dp).border(2.dp, palette.ink).background(palette.bgDark), contentAlignment = Alignment.Center) {
                DexSprite(detail.spriteUrl, seen = true, palette = palette, modifier = Modifier.fillMaxSize().padding(4.dp))
                Box(Modifier.fillMaxSize().background(palette.light.copy(alpha = flash.value)))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                LcdText("No.%04d".format(detail.id), palette.inkSoft, 12.sp)
                LcdText(dexName(detail.name), palette.ink, 17.sp, weight = FontWeight.Black, maxLines = 1)
                detail.genus?.let { LcdText(it.uppercase(), palette.ink, 11.sp, maxLines = 1) }
                Spacer(Modifier.height(4.dp))
                LcdText("%s %.1fm".format(stringResource(R.string.dex_lcd_height), detail.heightM), palette.ink, 12.sp)
                LcdText("%s %.1fkg".format(stringResource(R.string.dex_lcd_weight), detail.weightKg), palette.ink, 12.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    detail.types.forEach { type ->
                        LcdText(
                            localizedTypeName(type).uppercase(), palette.ink, 10.sp,
                            Modifier.border(1.dp, palette.ink).padding(horizontal = 4.dp, vertical = 1.dp),
                            maxLines = 1
                        )
                    }
                }
            }
        }
        // A few lines of air between the portrait/details and the description.
        Spacer(Modifier.height(18.dp))
        DottedRule(palette)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            LcdText(flavor.take(typed) + if (typed < flavor.length) "▌" else "", palette.ink, 13.sp)
        }
    }
}

@Composable
private fun StatsPage(detail: DexEntryDetail, palette: LcdPalette) {
    val labels = mapOf(
        "hp" to stringResource(R.string.dex_stat_hp),
        "attack" to stringResource(R.string.dex_stat_atk),
        "defense" to stringResource(R.string.dex_stat_def),
        "special-attack" to stringResource(R.string.dex_stat_spatk),
        "special-defense" to stringResource(R.string.dex_stat_spdef),
        "speed" to stringResource(R.string.dex_stat_spd)
    )
    val grow = remember(detail.id) { Animatable(0f) }
    LaunchedEffect(detail.id) { grow.animateTo(1f, tween(600)) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LcdText("${dexName(detail.name)}  No.%04d".format(detail.id), palette.ink, 12.sp, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        detail.stats.forEach { (name, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                LcdText(labels[name] ?: name.uppercase(), palette.ink, 12.sp, Modifier.width(64.dp), maxLines = 1)
                LcdText("%3d".format(value), palette.ink, 12.sp, Modifier.width(34.dp))
                StatBar(value * grow.value, palette, Modifier.weight(1f).height(10.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        DottedRule(palette)
        LcdText("%s %d".format(stringResource(R.string.dex_lcd_total), detail.stats.sumOf { it.second }), palette.ink, 13.sp, weight = FontWeight.Black)
    }
}

@Composable
private fun StatBar(value: Float, palette: LcdPalette, modifier: Modifier) {
    Canvas(modifier) {
        val segments = 16
        val gap = 2.dp.toPx()
        val segW = (size.width - gap * (segments - 1)) / segments
        val filled = (value / 200f * segments).coerceIn(0f, segments.toFloat())
        for (i in 0 until segments) {
            val on = i < filled
            drawRect(
                if (on) palette.ink else palette.ink.copy(alpha = 0.15f),
                Offset(i * (segW + gap), 0f),
                Size(segW, size.height)
            )
        }
    }
}

@Composable
private fun MovesPage(detail: DexEntryDetail, palette: LcdPalette, movesState: LazyListState) {
    if (detail.moves.isEmpty()) {
        LcdCentered(palette) { LcdText(stringResource(R.string.dex_lcd_no_data), palette.ink, 13.sp) }
        return
    }
    val lv = stringResource(R.string.dex_lcd_lv)
    LazyColumn(state = movesState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(detail.moves) { _, (level, move) ->
            Row(Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
                LcdText(if (level <= 1) "$lv --" else "$lv%2d".format(level), palette.inkSoft, 12.sp, Modifier.width(56.dp))
                LcdText(move.replace('-', ' ').uppercase(), palette.ink, 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DexSprite(url: String?, seen: Boolean, palette: LcdPalette, modifier: Modifier) {
    val context = LocalContext.current
    val filter = remember(seen, palette) {
        when {
            !seen -> ColorFilter.tint(if (palette.isColor) palette.inkSoft else palette.ink, BlendMode.SrcIn)
            palette.isColor -> null
            else -> greenScreenFilter(palette)
        }
    }
    AsyncImage(
        model = remember(url) { ImageRequest.Builder(context).data(url).crossfade(false).build() },
        contentDescription = null,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        colorFilter = filter,
        modifier = modifier
    )
}

/** Maps sprite luminance onto the green LCD ramp (dark pixels → ink, light → screen). */
private fun greenScreenFilter(p: LcdPalette): ColorFilter {
    fun row(ink: Float, light: Float): FloatArray {
        val d = light - ink
        return floatArrayOf(0.299f * d, 0.587f * d, 0.114f * d, 0f, ink * 255f)
    }
    val m = row(p.ink.red, p.light.red) + row(p.ink.green, p.light.green) + row(p.ink.blue, p.light.blue) +
        floatArrayOf(0f, 0f, 0f, 1f, 0f)
    return ColorFilter.colorMatrix(ColorMatrix(m))
}

@Composable
private fun MiniBall(ink: Color) {
    Canvas(Modifier.size(12.dp)) {
        val r = size.minDimension / 2f
        drawArc(ink, 180f, 180f, true, topLeft = Offset.Zero, size = size)
        drawCircle(ink, r - 1.dp.toPx() / 2, center, style = Stroke(1.dp.toPx()))
        drawCircle(ink, r * 0.3f, center)
    }
}

@Composable
private fun PixelBallSpinner(palette: LcdPalette) {
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900)), label = "spin_deg"
    )
    // Step the rotation in 45° jumps for a low-framerate LCD feel.
    PixelBall(palette.ink, Modifier.size(40.dp).graphicsLayer { rotationZ = (spin / 45f).toInt() * 45f })
}

@Composable
private fun BlinkingText(text: String, palette: LcdPalette) {
    val a by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "blink_a"
    )
    LcdText(text, palette.ink.copy(alpha = if (a > 0.5f) 1f else 0.15f), 14.sp, weight = FontWeight.Black)
}

@Composable
private fun LcdCentered(palette: LcdPalette, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { content() }
}

@Composable
private fun DottedRule(palette: LcdPalette) {
    Canvas(Modifier.fillMaxWidth().height(2.dp)) {
        val step = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawRect(palette.ink.copy(alpha = 0.6f), Offset(x, 0f), Size(step / 2f, size.height))
            x += step
        }
    }
}

@Composable
private fun TextSearchDialog(onSearch: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dex_search_title)) },
        text = {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.dex_search_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onSearch(query) })
            )
        },
        confirmButton = {
            TextButton(onClick = { if (query.isNotBlank()) onSearch(query) }) { Text(stringResource(R.string.dex_search_go)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

// ─── Small colour helpers ─────────────────────────────────────────────────────

private fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

private fun Color.darken(f: Float): Color = Color(red * f, green * f, blue * f, alpha)
