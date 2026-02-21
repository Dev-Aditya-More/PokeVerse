package com.aditya1875.pokeverse.presentation.screens.detail.components

import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.data.remote.model.PokemonResult
import com.aditya1875.pokeverse.presentation.screens.detail.GlossyCard
import com.aditya1875.pokeverse.presentation.screens.detail.getPokemonBackgroundColor
import com.aditya1875.pokeverse.presentation.screens.home.components.AddToTeamBottomSheet
import com.aditya1875.pokeverse.presentation.screens.team.components.CreateTeamDialog
import com.aditya1875.pokeverse.presentation.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.utils.TeamMapper.toEntity
import com.aditya1875.pokeverse.utils.UiError
import org.koin.androidx.compose.koinViewModel
import kotlin.collections.get

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun PokemonDetailPage(
    navController: NavController,
    specialEffectsEnabled: Boolean,
    spriteEffectsEnabledState: MutableState<Boolean>,
    viewModel: PokemonViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pokemon = uiState.pokemon
    val spriteEffectsEnabled = spriteEffectsEnabledState.value
    val typeList = pokemon?.types?.map { it.type.name } ?: emptyList()
    val currentNameForBg = pokemon?.name ?: ""
    val context = LocalContext.current

    val ttsManager = remember { context.getTTSManager() }
    val isTtsReady by ttsManager.isReady.collectAsStateWithLifecycle()
    val isSpeaking by ttsManager.isSpeaking.collectAsStateWithLifecycle()

    val mediaPlayer = remember { MediaPlayer() }
    var isPlayingCry by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }

    fun playCry() {
        pokemon?.let {
            val cryUrl = it.cries?.latest ?: it.cries?.legacy

            if (cryUrl != null) {
                try {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                    }
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(cryUrl)
                    mediaPlayer.prepareAsync()
                    mediaPlayer.setOnPreparedListener { mp ->
                        mp.start()
                        isPlayingCry = true
                    }
                    mediaPlayer.setOnCompletionListener {
                        isPlayingCry = false
                    }
                    mediaPlayer.setOnErrorListener { _, what, extra ->
                        Toast.makeText(context, "Could not play cry", Toast.LENGTH_SHORT).show()
                        isPlayingCry = false
                        true
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Cry not available", Toast.LENGTH_SHORT).show()
                    Log.e("PokemonDetail", "Error playing cry", e)
                }
            } else {
                Toast.makeText(context, "Cry not available for this Pokémon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stopCry() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            isPlayingCry = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "speaking")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    var showLoader by remember { mutableStateOf(false) }
    val name = pokemon?.name?.replaceFirstChar { it.uppercase() } ?: "This Pokémon"
    val type = pokemon?.types?.firstOrNull()?.type?.name ?: "unknown type"
    val descriptionText = pokemon?.id?.let { viewModel.getLocalDescription(it) } ?: ""
    val cleanText = descriptionText.replace(Regex("[^\\x00-\\x7F]"), " ")
        .replace("\n", " ")
        .trim()
    val speechText = "$name. A $type type Pokémon. $cleanText"

    var isSpriteChanged by rememberSaveable { mutableStateOf(false) }
    val evolutionUi = uiState.evolutionUi
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

    val bgColor = gmaxPokemonColors[currentNameForBg]
        ?: getPokemonBackgroundColor(currentNameForBg, typeList)

    val spriteVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 200
        }
    }

    var isShinyEnabled by rememberSaveable { mutableStateOf(false) }
    var currentSpriteSource by rememberSaveable { mutableStateOf("official-artwork") }
    var currentSpriteUrl by rememberSaveable {
        mutableStateOf(
            pokemon?.sprites?.other?.officialArtwork?.frontDefault
                ?: pokemon?.sprites?.other?.home?.frontDefault
        )
    }

    fun getSpriteUrl(source: String, shiny: Boolean): String? {
        return when (source) {
            "official-artwork" -> {
                if (shiny) pokemon?.sprites?.other?.officialArtwork?.frontShiny
                else pokemon?.sprites?.other?.officialArtwork?.frontDefault
            }
            "home" -> {
                if (shiny) pokemon?.sprites?.other?.home?.frontShiny
                else pokemon?.sprites?.other?.home?.frontDefault
            }
            "dream-world" -> {
                if (shiny) pokemon?.sprites?.other?.dreamWorld?.frontShiny
                else pokemon?.sprites?.other?.dreamWorld?.frontDefault
            }
            "showdown" -> {
                if (shiny) pokemon?.sprites?.other?.showdown?.frontShiny
                else pokemon?.sprites?.other?.showdown?.frontDefault
            }
            else -> pokemon?.sprites?.other?.officialArtwork?.frontDefault
        }
    }

    LaunchedEffect(pokemon) {
        currentSpriteUrl = pokemon?.sprites?.other?.officialArtwork?.frontDefault
            ?: pokemon?.sprites?.other?.home?.frontDefault
        currentSpriteSource = when {
            pokemon?.sprites?.other?.officialArtwork?.frontDefault != null -> "official-artwork"
            pokemon?.sprites?.other?.home?.frontDefault != null -> "home"
            else -> "official-artwork"
        }
        isShinyEnabled = false
    }

    LaunchedEffect(isShinyEnabled) {
        val newUrl = getSpriteUrl(currentSpriteSource, isShinyEnabled)
        if (newUrl != null && newUrl != currentSpriteUrl) {
            currentSpriteUrl = newUrl
            showLoader = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
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
                            stopCry()
                            if (mediaPlayer.isPlaying) {
                                mediaPlayer.stop()
                            }
                            navController.popBackStack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val orderedSprites = listOf(
                                    "official-artwork" to (pokemon?.sprites?.other?.officialArtwork?.frontDefault
                                        ?: pokemon?.sprites?.other?.officialArtwork?.frontShiny),
                                    "home" to (pokemon?.sprites?.other?.home?.frontDefault
                                        ?: pokemon?.sprites?.other?.home?.frontShiny),
                                    "dream-world" to (pokemon?.sprites?.other?.dreamWorld?.frontDefault
                                        ?: pokemon?.sprites?.other?.dreamWorld?.frontShiny),
                                    "showdown" to (pokemon?.sprites?.other?.showdown?.frontDefault
                                        ?: pokemon?.sprites?.other?.showdown?.frontShiny),
                                ).filter { it.second != null }

                                val currentIndex = orderedSprites.indexOfFirst { it.first == currentSpriteSource }
                                val nextIndex = if (currentIndex == -1 || currentIndex == orderedSprites.lastIndex) 0 else currentIndex + 1

                                val nextSource = orderedSprites.getOrNull(nextIndex)
                                if (nextSource != null) {
                                    currentSpriteSource = nextSource.first
                                    val newUrl = getSpriteUrl(nextSource.first, isShinyEnabled)
                                    if (newUrl != null && newUrl != currentSpriteUrl) {
                                        currentSpriteUrl = newUrl
                                        showLoader = true
                                        isSpriteChanged = true
                                    }
                                }
                            },
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        Toast.makeText(context, "Tap to switch sprite style ✨", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Switch Sprite Style",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        var showAudioMenu by remember { mutableStateOf(false) }

                        Box {
                            IconButton(
                                onClick = { showAudioMenu = true },
                                modifier = Modifier.graphicsLayer {
                                    alpha = if (isSpeaking || isPlayingCry) pulseAlpha else 1f
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking || isPlayingCry) {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    },
                                    contentDescription = "Audio options",
                                    tint = if (isSpeaking || isPlayingCry) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }

                            DropdownMenu(
                                expanded = showAudioMenu,
                                onDismissRequest = { showAudioMenu = false }
                            ) {
                                // Pokédex entry TTS
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                tint = if (isSpeaking)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                if (isSpeaking) "Stop Pokédex Entry"
                                                else "Pokédex Entry",
                                                color = if (isSpeaking)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        showAudioMenu = false
                                        if (isTtsReady) {
                                            if (isSpeaking) {
                                                ttsManager.stop()
                                            } else {
                                                stopCry()
                                                ttsManager.speak(speechText, withBeep = true)
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Initializing...",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )

                                HorizontalDivider()

                                // Pokémon cry
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RecordVoiceOver,
                                                contentDescription = null,
                                                tint = if (isPlayingCry)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                if (isPlayingCry) "Stop Cry"
                                                else "Pokémon Cry",
                                                color = if (isPlayingCry)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        showAudioMenu = false
                                        if (isPlayingCry) {
                                            stopCry()
                                        } else {
                                            ttsManager.stop()
                                            playCry()
                                        }
                                    }
                                )
                            }
                        }

                        val isInFavorites by viewModel.isInFavorites(pokemon?.name ?: "")
                            .collectAsStateWithLifecycle(initialValue = false)

                        val allTeamsWithMembers by viewModel.allTeamsWithMembers.collectAsStateWithLifecycle()
                        val teamsContainingPokemon by viewModel.getTeamsForPokemon(pokemon?.name ?: "")
                            .collectAsStateWithLifecycle(initialValue = emptyList())

                        var showTeamBottomSheet by remember { mutableStateOf(false) }
                        var showCreateTeamDialog by remember { mutableStateOf(false) }
                        var teamCreationError by remember { mutableStateOf<String?>(null) }

                        PokemonActionsMenu(
                            pokemon = pokemon,
                            teamsContainingPokemon = teamsContainingPokemon,
                            allTeamsWithMembers = allTeamsWithMembers,
                            isInFavorites = isInFavorites,
                            onManageTeams = {
                                showTeamBottomSheet = true
                            },
                            onAddToFavorites = {
                                pokemon?.let { pokemonData ->
                                    viewModel.addToFavorites(
                                        PokemonResult(
                                            name = pokemonData.name,
                                            url = "https://pokeapi.co/api/v2/pokemon/${pokemonData.id}/"
                                        )
                                    )
                                    Toast.makeText(context, "${pokemonData.name.replaceFirstChar { c -> c.uppercase() }} added to favorites ⭐", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRemoveFromFavorites = {
                                pokemon?.let {
                                    viewModel.removeFromFavoritesByName(it.name)
                                    Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        if (showTeamBottomSheet) {
                            AddToTeamBottomSheet(
                                pokemonName = pokemon?.name ?: "",
                                allTeamsWithMembers = allTeamsWithMembers,
                                onDismiss = { showTeamBottomSheet = false },
                                onTeamSelected = { teamId ->
                                    pokemon?.let { poke ->
                                        viewModel.togglePokemonInTeam(
                                            pokemonResult = PokemonResult(
                                                name = poke.name,
                                                url = "https://pokeapi.co/api/v2/pokemon/${poke.id}/"
                                            ),
                                            teamId = teamId,
                                            onResult = { result ->
                                                when (result) {
                                                    is PokemonViewModel.TeamAdditionResult.Success -> {
                                                        val message = if (result.wasAdded)
                                                            "Added to ${result.teamName}!"
                                                        else
                                                            "Removed from ${result.teamName}"
                                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                    }
                                                    is PokemonViewModel.TeamAdditionResult.TeamFull -> {
                                                        Toast.makeText(context, "Team is full!", Toast.LENGTH_SHORT).show()
                                                    }
                                                    is PokemonViewModel.TeamAdditionResult.Error -> {
                                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        )
                                    }
                                },
                                onCreateNewTeam = {
                                    showTeamBottomSheet = false
                                    showCreateTeamDialog = true
                                }
                            )
                        }

                        if (showCreateTeamDialog) {
                            CreateTeamDialog(
                                onCreateTeam = { teamName ->
                                    viewModel.createTeam(
                                        teamName = teamName,
                                        onSuccess = {
                                            showCreateTeamDialog = false
                                            teamCreationError = null
                                            Toast.makeText(context, "Team \"$teamName\" created!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { error ->
                                            teamCreationError = error
                                        }
                                    )
                                },
                                onDismiss = {
                                    showCreateTeamDialog = false
                                    teamCreationError = null
                                },
                                errorMessage = teamCreationError
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
                            .background(MaterialTheme.colorScheme.background),
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
                            PokemonDetailHeader(
                                pokemon = pokemon,
                                bgColor = bgColor,
                                specialEffectsEnabled = specialEffectsEnabled,
                                spriteEffectsEnabled = spriteEffectsEnabled,
                                spriteEffectsEnabledState = spriteEffectsEnabledState,
                                isSpeaking = isSpeaking,
                                spriteVisible = spriteVisible,
                                evolutionUi = evolutionUi,
                                onPokemonClick = { name ->
                                    viewModel.fetchPokemonData(name)
                                },
                                isShinyEnabled = isShinyEnabled,
                                onShinyToggle = { isShinyEnabled = it },
                                currentSpriteUrl = currentSpriteUrl,
                                onSpriteLoaded = { loaded ->
                                    showLoader = !loaded
                                },
                                onSpriteError = { /* handle error if needed */ },
                                showLoader = showLoader,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        }

                        // Basic Info
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "ID: #${pokemon.id.toString().padStart(4, '0')}",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Height: ${pokemon.height / 10.0} m",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Weight: ${pokemon.weight / 10.0} kg",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Types
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Types",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        pokemon.types.forEach {
                                            AssistChip(
                                                onClick = {
                                                },
                                                label = {
                                                    Text(
                                                        it.type.name.uppercase(),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = MaterialTheme.colorScheme.surface,
                                                    labelColor = MaterialTheme.colorScheme.onSurface
                                                )
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
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = descriptionText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
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
                                        color = MaterialTheme.colorScheme.onSurface
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
                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.9f
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = stat.base_stat.toString(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.7f
                                                    ),
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
                                                    .background(
                                                        MaterialTheme.colorScheme.onSurface.copy(
                                                            alpha = 0.1f
                                                        )
                                                    )
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

                        item {
                            var isMovesExpanded by rememberSaveable { mutableStateOf(false) }

                            val learnableMoves = pokemon.moves
                                .filter { move ->
                                    move.version_group_details.any {
                                        it.move_learn_method.name == "level-up"
                                    }
                                }
                                .sortedBy { move ->
                                    move.version_group_details
                                        .filter { it.move_learn_method.name == "level-up" }
                                        .minOfOrNull { it.level_learned_at } ?: 999
                                }

                            val displayedMoves = if (isMovesExpanded) learnableMoves else learnableMoves.take(8)

                            if (learnableMoves.isNotEmpty()) {
                                GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Moves",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${learnableMoves.size} total",
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.6f
                                                ),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            displayedMoves.forEach { move ->
                                                val level = move.version_group_details
                                                    .filter { it.move_learn_method.name == "level-up" }
                                                    .minOfOrNull { it.level_learned_at } ?: 0

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(bgColor.copy(alpha = 0.15f))
                                                        .padding(
                                                            horizontal = 12.dp,
                                                            vertical = 10.dp
                                                        ),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = move.move.name
                                                            .replace("-", " ")
                                                            .replaceFirstChar { it.uppercase() },
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(bgColor.copy(alpha = 0.4f))
                                                            .padding(
                                                                horizontal = 10.dp,
                                                                vertical = 4.dp
                                                            )
                                                    ) {
                                                        Text(
                                                            text = if (level > 0) "Lv. $level" else "Start",
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (learnableMoves.size > 8) {
                                            Spacer(modifier = Modifier.height(8.dp))

                                            TextButton(
                                                onClick = { isMovesExpanded = !isMovesExpanded },
                                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                            ) {
                                                Text(
                                                    text = if (isMovesExpanded)
                                                        "Show Less"
                                                    else
                                                        "Show All ${learnableMoves.size} Moves",
                                                    color = bgColor
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (isMovesExpanded)
                                                        Icons.Default.KeyboardArrowUp
                                                    else
                                                        Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = bgColor,
                                                    modifier = Modifier.size(20.dp)
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
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(uiState.varieties) { variety ->
                                                ElevatedAssistChip(
                                                    onClick = {
                                                        val formName = variety.pokemon.name
                                                        val currentName = uiState.pokemon?.name
                                                        if (!formName.equals(
                                                                currentName,
                                                                ignoreCase = true
                                                            )
                                                        ) {
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
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    colors = AssistChipDefaults.assistChipColors(
                                                        containerColor = bgColor.copy(alpha = 0.3f),
                                                        labelColor = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                uiState.error is UiError.NotFound -> {
                    val missingName = (uiState.error as UiError.NotFound).name

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "No Pokémon found",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.headlineSmall
                            )

                            Text(
                                text = "\"$missingName\" doesn't exist.\nCheck spelling or try suggestions.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { navController.popBackStack() }
                                ) {
                                    Text("Go Back")
                                }

                                Button(
                                    onClick = {
                                        viewModel.fetchPokemonData("pikachu")
                                    }
                                ) {
                                    Text("Try Pikachu")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}