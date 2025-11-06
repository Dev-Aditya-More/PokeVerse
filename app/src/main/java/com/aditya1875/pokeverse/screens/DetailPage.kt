package com.aditya1875.pokeverse.screens

import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.components.LayeredWaveformVisualizer
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionStage
import com.aditya1875.pokeverse.specialscreens.ParticleBackground
import com.aditya1875.pokeverse.specialscreens.getParticleTypeFor
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.utils.TeamMapper.toEntity
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun PokemonDetailPage(
    currentStage: EvolutionStage?,
    navController: NavController,
    specialEffectsEnabled: Boolean,
    spriteEffectsEnabledState: MutableState<Boolean>,
    viewModel: PokemonViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pokemon = uiState.pokemon
    val spriteEffectsEnabled = spriteEffectsEnabledState.value
    val typeList = pokemon?.types?.map { it.type.name } ?: emptyList()
    val currentNameForBg = currentStage?.name ?: pokemon?.name ?: ""
    val context = LocalContext.current
    var isTtsReady by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.beepeffect) }
    var isSpeaking by remember { mutableStateOf(false) }
    var showLoader by remember { mutableStateOf(false) }
    val name =
        pokemon?.name?.replaceFirstChar { it.uppercase() } ?: "This Pokémon"
    val type = pokemon?.types?.firstOrNull()?.type?.name ?: "unknown type"
    val descriptionText =
        pokemon?.id?.let { viewModel.getLocalDescription(it) } ?: ""
    val cleanText =
        descriptionText.replace(Regex("[^\\x00-\\x7F]"), " ")
            .replace("\n", " ")
            .trim()
    val speechText = "$name. A $type type Pokémon. $cleanText"

    var isSpriteChanged by rememberSaveable { mutableStateOf(false) }
    var currentSpriteUrl by rememberSaveable {
        mutableStateOf(
            pokemon?.sprites?.other?.officialArtwork?.frontDefault
                ?: pokemon?.sprites?.other?.home?.frontDefault
                ?: currentStage?.imageUrl
        )
    }

    LaunchedEffect(pokemon) {
        currentSpriteUrl = pokemon?.sprites?.other?.officialArtwork?.frontDefault
            ?: pokemon?.sprites?.other?.home?.frontDefault
                    ?: currentStage?.imageUrl

        pokemon?.name?.let { name ->
            viewModel.fetchEvolutionChain(name)
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(uiState.pokemon?.name) {
        listState.scrollToItem(0)
    }

    val gmaxPokemonColors = mapOf(
        "Charizard-gmax" to Color(0xFFDA4453),
        "Venusaur-gmax" to Color(0xFF88B04B),
        "Blastoise-gmax" to Color(0xFF2980B9),
        "Pikachu-gmax" to Color(0xFFFFD700),
        "Eevee-gmax" to Color(0xFFF5CBA7),
        "Meowth-gmax" to Color(0xFFFFE082),
        "Inteleon-gmax" to Color(0xFF00BFFF),
        "Cinderace-gmax" to Color(0xFFFF4500),
        "Rillaboom-gmax" to Color(0xFF2ECC71),
        "Gengar-gmax" to Color(0xFF6A1B9A),
        "Lapras-gmax" to Color(0xFF81D4FA),
        "Snorlax-gmax" to Color(0xFF4CAF50),
        "Machamp-gmax" to Color(0xFFD84315),
        "Butterfree-gmax" to Color(0xFFBA68C8),
        "Toxtricity-gmax" to Color(0xFF8E24AA),
    )

    val bgColor =
        gmaxPokemonColors[currentNameForBg] ?: getPokemonBackgroundColor(currentNameForBg, typeList)

    val spriteVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 200
        }
    }

    val pressed = remember { mutableStateOf(false) }
    val defaultColor = Color.Black.copy(alpha = 0.6f)
    val containerColor by animateColorAsState(
        targetValue = if (pressed.value) bgColor else defaultColor,
        animationSpec = tween(150),
        label = "chip color anim"
    )

    val tts = remember {
        lateinit var ttsInstance: TextToSpeech
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // now it's safe to call methods on the created instance
                val result = ttsInstance.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported or missing data: $result")
                    isTtsReady = false
                } else {
                    ttsInstance.setPitch(1.3f)
                    ttsInstance.setSpeechRate(0.9f)
                    isTtsReady = true
                }
            } else {
                Log.e("TTS", "Initialization failed: $status")
                isTtsReady = false
            }
        }
        ttsInstance
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                tts.stop()
                tts.shutdown()
            } catch (_: Exception) {
            }
            try {
                mediaPlayer.release()
            } catch (_: Exception) {
            }
        }
    }

    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            isSpeaking = true
        }

        override fun onDone(utteranceId: String?) {
            isSpeaking = false
        }

        override fun onError(utteranceId: String?) {
            isSpeaking = false
        }
    })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .align(Alignment.TopCenter)
                .zIndex(1f) // this container remains top area
        ) {

            // background radial glow
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.radialGradient(
                    colors = listOf(
                        bgColor.copy(alpha = 0.55f),
                        bgColor.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.maxDimension * 0.7f
                )
                drawCircle(
                    brush = gradientBrush,
                    radius = size.maxDimension * 0.7f,
                    center = center
                )
            }

            LayeredWaveformVisualizer(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(140.dp)
                    .zIndex(0f), // ensure behind
                color = bgColor,
                isSpeaking = isSpeaking
            )

            if (specialEffectsEnabled && spriteVisible && spriteEffectsEnabled) {
                val particleType = getParticleTypeFor(typeList)
                ParticleBackground(particleType, pokemon?.name.toString())
            }

            val pressScale = remember { Animatable(1f) }
            val context = LocalContext.current
            val imageLoader = ImageLoader.Builder(context)
                .components {
                    add(ImageDecoderDecoder.Factory())
                }
                .build()

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.Center)
                    .zIndex(2f)
            ) {
                AsyncImage(

                    model = ImageRequest.Builder(context)
                        .data(currentSpriteUrl)
                        .decoderFactory(SvgDecoder.Factory())
                        .crossfade(true)
                        .allowHardware(false)
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = pokemon?.name ?: currentStage?.name,
                    contentScale = ContentScale.Fit,
                    imageLoader = imageLoader,
                    onLoading = { showLoader = true },
                    onSuccess = { showLoader = false; isSpriteChanged = false },
                    onError = { showLoader = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (showLoader || !spriteVisible) 0f else 1f
                            scaleX = pressScale.value
                            scaleY = pressScale.value
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    pressScale.animateTo(1.1f, tween(150))
                                    spriteEffectsEnabledState.value = true
                                    tryAwaitRelease()
                                    pressScale.animateTo(1.0f, tween(150))
                                    spriteEffectsEnabledState.value = false
                                }
                            )
                        }
                )

                AnimatedVisibility(
                    visible = showLoader,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LottieAnimation(
                        composition = rememberLottieComposition(
                            LottieCompositionSpec.RawRes(R.raw.shine)
                        ).value,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        val displayName = pokemon?.name
                            ?.replace("-", " ")
                            ?.replaceFirstChar { it.uppercase() }
                            ?: ""

                        val fontSize = if (displayName.length > 15) 18.sp else 22.sp

                        Text(
                            text = displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize,
                            textAlign = TextAlign.Start,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        val team by viewModel.team.collectAsStateWithLifecycle()
                        val teamMembershipMap = remember(team) { team.associate { it.name to true } }
                        val isInTeam = teamMembershipMap[pokemon?.name] ?: false

                        IconButton(
                            onClick = {
                                // ordered sprite priority list
                                val orderedSprites = listOfNotNull(
                                    pokemon?.sprites?.other?.officialArtwork?.frontDefault,
                                    pokemon?.sprites?.other?.home?.frontDefault,
                                    pokemon?.sprites?.other?.dreamWorld?.frontDefault,
                                    pokemon?.sprites?.other?.showdown?.frontDefault,
                                    currentStage?.imageUrl
                                )

                                // find current index in that order
                                val currentIndex = orderedSprites.indexOf(currentSpriteUrl)
                                val nextIndex = if (currentIndex == -1 || currentIndex == orderedSprites.lastIndex) 0 else currentIndex + 1

                                val nextSprite = orderedSprites.getOrNull(nextIndex)
                                if (nextSprite != null && nextSprite != currentSpriteUrl) {
                                    currentSpriteUrl = nextSprite
                                    showLoader = true
                                    isSpriteChanged = true
                                }
                            },
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        Toast
                                            .makeText(context, "Tap to switch sprite in order ✨", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Switch Sprite",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                pokemon?.let {
                                    if (isInTeam) {
                                        // remove by name so we don't rely on the autogenerated id
                                        viewModel.removeFromTeamByName(it.name)
                                        Toast.makeText(context, "${it.name.replaceFirstChar { c -> c.uppercase() }} removed from team", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addToTeam(it.toEntity())
                                        Toast.makeText(context, "${it.name.replaceFirstChar { c -> c.uppercase() }} added to team", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isInTeam) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (isInTeam) "Remove from Team" else "Add to Team",
                                tint = if (isInTeam) Color(0xFFFFD700) else Color.White
                            )
                        }
                        IconButton(onClick = {

                            if (isTtsReady) {
                                val utteranceId = java.util.UUID.randomUUID().toString()
                                try {
                                    // try to play beep then speak
                                    mediaPlayer.setOnCompletionListener {
                                        tts.speak(
                                            speechText,
                                            TextToSpeech.QUEUE_FLUSH,
                                            null,
                                            utteranceId
                                        )
                                    }
                                    mediaPlayer.start()

                                    // fallback: if beep didn't start, speak immediately
                                    if (!mediaPlayer.isPlaying) {
                                        tts.speak(
                                            speechText,
                                            TextToSpeech.QUEUE_FLUSH,
                                            null,
                                            utteranceId
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("TTS", "beep/speak failed, speaking directly", e)
                                    tts.speak(
                                        speechText,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        utteranceId
                                    )
                                }
                            } else {
                                Toast.makeText(context, "TTS not ready", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak Description",
                                tint = Color.White
                            )
                        }

                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                        .zIndex(10f)
                )
            }
        ) { padding ->

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {

                        LoadingIndicator(
                            modifier = Modifier.size(95.dp),
                            color = bgColor
                        )
                    }
                }

                pokemon != null -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        item {

                            Spacer(modifier = Modifier.height(250.dp))
                        }

                        // Basic Info
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "ID: #${pokemon.id.toString().padStart(4, '0')}",
                                        color = Color.White
                                    )
                                    Text("Height: ${pokemon.height / 10.0} m", color = Color.White)
                                    Text("Weight: ${pokemon.weight / 10.0} kg", color = Color.White)
                                }
                            }
                        }

                        // Types
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Types", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                        pokemon.types.forEach {

                                            AssistChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        it.type.name.uppercase(),
                                                        color = Color.White
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = containerColor,
                                                    labelColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onPress = {
                                                                pressed.value = true
                                                                try {
                                                                    // Wait for the press to finish
                                                                    awaitRelease()
                                                                } finally {
                                                                    pressed.value = false
                                                                }
                                                            }
                                                        )
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (descriptionText.isNotBlank()) {
                            item {
                                GlossyCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Description",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = descriptionText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 20.sp,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }

                        // stats
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Base Stats",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    pokemon.stats.forEachIndexed { index, stat ->
                                        val animatedProgress = animateFloatAsState(
                                            targetValue = stat.base_stat / 255f,
                                            animationSpec = tween(
                                                durationMillis = 2500,
                                                delayMillis = index * 120,
                                                easing = FastOutSlowInEasing
                                            ), label = "statAnimation"
                                        )

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = stat.stat.name.replace("-", " ")
                                                        .replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = stat.base_stat.toString(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.width(40.dp),
                                                    textAlign = TextAlign.End
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(10.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color.White.copy(alpha = 0.1f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(animatedProgress.value)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(50))
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(bgColor.copy(alpha = 0.7f), bgColor)
                                                            )
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            val evoList by viewModel.evolutionList.collectAsStateWithLifecycle()

                            if (evoList.isNotEmpty()) {
                                GlossyCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Evolution Chain",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            evoList.forEach { stage ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.animateItem()
                                                ) {
                                                    AsyncImage(
                                                        model = stage.imageUrl,
                                                        contentDescription = stage.name,
                                                        modifier = Modifier
                                                            .size(90.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.05f))
                                                            .border(
                                                                width = 1.dp,
                                                                color = Color.White.copy(alpha = 0.2f),
                                                                shape = CircleShape
                                                            )
                                                            .clickable { /* TODO: handle click (like show details) */ },
                                                        contentScale = ContentScale.Fit
                                                    )

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Text(
                                                        text = stage.name.replaceFirstChar { it.uppercase() },
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                        color = Color.White.copy(alpha = 0.9f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Mega Evolutions / Other Forms
                        if (uiState.varieties.isNotEmpty()) {
                            item {
                                GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Other Forms",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(uiState.varieties) { variety ->
                                                ElevatedAssistChip(
                                                    onClick = {
                                                        val formName = variety.pokemon.name
                                                        val currentName = uiState.pokemon?.name
                                                        if (!formName.equals(currentName, ignoreCase = true)) {
                                                            viewModel.fetchVarietyPokemon(formName)
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            text = variety.pokemon.name.replace(
                                                                "-",
                                                                " "
                                                            )
                                                                .replaceFirstChar { it.uppercase() },
                                                            color = Color.White
                                                        )
                                                    },
                                                    colors = AssistChipDefaults.assistChipColors(
                                                        containerColor = bgColor.copy(alpha = 0.3f),
                                                        labelColor = Color.White
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Failed to load data.", color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    navController.navigate("detailscreen/${pokemon}")
                                }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}