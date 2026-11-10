package com.aditya1875.pokeverse.feature.pokemon.onboarding

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.compare.presentation.components.ConfettiBurst
import com.aditya1875.pokeverse.feature.core.navigation.components.Route
import com.aditya1875.pokeverse.feature.core.ui.components.DialogueBox
import com.aditya1875.pokeverse.feature.core.ui.components.ForceDarkSystemBars
import com.aditya1875.pokeverse.feature.core.ui.components.darkFieldColors
import com.aditya1875.pokeverse.feature.core.ui.components.introBackground
import com.aditya1875.pokeverse.feature.core.ui.components.onAccent
import com.aditya1875.pokeverse.feature.core.ui.components.readableOnDark
import com.aditya1875.pokeverse.feature.core.ui.components.rememberTypewriter
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.TrainerChip
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels.SettingsViewModel
import com.aditya1875.pokeverse.utils.Haptics
import com.aditya1875.pokeverse.utils.LocaleHelper
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Declaration order is play order. NOTIFY is dropped at runtime where there is nothing to ask for
 * (pre-Android 13, or already granted).
 */
private enum class IntroStep {
    LANGUAGE, HELLO_1, HELLO_2, HELLO_3, NAME, STARTER, CONFIRM,
    TOUR_DEX, TOUR_GAMES, TOUR_RANK, TOUR_STYLE, NOTIFY, FINALE
}

private const val NAME_MAX = 16

private fun IntroStep.mood(): ProfessorMood = when (this) {
    IntroStep.LANGUAGE, IntroStep.HELLO_1, IntroStep.CONFIRM, IntroStep.TOUR_DEX, IntroStep.TOUR_STYLE -> ProfessorMood.HAPPY
    IntroStep.HELLO_2 -> ProfessorMood.NEUTRAL
    IntroStep.HELLO_3, IntroStep.STARTER, IntroStep.TOUR_GAMES, IntroStep.TOUR_RANK, IntroStep.FINALE -> ProfessorMood.EXCITED
    IntroStep.NAME, IntroStep.NOTIFY -> ProfessorMood.THINKING
}

/** Big while he's just talking; shrinks to make room when there is something to look at or fill in. */
private fun IntroStep.mascotSize(): Dp = when (this) {
    IntroStep.LANGUAGE, IntroStep.HELLO_1, IntroStep.HELLO_2, IntroStep.HELLO_3 -> 190.dp
    IntroStep.NAME -> 120.dp
    IntroStep.STARTER -> 80.dp
    IntroStep.FINALE -> 110.dp
    else -> 96.dp
}

/**
 * First-run experience: Professor Dex greets you, asks your language and name, offers a starter,
 * walks you through the app, and asks about notifications. The result becomes the trainer's name,
 * avatar and first favorite type. Skip drops straight into the app.
 *
 * Progress is saved across configuration changes, because choosing a language recreates the
 * activity mid-flow.
 */
