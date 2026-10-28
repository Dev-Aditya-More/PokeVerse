package com.aditya1875.pokeverse.feature.pokemon.onboarding

import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import androidx.activity.compose.BackHandler
import com.aditya1875.pokeverse.feature.core.ui.components.ForceDarkSystemBars
import com.aditya1875.pokeverse.feature.core.ui.components.introBackground
import com.aditya1875.pokeverse.feature.core.ui.components.onAccent
import com.aditya1875.pokeverse.feature.core.ui.components.readableOnDark
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.core.navigation.components.Route
import com.aditya1875.pokeverse.feature.core.ui.components.DialogueBox
import com.aditya1875.pokeverse.feature.pokemon.dex.PokeBallIcon
import com.aditya1875.pokeverse.feature.core.ui.components.darkFieldColors
import com.aditya1875.pokeverse.feature.core.ui.components.rememberTypewriter
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.TrainerChip
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels.SettingsViewModel
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private enum class IntroStep { HELLO_1, HELLO_2, HELLO_3, NAME, STARTER, CONFIRM, FINALE }

private const val NAME_MAX = 16

/**
 * First-run experience: a professor greets you, asks your name, and offers a starter. The result
 * becomes the trainer's name, avatar and first favorite type. Skip drops straight into the app.
 */
@Composable
fun ProfessorIntroScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = koinViewModel(),
) {
    val sound: SoundManager = koinInject()
    val pokedex: PokedexRepository = koinInject()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val assetsEnabled by settingsViewModel.originalAssetsEnabled.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(IntroStep.HELLO_1) }
    var name by remember { mutableStateOf("") }
    var starter by remember { mutableStateOf<Starter?>(null) }
    var finishing by remember { mutableStateOf(false) }

    val accent = starter?.let { pokemonTypeColor(it.type) } ?: MaterialTheme.colorScheme.primary
    val safeName = name.trim()

    val prompt = when (step) {
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
        IntroStep.FINALE -> stringResource(R.string.pro_finale, safeName)
    }
    val typewriter = rememberTypewriter(prompt)

    val canAdvance = when (step) {
        IntroStep.NAME -> safeName.isNotEmpty()
        IntroStep.STARTER -> starter != null
        else -> true
    }

    fun leave() {
        navController.navigate(Route.BottomBar.Home.route) {
            popUpTo(Route.Onboarding.route) { inclusive = true }
        }
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

    fun advance() {
        sound.play(SoundManager.Sound.BUTTON_CLICK)
        if (step == IntroStep.CONFIRM && starter == Starter.PIKACHU) {
            sound.play(SoundManager.Sound.PIKACHU_CRY)
        }
        val next = IntroStep.entries.getOrNull(step.ordinal + 1)
        if (next != null) {
            step = next
            return
        }
        val chosen = starter ?: return
        if (finishing) return
        finishing = true
        scope.launch {
            sound.play(SoundManager.Sound.LEVEL_UP)
            profileViewModel.saveStarter(safeName, chosen)
            ScreenStateManager.markIntroSeen(context)
            leave()
        }
    }

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
        val previousStep = IntroStep.entries.getOrNull(step.ordinal - 1)
        BackHandler(enabled = previousStep != null && !finishing) { previousStep?.let { step = it } }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = ::skip) {
                Text(stringResource(R.string.pro_skip), color = Color.White.copy(alpha = 0.6f))
            }
        }

        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 4 } + fadeOut())
            },
            label = "introStep",
        ) { current ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Choices wait for the line to finish typing, like the games.
                AnimatedVisibility(
                    visible = !typewriter.typing || current != step || current <= IntroStep.HELLO_3,
                    enter = fadeIn()
                ) {
                    when (current) {
                        IntroStep.HELLO_1, IntroStep.HELLO_2, IntroStep.HELLO_3 ->
                            ProfessorMascot(talking = typewriter.typing && current == step, accent = accent)

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
                            modifier = Modifier.fillMaxWidth(),
                        )

                        IntroStep.STARTER -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Starter.entries.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    pair.forEach { s ->
                                        StarterCard(
                                            starter = s,
                                            selected = starter == s,
                                            assetsEnabled = assetsEnabled,
                                            onClick = {
                                                sound.play(SoundManager.Sound.BUTTON_CLICK)
                                                starter = s
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }

                        IntroStep.CONFIRM -> starter?.let { StarterShowcase(it, assetsEnabled, size = 190.dp) }

                        IntroStep.FINALE -> starter?.let { StarterShowcase(it, assetsEnabled, size = 220.dp) }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "PROFESSOR DEX",
            color = accent.readableOnDark(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
        )
        DialogueBox(typewriter = typewriter, accent = accent)
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val prev = IntroStep.entries.getOrNull(step.ordinal - 1)
            if (prev != null) {
                TextButton(onClick = { step = prev }) {
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
                        IntroStep.FINALE -> stringResource(R.string.pro_begin)
                        else -> stringResource(R.string.tc_next)
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun StarterCard(
    starter: Starter,
    selected: Boolean,
    assetsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = pokemonTypeColor(starter.type)
    val lift by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "starterLift",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .scale(lift)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
            .border(
                BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) color else Color.White.copy(alpha = 0.15f)),
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        AsyncImage(
            model = starter.artworkUrl,
            contentDescription = starter.displayName,
            colorFilter = starterFilter(assetsEnabled),
            modifier = Modifier.size(104.dp),
        )
        Text(starter.displayName, color = Color.White, fontWeight = FontWeight.Bold)
        TrainerChip(localizedTypeName(starter.type), color)
    }
}

/** Franchise artwork stays a silhouette until the user enables Original Assets, like everywhere else in the app. */
private fun starterFilter(assetsEnabled: Boolean): ColorFilter? =
    if (assetsEnabled) null else ColorFilter.tint(Color.Black, BlendMode.SrcAtop)

/**
 * Stand-in for the professor: a Poké Ball that floats, glows, and rattles while he talks.
 * Drawn in code, so it needs no franchise artwork and works with Original Assets off.
 */
@Composable
private fun ProfessorMascot(talking: Boolean, accent: Color) {
    val transition = rememberInfiniteTransition(label = "mascot")
    val bob by transition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val chatter by transition.animateFloat(
        initialValue = -9f, targetValue = 9f,
        animationSpec = infiniteRepeatable(tween(240, easing = LinearEasing), RepeatMode.Reverse),
        label = "chatter",
    )
    val glow by transition.animateFloat(
        initialValue = 0.18f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(accent.copy(alpha = glow), Color.Transparent)), CircleShape)
        )
        PokeBallIcon(
            Modifier
                .size(124.dp)
                .graphicsLayer {
                    translationY = bob
                    rotationZ = if (talking) chatter else 0f
                }
        )
    }
}

/** The chosen starter, popping in with a springy scale. */
@Composable
private fun StarterShowcase(starter: Starter, assetsEnabled: Boolean, size: Dp) {
    var visible by remember(starter) { mutableStateOf(false) }
    LaunchedEffect(starter) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "starterPop",
    )
    AsyncImage(
        model = starter.artworkUrl,
        contentDescription = starter.displayName,
        colorFilter = starterFilter(assetsEnabled),
        modifier = Modifier.size(size).scale(scale),
    )
}
