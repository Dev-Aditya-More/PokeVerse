package com.aditya1875.pokeverse.screens

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.components.CustomProgressIndicator
import com.aditya1875.pokeverse.components.EvolutionConnector
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionStage
import com.aditya1875.pokeverse.specialscreens.ParticleBackground
import com.aditya1875.pokeverse.specialscreens.getParticleTypeFor
import com.aditya1875.pokeverse.ui.viewmodel.PokemonDetailUiState
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailPage(
    uiState: PokemonDetailUiState,
    currentStage: EvolutionStage?,
    showLeftConnector: Boolean,
    showRightConnector: Boolean,
    onConnectorClick: (Int) -> Unit,
    navController: NavController,
    specialEffectsEnabled: Boolean,
    spriteEffectsEnabledState: MutableState<Boolean>,
    viewModel: PokemonViewModel = koinViewModel()
) {
    val pokemon = uiState.pokemon
    val spriteEffectsEnabled = spriteEffectsEnabledState.value
    val typeList = pokemon?.types?.map { it.type.name } ?: emptyList()
    val currentNameForBg = currentStage?.name ?: pokemon?.name ?: ""
    // gmax color map (kept as you had it)
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
    val formKey = "$gmaxPokemonColors"
    val bgColor = if (formKey.isNotEmpty())
        gmaxPokemonColors[formKey] ?: getPokemonBackgroundColor(currentNameForBg, typeList)
    else getPokemonBackgroundColor(currentNameForBg, typeList)

    val context = LocalContext.current
    var isTtsReady by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.beepeffect) }
    val listState = rememberLazyListState()
    val spriteVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset < 200
        }
    }
    LaunchedEffect(currentStage, showLeftConnector, showRightConnector) {
        Log.d("DBG_EVO", "currentStage=${currentStage?.name} showLeft=$showLeftConnector showRight=$showRightConnector")
    }

    // TTS
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
            } catch (_: Exception) {}
            try { mediaPlayer.release() } catch (_: Exception) {}
        }
    }

    // use the stage image immediately if uiState.pokemon hasn't loaded yet
    val spriteUrlFallback = currentStage?.imageUrl

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Dark background
    ) {

        // Gradient behind Pokémon image + sprite + connectors
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(top = 75.dp)
                .align(Alignment.TopCenter)
                .zIndex(1f)
        ) {

            if (specialEffectsEnabled && spriteEffectsEnabled) {
                val particleType = getParticleTypeFor(typeList)
                ParticleBackground(particleType, pokemon.toString())
            }

            // Actual radial background (kept unchanged)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.radialGradient(
                    colors = listOf(
                        bgColor.copy(alpha = 0.6f),  // Core glow
                        bgColor.copy(alpha = 0.15f),  // Fade out
                        Color.Transparent              // Smooth outer edge
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

            // Left connector (glowing) — clickable: navigate to prev stage by calling onConnectorClick
            if (showLeftConnector && currentStage?.prevId != null) {
                com.aditya1875.pokeverse.components.EvolutionConnector(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(width = 120.dp, height = 40.dp)
                        .zIndex(10f),
                    direction = EvolutionConnector.Direction.LEFT
                ) {
                    // the ViewModel already holds stages list; find prev stage by id/name and call onConnectorClick
                    // We pass the stage through onConnectorClick; parent pager will navigate.
                    onConnectorClick(currentStage.prevId)
                }
            }

            // Pokémon Image animation (sprite uses uiState.pokemon if available else fallback)
            val imageAlpha = remember { Animatable(0f) }
            val imageScale = remember { Animatable(0.9f) }

            LaunchedEffect(currentStage?.imageUrl, uiState.pokemon?.id) {
                imageAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
                imageScale.animateTo(1.0f, tween(600, easing = FastOutSlowInEasing))
            }

            val pressScale = remember { Animatable(1.0f) }

            AsyncImage(
                model = uiState.pokemon?.sprites?.other?.officialArtwork?.frontDefault
                    ?: uiState.pokemon?.sprites?.front_default
                    ?: spriteUrlFallback,
                contentDescription = uiState.pokemon?.name ?: currentStage?.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        alpha = if (spriteVisible) 1f else 0f
                        translationY = if (spriteVisible) 0f else -1000f
                        scaleX = pressScale.value
                        scaleY = pressScale.value
                    }
                    .align(Alignment.Center)
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

            // Right connector
            if (showRightConnector && currentStage?.nextId != null) {
                EvolutionConnector(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(width = 120.dp, height = 40.dp)
                        .zIndex(10f),
                    direction = EvolutionConnector.Direction.RIGHT
                ) {
                    onConnectorClick(currentStage.nextId)
                }
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = pokemon?.name?.replaceFirstChar { it.uppercase() } ?: "",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val name = pokemon?.name?.replaceFirstChar { it.uppercase() } ?: "This Pokémon"
                            val type = pokemon?.types?.firstOrNull()?.type?.name ?: "unknown type"
                            val descriptionText = pokemon?.id?.let { viewModel.getLocalDescription(it) } ?: ""
                            val cleanText = descriptionText.replace(Regex("[^\\x00-\\x7F]"), " ").replace("\n", " ").trim()
                            val speechText = "$name. A $type type Pokémon. $cleanText"

                            if (isTtsReady) {
                                val utteranceId = java.util.UUID.randomUUID().toString()
                                try {
                                    // try to play beep then speak
                                    mediaPlayer.setOnCompletionListener {
                                        tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                                    }
                                    mediaPlayer.start()

                                    // fallback: if beep didn't start, speak immediately
                                    if (!mediaPlayer.isPlaying) {
                                        tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                                    }
                                } catch (e: Exception) {
                                    Log.e("TTS", "beep/speak failed, speaking directly", e)
                                    tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                                }
                            } else {
                                Toast.makeText(context, "TTS not ready", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak Description", tint = Color.White)
                        }

                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            // I preserved your gradient then the exact when block and LazyColumn content.
            Brush.verticalGradient(listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A))) // no-op here to keep parity

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CustomProgressIndicator()
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
                        item { Spacer(modifier = Modifier.height(260.dp)) }

                        // Basic Info
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("ID: #${pokemon.id.toString().padStart(4, '0')}", color = Color.White)
                                    Text("Height: ${pokemon.height / 10.0} m", color = Color.White)
                                    Text("Weight: ${pokemon.weight / 10.0} kg", color = Color.White)
                                }
                            }
                        }

                        // Types
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Types", fontWeight = FontWeight.Bold, color = Color.White)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        pokemon.types.forEach {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(it.type.name.uppercase(), color = Color.White) },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = Color.Black.copy(alpha = 0.6f),
                                                    labelColor = Color.White
                                                )
                                            )
                                        }
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
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    pokemon.stats.forEachIndexed { index, stat ->
                                        val animatedProgress = animateFloatAsState(
                                            targetValue = stat.base_stat / 255f,
                                            animationSpec = tween(
                                                durationMillis = 3000,
                                                delayMillis = index * 100,
                                                easing = FastOutSlowInEasing
                                            ), label = "statAnimation"
                                        )

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stat.stat.name.replace("-", " ")
                                                        .replaceFirstChar { it.uppercase() },
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = stat.base_stat.toString(),
                                                    color = Color.White,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                    modifier = Modifier.width(40.dp)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(12.dp)
                                                    .clip(MaterialTheme.shapes.small.copy(all = androidx.compose.foundation.shape.CornerSize(50.dp)))
                                                    .background(Color.Gray.copy(alpha = 0.2f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(animatedProgress.value)
                                                        .fillMaxHeight()
                                                        .clip(MaterialTheme.shapes.small.copy(all = androidx.compose.foundation.shape.CornerSize(50.dp)))
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(
                                                                    bgColor.copy(alpha = 0.7f),
                                                                    bgColor
                                                                )
                                                            )
                                                        )
                                                )
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
                                                        viewModel.fetchVarietyPokemon(formName)
                                                    },
                                                    label = {
                                                        Text(
                                                            text = variety.pokemon.name.replace("-", " ")
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