@Composable
fun ProfessorIntroScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = koinViewModel(),
) {
    val sound: SoundManager = koinInject()
    val haptics: Haptics = koinInject()
    val pokedex: PokedexRepository = koinInject()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val assetsEnabled = true
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val steps = remember {
        IntroStep.entries.filter {
            it != IntroStep.NOTIFY || (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                )
        }
    }

    var stepName by rememberSaveable { mutableStateOf(IntroStep.LANGUAGE.name) }
    var name by rememberSaveable { mutableStateOf("") }
    var starterId by rememberSaveable { mutableStateOf<String?>(null) }
    var nudge by remember { mutableIntStateOf(0) }
    var finishing by remember { mutableStateOf(false) }

    val step = IntroStep.valueOf(stepName)
    val starter = starterId?.let { Starter.fromId(it) }
    val nextStep = steps.firstOrNull { it.ordinal > step.ordinal }
    val previousStep = steps.lastOrNull { it.ordinal < step.ordinal }
    val stepIndex = steps.indexOf(step).coerceAtLeast(0)

    val accent = starter?.let { pokemonTypeColor(it.type) } ?: MaterialTheme.colorScheme.primary
    val safeName = name.trim()

    val prompt = when (step) {
        IntroStep.LANGUAGE -> stringResource(R.string.pro_language_greeting)
        IntroStep.HELLO_1 -> stringResource(R.string.pro_hello_1)
        IntroStep.HELLO_2 -> stringResource(R.string.pro_hello_2)
        IntroStep.HELLO_3 -> stringResource(R.string.pro_hello_3)
        IntroStep.NAME -> stringResource(R.string.pro_ask_name)
        IntroStep.STARTER -> stringResource(R.string.pro_ask_starter, safeName)
        IntroStep.CONFIRM -> stringResource(
            R.string.pro_confirm_starter,
            starter?.displayName.orEmpty(),
            starter?.let { localizedTypeName(it.type) }.orEmpty(),
        )
        IntroStep.TOUR_DEX -> stringResource(R.string.pro_tour_dex, starter?.displayName.orEmpty())
        IntroStep.TOUR_GAMES -> stringResource(R.string.pro_tour_games)
        IntroStep.TOUR_RANK -> stringResource(R.string.pro_tour_rank, safeName)
        IntroStep.TOUR_STYLE -> stringResource(R.string.pro_tour_style)
        IntroStep.NOTIFY -> stringResource(R.string.pro_ask_notify)
        IntroStep.FINALE -> stringResource(R.string.pro_finale, safeName)
    }
    val typewriter = rememberTypewriter(prompt, charDelayMs = if (prompt.length > 110) 16L else 24L)

    val canAdvance = when (step) {
        IntroStep.NAME -> safeName.isNotEmpty()
        IntroStep.STARTER -> starter != null
        else -> true
    }

    // The celebration belongs to arriving at the finale, not to tapping the last button.
    LaunchedEffect(step) {
        if (step == IntroStep.FINALE) sound.play(SoundManager.Sound.LEVEL_UP)
    }

    fun leave() {
        navController.navigate(Route.BottomBar.Home.route) {
            popUpTo(Route.Onboarding.route) { inclusive = true }
        }
    }

    fun go(target: IntroStep) {
        stepName = target.name
        nudge++
    }

    fun skip() {
        if (finishing) return
        finishing = true
        scope.launch {
            pokedex.ensureDefaultStarter() // skipping still leaves you with Pikachu
            ScreenStateManager.markIntroSeen(context)
            leave()
        }
    }

    fun goNext() {
        val next = nextStep
        if (next != null) {
            go(next)
            return
        }
        val chosen = starter ?: return
        if (finishing) return
        finishing = true
        scope.launch {
            profileViewModel.saveStarter(safeName, chosen)
            ScreenStateManager.markIntroSeen(context)
            leave()
        }
    }

    // Whatever the answer, the conversation moves on; the system dialog is the only real decision.
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { goNext() }

    fun advance() {
        sound.play(SoundManager.Sound.BUTTON_CLICK)
        if (step == IntroStep.CONFIRM && starter == Starter.PIKACHU) {
            sound.play(SoundManager.Sound.PIKACHU_CRY)
        }
        if (step == IntroStep.NOTIFY && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        goNext()
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .introBackground(accent)
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            ForceDarkSystemBars()
            // Back walks back through the dialogue; on the first line it falls through to the system.
            BackHandler(enabled = previousStep != null && !finishing) { previousStep?.let(::go) }

            // Progress + skip.
            val progressDescription = stringResource(R.string.pro_cd_progress, stepIndex + 1, steps.size)
            val progress by animateFloatAsState(
                targetValue = (stepIndex + 1) / steps.size.toFloat(),
                animationSpec = tween(450),
                label = "introProgress",
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .semantics { contentDescription = progressDescription },
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(accent.readableOnDark()),
                    )
                }
                TextButton(onClick = ::skip) {
                    Text(stringResource(R.string.pro_skip), color = Color.White.copy(alpha = 0.6f))
                }
            }

            // Stage: the professor stays put and reacts; only what's under him changes.
            val mascotSize by animateDpAsState(step.mascotSize(), tween(350), label = "mascotSize")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ProfessorBall(
                    mood = step.mood(),
                    talking = typewriter.typing,
                    accent = accent,
                    nudge = nudge,
                    modifier = Modifier.size(mascotSize),
                )
                AnimatedContent(
                    targetState = step,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    transitionSpec = {
                        (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 4 } + fadeOut())
                    },
                    label = "introStep",
                ) { current ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        when (current) {
                            IntroStep.HELLO_1, IntroStep.HELLO_2, IntroStep.HELLO_3 -> Spacer(Modifier.height(1.dp))

                            IntroStep.LANGUAGE -> LanguageChoices(
                                accent = accent,
                                onPick = { tag ->
                                    sound.play(SoundManager.Sound.BUTTON_CLICK)
                                    if (tag != LocaleHelper.getSelectedLanguage(context)) {
                                        settingsViewModel.setLanguage(tag)
                                        // The locale is applied when the activity is created, so
                                        // recreate; progress survives via rememberSaveable.
                                        context.findActivity()?.recreate()
                                    }
                                },
                            )

                            IntroStep.NAME -> OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.take(NAME_MAX) },
                                label = { Text(stringResource(R.string.profile_name_label)) },
                                singleLine = true,
                                colors = darkFieldColors(accent),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { if (canAdvance && !typewriter.typing && !finishing) advance() },
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            IntroStep.STARTER -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Starter.entries.chunked(2).forEach { pair ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        pair.forEach { s ->
                                            StarterCard(
                                                starter = s,
                                                selected = starter == s,
                                                showArtwork = assetsEnabled,
                                                onClick = {
                                                    // The pick has its own haptic; keep the click sound from adding another.
                                                    sound.play(SoundManager.Sound.BUTTON_CLICK, haptic = false)
                                                    haptics.play(Haptics.Haptic.POP)
                                                    starterId = s.id
                                                },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                }
                            }

                            // Still a silhouette: the entry isn't registered until the player says yes.
                            IntroStep.CONFIRM -> starter?.let { StarterShowcase(it, reveal = false, size = 160.dp) }

                            IntroStep.TOUR_DEX -> starter?.let { TourDexVisual(it, assetsEnabled, accent) }
                            IntroStep.TOUR_GAMES -> TourGamesVisual()
                            IntroStep.TOUR_RANK -> TourRankVisual(safeName, accent)
                            IntroStep.TOUR_STYLE -> TourStyleVisual()

                            IntroStep.NOTIFY -> NotifyContent(
                                accent = accent,
                                onLater = {
                                    sound.play(SoundManager.Sound.BUTTON_CLICK)
                                    goNext()
                                },
                            )

                            IntroStep.FINALE -> starter?.let { StarterShowcase(it, reveal = assetsEnabled, size = 190.dp) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.pro_name_tag),
                color = accent.readableOnDark(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
            )
            DialogueBox(typewriter = typewriter, accent = accent)
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (previousStep != null) {
                    TextButton(onClick = { go(previousStep) }) {
                        Text(stringResource(R.string.tc_back), color = Color.White.copy(alpha = 0.7f))
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = ::advance,
                    enabled = canAdvance && !typewriter.typing && !finishing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = accent.onAccent()),
                ) {
                    Text(
                        when (step) {
                            IntroStep.CONFIRM -> stringResource(R.string.pro_yes)
                            IntroStep.NOTIFY -> stringResource(R.string.pro_notify_allow)
                            IntroStep.FINALE -> stringResource(R.string.pro_begin)
                            else -> stringResource(R.string.tc_next)
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            }
        }

        if (step == IntroStep.FINALE) ConfettiBurst(visible = true)
    }
}

@Composable
private fun LanguageChoices(accent: Color, onPick: (String) -> Unit) {
    val context = LocalContext.current
    val selected = LocaleHelper.getSelectedLanguage(context)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LocaleHelper.supportedLanguages.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (tag, label) ->
                    ChoiceCard(
                        selected = tag == selected,
                        accent = accent,
                        onClick = { onPick(tag) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/** The shared selectable-card chrome: springs up when picked and takes the accent colour. */
@Composable
private fun ChoiceCard(
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val lift by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "choiceLift",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .scale(lift)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
            .border(BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) accent else Color.White.copy(alpha = 0.15f)), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A bell with ripples, and the quiet way out. The real yes is the main button. */
@Composable
private fun NotifyContent(accent: Color, onLater: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "bell")
    val ripple by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ripple",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(70.dp)
                    .graphicsLayer {
                        scaleX = 1f + ripple * 0.6f
                        scaleY = 1f + ripple * 0.6f
                        alpha = (1f - ripple) * 0.5f
                    }
                    .border(BorderStroke(2.dp, accent), CircleShape),
            )
            Box(
                Modifier.size(70.dp).clip(CircleShape).background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = null, tint = accent.readableOnDark(), modifier = Modifier.size(34.dp))
            }
        }
        TextButton(onClick = onLater) {
            Text(stringResource(R.string.pro_notify_later), color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun StarterCard(
    starter: Starter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = false,
) {
    val color = pokemonTypeColor(starter.type)
    val lift by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "starterLift",
    )
    // Compact side-by-side layout: four starters (artwork + name + type) fit above the dialog box
    // even on a short phone, with no scrolling needed to see all of them.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .scale(lift)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
            .border(
                BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) color else Color.White.copy(alpha = 0.15f)),
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        // A glow in the type's colour sits behind the artwork so each starter reads at a glance.
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(68.dp)) {
            Box(
                Modifier.matchParentSize().background(
                    Brush.radialGradient(listOf(color.copy(alpha = if (selected) 0.55f else 0.32f), Color.Transparent)),
                ),
            )
            AsyncImage(
                model = starter.artworkUrl,
                contentDescription = starter.displayName,
                // Not registered yet, so no real colours: a silhouette in the type's colour, unless the
                // player has Original Assets on.
                colorFilter = if (showArtwork) null else ColorFilter.tint(color.copy(alpha = 0.95f), BlendMode.SrcAtop),
                modifier = Modifier.size(64.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                starter.displayName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TrainerChip(localizedTypeName(starter.type), color)
        }
    }
}

/**
 * The chosen starter, popping in with a springy scale. With [reveal] the silhouette turns into the
 * artwork; without it (artwork not enabled, or the entry isn't registered yet) it stays a silhouette.
 */
@Composable
private fun StarterShowcase(starter: Starter, reveal: Boolean, size: Dp) {
    var visible by remember(starter) { mutableStateOf(false) }
    LaunchedEffect(starter) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "starterPop",
    )
    RevealArt(
        url = starter.artworkUrl,
        name = starter.displayName,
        reveal = reveal,
        modifier = Modifier.size(size).scale(scale),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
