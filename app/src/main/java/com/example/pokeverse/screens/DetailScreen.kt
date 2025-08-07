package com.example.pokeverse.screens

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pokeverse.R
import com.example.pokeverse.ui.viewmodel.PokemonViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(pokemonName: String, navController: NavController) {

    val viewModel: PokemonViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val pokemon = uiState.pokemon

    LaunchedEffect(pokemonName) {
        viewModel.fetchPokemonData(pokemonName)
        viewModel.fetchVarietyPokemon(pokemonName)
    }

    val description = pokemon?.id?.let { viewModel.getLocalDescription(it) } ?: ""

    val isLoading = uiState.isLoading
    val cleanText = description
        .replace(Regex("[^\\x00-\\x7F]"), " ")
        .replace("\n", " ")
        .replace("\u000c", " ")
        .trim()

    val gmaxPokemonColors = mapOf(
        "Charizard-gmax" to Color(0xFFDA4453),   // Fiery red-orange with magma vibes
        "Venusaur-gmax" to Color(0xFF88B04B),    // Lush green from the massive flower
        "Blastoise-gmax" to Color(0xFF2980B9),   // Deep blue for the heavy water cannons
        "Pikachu-gmax" to Color(0xFFFFD700),     // Electric gold for retro fat Pikachu
        "Eevee-gmax" to Color(0xFFF5CBA7),       // Soft beige for fluffy Gmax Eevee
        "Meowth-gmax" to Color(0xFFFFE082),      // Pale gold from long coin body
        "Inteleon-gmax" to Color(0xFF00BFFF),    // Ice blue sniper tower
        "Cinderace-gmax" to Color(0xFFFF4500),   // Fiery soccer aura
        "Rillaboom-gmax" to Color(0xFF2ECC71),   // Bright jungle green drum
        "Gengar-gmax" to Color(0xFF6A1B9A),       // Haunting purple
        "Lapras-gmax" to Color(0xFF81D4FA),       // Musical icy glow
        "Snorlax-gmax" to Color(0xFF4CAF50),      // Nature green forest belly
        "Machamp-gmax" to Color(0xFFD84315),      // Bold orange, power stance
        "Butterfree-gmax" to Color(0xFFBA68C8),   // Magical violet aura
        "Toxtricity-gmax" to Color(0xFF8E24AA),   // Electric neon purple-pink
    )

    val typeList = pokemon?.types?.map { it.type.name } ?: emptyList()
    val formKey = "$gmaxPokemonColors"

    val bgColor = if(formKey.isNotEmpty()) gmaxPokemonColors[formKey] ?: getPokemonBackgroundColor(pokemonName, typeList) else getPokemonBackgroundColor(pokemonName, typeList)

    val context = LocalContext.current
    var isTtsReady by remember { mutableStateOf(false) }
    val mediaPlayer = MediaPlayer.create(context, R.raw.beepeffect)

    val tts = remember {
        TextToSpeech(context) { status ->
            isTtsReady = status == TextToSpeech.SUCCESS
        }.apply {
            language = Locale.US
            setPitch(1.3f)       // Higher pitch = softer and more cheerful
            setSpeechRate(0.9f)  // Slightly slower = clearer and more expressive
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Dark background
    ) {

        // Gradient behind Pokémon image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(top = 75.dp)
                .align(Alignment.TopCenter)
                .zIndex(-1f)
        ) {

            AnimatedEmberBackground(
                types = typeList
            )

            // Actual radial background
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

            // Pokémon Image animation
            val imageAlpha = remember { Animatable(0f) }
            val imageScale = remember { Animatable(0.9f) }
            val isPressed = remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                imageAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
                imageScale.animateTo(1.0f, tween(600, easing = FastOutSlowInEasing))
            }

            val pressScale by animateFloatAsState(
                targetValue = if (isPressed.value) 1.1f else imageScale.value,
                animationSpec = tween(300),
                label = "pressScale"
            )

            AsyncImage(
                model = pokemon?.sprites?.other?.officialArtwork?.frontDefault
                    ?: pokemon?.sprites?.front_default,
                contentDescription = pokemon?.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        alpha = imageAlpha.value
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .align(Alignment.Center)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed.value = true
                                tryAwaitRelease()
                                isPressed.value = false
                            }
                        )
                    }
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = pokemon?.name?.replaceFirstChar { it.uppercase() } ?: "Details",
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
                            val speechText = buildString {
                                append("$name. ")
                                append("A $type type Pokémon. ")
                                append(cleanText)
                            }

                            if (isTtsReady) {
                                tts.language = Locale.US
                                tts.setPitch(0.8f)
                                tts.setSpeechRate(0.85f)
                                mediaPlayer.start()
                                mediaPlayer.setOnCompletionListener {
                                    tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            } else {
                                Toast.makeText(context, "Voice engine not ready", Toast.LENGTH_SHORT).show()
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
                        containerColor = Color.Black,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            val pokeballGradient = Brush.verticalGradient(
                listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A))
            )
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize().background(brush = pokeballGradient), contentAlignment = Alignment.Center) {
                        CustomProgressIndicator()
                    }
                }

                pokemon != null -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(260.dp))
                        }

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

                                    // This must be a Composable block!
                                    pokemon.stats.forEachIndexed { index, stat ->
                                        val animatedProgress = animateFloatAsState(
                                            targetValue = stat.base_stat / 255f,
                                            animationSpec = tween(
                                                durationMillis = 1000,
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
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier.width(40.dp)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(12.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color.Gray.copy(alpha = 0.2f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(animatedProgress.value)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(50))
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
                                                        navController.navigate("pokemon_detail/$formName")
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
                    // Retry UI
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
                                    viewModel.fetchPokemonData(pokemonName.lowercase())
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

@Composable
fun GlossyCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .graphicsLayer {
                alpha = 0.9f
                shadowElevation = 4f
                shape = RoundedCornerShape(20.dp)
                clip = true
            }
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.Black.copy(alpha = 0.5f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        content = content
    )
}

fun getPokemonBackgroundColor(name: String, types: List<String>): Color {
    val pokemonVariantColors = mapOf(
        "charizard-mega-x" to Color(0xFF1e3c72),  // Custom blue flame color
        "charizard-mega-y" to Color(0xFFE08503),  // Red like fire type
        // Mega Mewtwo
        "mewtwo-mega-x" to Color(0xFF4F1026),   // Amethyst purple for Mega Mewtwo X
        "mewtwo-mega-y" to Color(0xFF4C1156),   // Deep sky blue for Mega Mewtwo Y

        // Mega Rayquaza
        "rayquaza-mega"   to Color(0xFF19501C),   // Light Sea Green for a fresh look

        // Mega Gengar
        "gengar-mega"     to Color(0xFF800080),   // Classic purple for Mega Gengar

        // Mega Venusaur
        "venusaur-mega"   to Color(0xFF2E8B57),   // Sea Green gives a grounded, earthy tone

        // Mega Blastoise
        "blastoise-mega"  to Color(0xFF008DF1),    // Steel Blue for a cool, resolute vibe

        "fire" to Color(0xFFEC5343),
        "water" to Color(0xFF30A2BE),
        "grass" to Color(0xFF56ab2f),
        "electric" to Color(0xFFFFE000),
        "psychic" to Color(0xFFFC5C7D),
        "ice" to Color(0xFF83a4d4),
        "dragon" to Color(0xFF1e3c72),
        "dark" to Color(0xFF434343),
        "fairy" to Color(0xFFF78CA0),
        "fighting" to Color(0xFFC31432),
        "rock" to Color(0xFF3C3B3F),
        "ground" to Color(0xFFD1913C),
        "bug" to Color(0xFFa8e063),
        "ghost" to Color(0xFF606c88),
        "steel" to Color(0xFFbdc3c7),
        "poison" to Color(0xFF9D50BB),
        "normal" to Color(0xFFC4C3A5),
        "flying" to Color(0xFF89F7FE)
    )

    // Try variant-specific color first
    val formColor = pokemonVariantColors[name.lowercase()]
    if (formColor != null) return formColor

    // Fallback to type-based color
    for (type in types) {
        val typeColor = pokemonVariantColors[type.lowercase()]
        if (typeColor != null) return typeColor
    }

    // Final fallback
    return Color.LightGray
}


@Preview(showSystemUi = true)
@Composable
private fun DetailScreen() {
    PokemonDetailScreen(pokemonName = "Pikachu", navController = NavController(LocalContext.current))
}