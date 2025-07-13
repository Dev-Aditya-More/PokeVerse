package com.example.pokeverse.screens

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    val description = uiState.description
    val isLoading = uiState.isLoading
    val cleanText = description
        .replace(Regex("[^\\x00-\\x7F]"), " ")
        .replace("\n", " ")
        .replace("\u000c", " ")
        .trim()

    val primaryType = pokemon?.types?.firstOrNull()?.type?.name ?: "normal"

    val context = LocalContext.current
    var isTtsReady by remember { mutableStateOf(false) }
    val mediaPlayer = MediaPlayer.create(context, R.raw.beepeffect)

    val tts = remember {
        TextToSpeech(context) { status ->
            isTtsReady = status == TextToSpeech.SUCCESS
        }
    }.apply {
        language = Locale.US
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    LaunchedEffect(pokemonName) {
        viewModel.fetchPokemonData(pokemonName.lowercase())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Dark background
    ) {
        val pokemonTypeColors = mapOf(
            "fire" to Color(0xFFEC5343),       // Red from Fire gradient
            "water" to Color(0xFF30A2BE),      // Blue from Water gradient
            "grass" to Color(0xFF56ab2f),      // Green from Grass gradient
            "electric" to Color(0xFFFFE000),   // Yellow from Electric gradient
            "psychic" to Color(0xFFFC5C7D),    // Pink from Psychic gradient
            "ice" to Color(0xFF83a4d4),        // Light blue from Ice gradient
            "dragon" to Color(0xFF1e3c72),     // Dark blue from Dragon gradient
            "dark" to Color(0xFF434343),       // Gray from Dark gradient
            "fairy" to Color(0xFFF78CA0),      // Light pink from Fairy gradient
            "fighting" to Color(0xFFC31432),   // Red from Fighting gradient
            "rock" to Color(0xFF3C3B3F),       // Dark gray from Rock gradient
            "ground" to Color(0xFFD1913C),     // Orange from Ground gradient
            "bug" to Color(0xFFa8e063),        // Light green from Bug gradient
            "ghost" to Color(0xFF606c88),      // Purple-gray from Ghost gradient
            "steel" to Color(0xFFbdc3c7),      // Light gray from Steel gradient
            "poison" to Color(0xFF9D50BB),     // Purple from Poison gradient
            "normal" to Color(0xFFC4C3A5),     // Yellow from Normal gradient
            "flying" to Color(0xFF89F7FE)      // Light blue from Flying gradient
        )
        val typeColor = pokemonTypeColors[primaryType] ?: Color.Gray

        // Gradient behind Pokémon image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(top = 75.dp)
                .align(Alignment.TopCenter)
                .zIndex(-1f)
        ) {
            // Actual radial background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.radialGradient(
                    colors = listOf(
                        typeColor.copy(alpha = 0.6f),  // Core glow
                        typeColor.copy(alpha = 0.15f),  // Fade out
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

            LaunchedEffect(Unit) {
                imageAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
                imageScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            }

            AsyncImage(
                model = pokemon?.sprites?.other?.officialArtwork?.frontDefault
                    ?: pokemon?.sprites?.front_default,
                contentDescription = pokemon?.name,
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        alpha = imageAlpha.value
                        scaleX = imageScale.value
                        scaleY = imageScale.value
                    }
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
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
                                imageVector = Icons.Filled.Info,
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

                        // Stats
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

                                    pokemon.stats.forEach { stat ->
                                        val progress = stat.base_stat / 255f

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stat.stat.name.replace("-", " ").replaceFirstChar { it.uppercase() },
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
                                                        .fillMaxWidth(progress)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(50))
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(
                                                                    typeColor.copy(alpha = 0.7f),
                                                                    typeColor
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

                        // Description
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("About", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(
                                        text = cleanText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
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

@Preview(showSystemUi = true)
@Composable
private fun DetailScreen() {
    PokemonDetailScreen(pokemonName = "Pikachu", navController = NavController(LocalContext.current))
}