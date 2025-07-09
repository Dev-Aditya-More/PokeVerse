package com.example.pokeverse.screens

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.pokeverse.R
import com.example.pokeverse.ui.viewmodel.PokemonViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(pokemonName: String, navController: NavController) {

    val typeGradientMap: Map<String, List<Color>> = mapOf(
        "fire" to listOf(Color(0xFFEC5343), Color(0xFFE17050)),
        "water" to listOf(Color(0xFF30A2BE), Color(0xFF61A2B6)),
        "grass" to listOf(Color(0xFF56ab2f), Color(0xFFA8E063)),
        "electric" to listOf(Color(0xFFFFE000), Color(0xFFFFA500)),
        "psychic" to listOf(Color(0xFFFC5C7D), Color(0xFF6A82FB)),
        "ice" to listOf(Color(0xFF83a4d4), Color(0xFFb6fbff)),
        "dragon" to listOf(Color(0xFF1e3c72), Color(0xFF2a5298)),
        "dark" to listOf(Color(0xFF434343), Color(0xFF000000)),
        "fairy" to listOf(Color(0xFFF78CA0), Color(0xFFF9748F)),
        "fighting" to listOf(Color(0xFFC31432), Color(0xFF240B36)),
        "rock" to listOf(Color(0xFF3C3B3F), Color(0xFF605C3C)),
        "ground" to listOf(Color(0xFFD1913C), Color(0xFFFFECD2)),
        "bug" to listOf(Color(0xFFa8e063), Color(0xFF56ab2f)),
        "ghost" to listOf(Color(0xFF606c88), Color(0xFF3f4c6b)),
        "steel" to listOf(Color(0xFFbdc3c7), Color(0xFF2c3e50)),
        "poison" to listOf(Color(0xFF9D50BB), Color(0xFF6E48AA)),
        "normal" to listOf(Color(0xFFEDE574), Color(0xFFE1F5C4)),
        "flying" to listOf(Color(0xFF89F7FE), Color(0xFF66A6FF))
    )

    val viewModel: PokemonViewModel = koinViewModel()

    val uiState by viewModel.uiState.collectAsState()

    val pokemon = uiState.pokemon
    val description = uiState.description
    val isLoading = uiState.isLoading
    val cleanText = description
        .replace(Regex("[^\\x00-\\x7F]"), " ")  // remove non-ASCII
        .replace("\n", " ")
        .replace("\u000c", " ")
        .trim()

    val primaryType = pokemon?.types?.firstOrNull()?.type?.name ?: "normal"
    val gradientColors = typeGradientMap[primaryType] ?: listOf(Color.Gray, Color.LightGray)
    
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
    LaunchedEffect(Unit) {
        viewModel.fetchPokemonData(pokemonName.lowercase())
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = gradientColors)
        )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = pokemon?.name?.replaceFirstChar { it.uppercase() } ?: "Details",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
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
                                tts.setPitch(0.8f)          // Deeper voice
                                tts.setSpeechRate(0.85f)    // Slightly slower

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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CustomProgressIndicator()
                }
            } else {
                pokemon?.let { data ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // Pokémon Image (larger & better quality if available)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = data.sprites.other?.officialArtwork?.frontDefault ?: data.sprites.front_default
                                    ),
                                    contentDescription = data.name,
                                    modifier = Modifier.size(220.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }


                        // Basic Info
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("ID: ${data.id}")
                                    Text("Height: ${data.height}")
                                    Text("Weight: ${data.weight}")
                                }
                            }
                        }

                        // Types Section
                        item {
                            GlossyCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Types", fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        data.types.forEach {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(it.type.name.uppercase()) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Stats Section
                        item {
                            GlossyCard(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Base Stats", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    data.stats.forEach { stat ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(stat.stat.name.replace("-", " "), fontWeight = FontWeight.Medium)
                                            Text(stat.base_stat.toString(), fontWeight = FontWeight.Light)
                                        }
                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Failed to load data. ")
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
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.1f)
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