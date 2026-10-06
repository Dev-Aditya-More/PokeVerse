package com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components

import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.feature.pokemon.shiny.LocalShinyDex
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.ui.res.stringResource
import com.aditya1875.pokeverse.R
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonResult
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.screens.GlossyCard
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.screens.getPokemonBackgroundColor
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.viewmodels.PokemonDetailsViewModel
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.FlavorTextEntry
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokeGame
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.AddToTeamBottomSheet
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.PokemonListViewModel
import com.aditya1875.pokeverse.feature.team.presentation.components.CreateTeamDialog
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels.SettingsViewModel
import com.aditya1875.pokeverse.feature.team.presentation.viewmodels.FavouritesViewModel
import com.aditya1875.pokeverse.feature.team.presentation.viewmodels.TeamViewModel
import com.aditya1875.pokeverse.feature.core.ui.components.LegendaryBadge
import com.aditya1875.pokeverse.utils.DisplayMove
import com.aditya1875.pokeverse.utils.LegendaryPokemon
import com.aditya1875.pokeverse.utils.UiError
import com.aditya1875.pokeverse.utils.rememberAdaptiveHPadding
import com.aditya1875.pokeverse.utils.rememberDetailHeaderMaxWidth
import org.koin.androidx.compose.koinViewModel

@Suppress("EffectKeys")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun PokemonDetailPage(
    navController: NavController,
    specialEffectsEnabled: Boolean,
    spriteEffectsEnabledState: MutableState<Boolean>,
    viewModel: PokemonDetailsViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
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

    var isPlayingCry by remember { mutableStateOf(false) }
    val mediaPlayer = remember {
        MediaPlayer().apply {
            setOnCompletionListener { isPlayingCry = false }
        }
    }

    val teamViewModel: TeamViewModel = koinViewModel()
    val favouritesViewModel: FavouritesViewModel = koinViewModel()

    DisposableEffect(Unit) {
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }

    LaunchedEffect(uiState) {
        Log.d(
            "DetailScreen",
            "Loading=${uiState.isLoading}, pokemon=${uiState.pokemon}, error=${uiState.error}"
        )
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
                        Toast.makeText(context, context.getString(R.string.detail_cry_error), Toast.LENGTH_SHORT).show()
                        isPlayingCry = false
                        true
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.detail_cry_unavailable), Toast.LENGTH_SHORT).show()
                    Log.e("PokemonDetail", "Error playing cry", e)
                }
            } else {
                Toast.makeText(context, context.getString(R.string.detail_cry_unavailable_for_pokemon), Toast.LENGTH_SHORT)
                    .show()
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

    val selectedGameId by settingsViewModel.selectedGame.collectAsStateWithLifecycle()
    val selectedGame = remember(selectedGameId) { selectedGameId?.let { PokeGame.fromId(it) } }

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

    // Premium Shiny Dex makes shiny the default; the toggle itself stays free.
    val shinyByDefault = LocalShinyDex.current
    var isShinyEnabled by rememberSaveable { mutableStateOf(shinyByDefault) }
    var currentSpriteSource by rememberSaveable { mutableStateOf("official-artwork") }
    var currentSpriteUrl by rememberSaveable {
        mutableStateOf(
            pokemon?.sprites?.other?.officialArtwork?.frontDefault
                ?: pokemon?.sprites?.other?.home?.frontDefault
        )
    }

    val show3DModel = currentSpriteSource == "go"
    val adaptiveHPadding = rememberAdaptiveHPadding()
    val headerMaxWidth = rememberDetailHeaderMaxWidth()

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
        currentSpriteSource = when {
            pokemon?.sprites?.other?.officialArtwork?.frontDefault != null -> "official-artwork"
            pokemon?.sprites?.other?.home?.frontDefault != null -> "home"
            else -> "official-artwork"
        }
        isShinyEnabled = shinyByDefault
        // Resolved here too: if isShinyEnabled didn't change, the effect below won't re-run.
        currentSpriteUrl = getSpriteUrl(currentSpriteSource, shinyByDefault)
            ?: pokemon?.sprites?.other?.officialArtwork?.frontDefault
            ?: pokemon?.sprites?.other?.home?.frontDefault
    }

    LaunchedEffect(isShinyEnabled) {
        if (currentSpriteSource != "go") {
            val newUrl = getSpriteUrl(currentSpriteSource, isShinyEnabled)
            if (newUrl != null && newUrl != currentSpriteUrl) {
                currentSpriteUrl = newUrl
                showLoader = true
            }
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
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val orderedSources = buildList {
                                    if (pokemon?.sprites?.other?.officialArtwork?.frontDefault != null ||
                                        pokemon?.sprites?.other?.officialArtwork?.frontShiny != null)
                                        add("official-artwork")
                                    if (pokemon?.sprites?.other?.home?.frontDefault != null ||
                                        pokemon?.sprites?.other?.home?.frontShiny != null)
                                        add("home")
                                    if (pokemon?.sprites?.other?.dreamWorld?.frontDefault != null ||
                                        pokemon?.sprites?.other?.dreamWorld?.frontShiny != null)
                                        add("dream-world")
                                    if (pokemon?.sprites?.other?.showdown?.frontDefault != null ||
                                        pokemon?.sprites?.other?.showdown?.frontShiny != null)
                                        add("showdown")
                                    add("go")
                                }

                                val currentIndex = orderedSources.indexOf(currentSpriteSource)
                                val nextIndex = if (currentIndex == -1 || currentIndex == orderedSources.lastIndex) 0 else currentIndex + 1
                                val nextSource = orderedSources[nextIndex]

                                currentSpriteSource = nextSource
                                if (nextSource != "go") {
                                    val newUrl = getSpriteUrl(nextSource, isShinyEnabled)
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
                                        val label = context.getString(
                                            when (currentSpriteSource) {
                                                "official-artwork" -> R.string.sprite_style_official
                                                "home" -> R.string.sprite_style_home
                                                "dream-world" -> R.string.sprite_style_dream_world
                                                "showdown" -> R.string.sprite_style_showdown
                                                "go" -> R.string.sprite_style_go
                                                else -> R.string.sprite_style_default
                                            }
                                        )
                                        Toast.makeText(context, context.getString(R.string.sprite_style_toast, label), Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = stringResource(R.string.detail_switch_sprite_style),
                                tint = if (show3DModel) bgColor else MaterialTheme.colorScheme.onSurface
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
                                    contentDescription = stringResource(R.string.detail_audio_options),
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
                                                if (isSpeaking) stringResource(R.string.detail_audio_stop_pokedex_entry)
                                                else stringResource(R.string.detail_audio_pokedex_entry),
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
                                                context.getString(R.string.detail_initializing),
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
                                                if (isPlayingCry) stringResource(R.string.detail_audio_stop_cry)
                                                else stringResource(R.string.detail_audio_cry),
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

                        val isInFavorites by favouritesViewModel.isInFavorites(pokemon?.name ?: "")
                            .collectAsStateWithLifecycle(initialValue = false)

                        val allTeamsWithMembers by teamViewModel.allTeamsWithMembers.collectAsStateWithLifecycle()
                        val teamsContainingPokemon by teamViewModel.getTeamsForPokemon(
                            pokemon?.name ?: ""
                        )
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
                                    favouritesViewModel.addToFavorites(
                                        PokemonResult(
                                            name = pokemonData.name,
                                            url = "https://pokeapi.co/api/v2/pokemon/${pokemonData.id}/"
                                        )
                                    )
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.favorites_added, pokemonData.name.replaceFirstChar { c -> c.uppercase() }),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onRemoveFromFavorites = {
                                pokemon?.let {
                                    favouritesViewModel.removeFromFavoritesByName(it.name)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.favorites_removed),
                                        Toast.LENGTH_SHORT
                                    ).show()
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
                                        teamViewModel.togglePokemonInTeam(
                                            pokemonResult = PokemonResult(
                                                name = poke.name,
                                                url = "https://pokeapi.co/api/v2/pokemon/${poke.id}/"
                                            ),
                                            teamId = teamId,
                                            onResult = { result ->
                                                when (result) {
                                                    is TeamViewModel.TeamAdditionResult.Success -> {
                                                        val message = if (result.wasAdded)
                                                            context.getString(R.string.team_added_to, result.teamName)
                                                        else
                                                            context.getString(R.string.team_removed_from, result.teamName)
                                                        Toast.makeText(
                                                            context,
                                                            message,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }

                                                    is TeamViewModel.TeamAdditionResult.TeamFull -> {
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.team_is_full),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }

                                                    is TeamViewModel.TeamAdditionResult.Error -> {
                                                        Toast.makeText(
                                                            context,
                                                            result.message,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
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
                                    teamViewModel.createTeam(
                                        teamName = teamName,
                                        onSuccess = {
                                            showCreateTeamDialog = false
                                            teamCreationError = null
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.team_created, teamName),
                                                Toast.LENGTH_SHORT
                                            ).show()
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
                            .padding(horizontal = adaptiveHPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
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
                                        viewModel.loadPokemon(name)
                                    },
                                    isShinyEnabled = isShinyEnabled,
                                    onShinyToggle = { isShinyEnabled = it },
                                    currentSpriteUrl = currentSpriteUrl,
                                    onSpriteLoaded = { loaded ->
                                        showLoader = !loaded
                                    },
                                    onSpriteError = { /* handle error if needed */ },
                                    showLoader = showLoader,
                                    show3DModel = show3DModel,
                                    modifier = Modifier
                                        .widthIn(max = headerMaxWidth)
                                        .fillMaxWidth()
                                        .aspectRatio(1f),
                                    settingsViewModel = settingsViewModel
                                )
                            }
                        }

                        // Basic Info
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            stringResource(R.string.detail_id, pokemon.id.toString().padStart(4, '0')),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (LegendaryPokemon.isLegendary(pokemon.id)) LegendaryBadge()
                                    }
                                    Text(
                                        stringResource(R.string.detail_height, (pokemon.height / 10.0).toString()),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        stringResource(R.string.detail_weight, (pokemon.weight / 10.0).toString()),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        item {
                            var selectedType by remember { mutableStateOf<String?>(null) }

                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {

                                    Text(
                                        stringResource(R.string.detail_types),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pokemon.types.forEach { type ->

                                            val typeName = type.type.name

                                            AssistChip(
                                                onClick = {
                                                    selectedType =
                                                        if (selectedType == typeName) null else typeName
                                                },
                                                label = {
                                                    Text(localizedTypeName(typeName).uppercase())
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor =
                                                        if (selectedType == typeName)
                                                            bgColor.copy(alpha = 0.3f)
                                                        else
                                                            MaterialTheme.colorScheme.surface
                                                )
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = selectedType != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(bgColor.copy(alpha = 0.15f))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = typeHint(selectedType ?: ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Abilities
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {

                                    var selectedAbility by remember { mutableStateOf<String?>(null) }

                                    Text(
                                        text = stringResource(R.string.detail_abilities),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    pokemon.abilities
                                        .sortedBy { it.slot }
                                        .forEach { ability ->

                                            val abilityInfo = getAbilityInfo(ability.ability.name)

                                            val abilityName = ability.ability.name

                                            val backBrush =
                                                if (selectedAbility == abilityName) {
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            bgColor.copy(alpha = 0.25f),
                                                            bgColor.copy(alpha = 0.25f)
                                                        )
                                                    )
                                                } else {
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            bgColor.copy(alpha = 0.15f),
                                                            bgColor.copy(alpha = 0.05f)
                                                        )
                                                    )
                                                }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        backBrush
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                                    .clickable {
                                                        val name = ability.ability.name
                                                        selectedAbility = if (selectedAbility == name) null else name
                                                    },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {

                                                Text(
                                                    text = ability.ability.name
                                                        .replace("-", " ")
                                                        .replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                if (ability.is_hidden) {
                                                    AssistChip(
                                                        onClick = {},
                                                        label = { Text(stringResource(R.string.detail_hidden)) }
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(4.dp))

                                            AnimatedVisibility(visible = selectedAbility == abilityName) {
                                                if (abilityInfo != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(bgColor.copy(alpha = 0.15f))
                                                            .padding(10.dp)
                                                    ) {
                                                        Column {
                                                            Text(
                                                                abilityInfo.short,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = bgColor
                                                            )

                                                            Spacer(Modifier.height(4.dp))

                                                            Text(
                                                                abilityInfo.detailed,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(4.dp))
                                        }
                                }
                            }
                        }

                        if (descriptionText.isNotBlank()) {
                            item {
                                GlossyCard {
                                    InfoBlock(
                                        title = stringResource(R.string.detail_overview),
                                        accentColor = MaterialTheme.colorScheme.onSurface,
                                        content = {
                                            Text(
                                                simplifyPokemonDescription(descriptionText),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // stats
                        item {
                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        stringResource(R.string.detail_base_stats),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    pokemon.stats.forEachIndexed { index, stat ->
                                        var targetFraction by remember(stat.stat.name) { mutableStateOf(0f) }
                                        val animatedProgress by animateFloatAsState(
                                            targetValue = targetFraction,
                                            animationSpec = tween(
                                                durationMillis = 900,
                                                delayMillis = index * 100,
                                                easing = FastOutSlowInEasing
                                            ),
                                            label = "stat_$index"
                                        )
                                        LaunchedEffect(stat.base_stat) {
                                            targetFraction = stat.base_stat / 255f
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = statLabel(stat.stat.name).replace("-", " ")
                                                            .replaceFirstChar { it.uppercase() },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                                            alpha = 0.9f
                                                        )
                                                    )
                                                    if (stat.effort > 0) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(50),
                                                            color = bgColor.copy(alpha = 0.16f)
                                                        ) {
                                                            Text(
                                                                text = stringResource(R.string.ev_yield_badge, stat.effort),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = bgColor
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = (animatedProgress * 255f).roundToInt().coerceIn(0, stat.base_stat).toString(),
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
                                                        .fillMaxWidth(animatedProgress)
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

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    )

                                    val totalBst = pokemon.stats.sumOf { it.base_stat }
                                    var totalTarget by remember { mutableStateOf(0f) }
                                    val totalAnimated by animateFloatAsState(
                                        targetValue = totalTarget,
                                        animationSpec = tween(
                                            durationMillis = 900,
                                            delayMillis = pokemon.stats.size * 100,
                                            easing = FastOutSlowInEasing
                                        ),
                                        label = "totalBstAnimation"
                                    )
                                    LaunchedEffect(totalBst) {
                                        totalTarget = (totalBst / 720f).coerceIn(0f, 1f)
                                    }

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
                                                text = stringResource(R.string.detail_total),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = (totalAnimated * 720f).roundToInt().coerceIn(0, totalBst).toString(),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = bgColor,
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
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                                )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(totalAnimated)
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

                        item {
                            StatCalculatorCard(pokemon = pokemon, accentColor = bgColor)
                        }

                        item {
                            GoBattlePowerCard(pokemonId = pokemon.id, accentColor = bgColor)
                        }

                        val movesByMethod: Map<String, List<DisplayMove>> =
                            pokemon.moves
                                .flatMap { move ->
                                    move.version_group_details.map { detail ->
                                        Triple(
                                            move.move.name,
                                            detail.move_learn_method.name,
                                            detail.level_learned_at
                                        )
                                    }
                                }
                                .groupBy { it.second }
                                .mapValues { (_, entries) ->
                                    entries
                                        .groupBy { it.first }
                                        .map { (moveName, sameMoves) ->
                                            val minLevel = sameMoves.minOf { it.third }
                                            if (minLevel > 0)
                                                DisplayMove(moveName, minLevel)
                                            else
                                                DisplayMove(moveName, null)
                                        }
                                        .sortedWith(
                                            compareBy<DisplayMove> { it.level ?: Int.MAX_VALUE }
                                                .thenBy { it.name }
                                        )
                                }

                        item {
                            var expandedMethod by rememberSaveable { mutableStateOf<String?>(null) }

                            GlossyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(R.string.detail_moves),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    movesByMethod.forEach { (method, moves) ->

                                        val displayName = when (method) {
                                            "level-up" -> stringResource(R.string.detail_method_level_up)
                                            "machine" -> stringResource(R.string.detail_method_tm)
                                            "tutor" -> stringResource(R.string.detail_method_tutor)
                                            "egg" -> stringResource(R.string.detail_method_egg)
                                            else -> method.replaceFirstChar { it.uppercase() }
                                        }

                                        val isExpanded = expandedMethod == method

                                        // Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bgColor.copy(alpha = 0.18f))
                                                .clickable {
                                                    expandedMethod = if (isExpanded) null else method
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(stringResource(R.string.moves_group_count, displayName, moves.size))
                                            Icon(
                                                imageVector = if (isExpanded)
                                                    Icons.Default.KeyboardArrowUp
                                                else
                                                    Icons.Default.KeyboardArrowDown,
                                                contentDescription = null
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        moves.take(6).chunked(2).forEach { pair ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                pair.forEach { move ->
                                                    MoveChip(move, method, bgColor, Modifier.weight(1f))
                                                }
                                                if (pair.size < 2) Spacer(Modifier.weight(1f))
                                            }
                                            Spacer(Modifier.height(6.dp))
                                        }

                                        AnimatedVisibility(visible = isExpanded) {
                                            Column {
                                                moves.drop(6).chunked(2).forEach { pair ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        pair.forEach { move ->
                                                            MoveChip(move, method, bgColor, Modifier.weight(1f))
                                                        }
                                                        if (pair.size < 2) Spacer(Modifier.weight(1f))
                                                    }
                                                    Spacer(Modifier.height(6.dp))
                                                }
                                            }
                                        }

                                        // Toggle
                                        if (moves.size > 6) {
                                            Text(
                                                text = stringResource(if (isExpanded) R.string.action_show_less else R.string.action_show_all),
                                                modifier = Modifier
                                                    .align(Alignment.End)
                                                    .clickable {
                                                        expandedMethod =
                                                            if (isExpanded) null else method
                                                    }
                                                    .padding(6.dp),
                                                color = bgColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Spacer(Modifier.height(12.dp))
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
                                            text = stringResource(R.string.detail_other_forms),
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
                                                            viewModel.loadVarietyPokemon(formName)
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
                                text = stringResource(R.string.detail_not_found_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.headlineSmall
                            )

                            Text(
                                text = stringResource(R.string.detail_not_found_body, missingName),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { navController.popBackStack() }
                                ) {
                                    Text(stringResource(R.string.action_go_back))
                                }

                                Button(
                                    onClick = {
                                        viewModel.loadPokemon("pikachu")
                                    }
                                ) {
                                    Text(stringResource(R.string.detail_try_pikachu))
                                }
                            }
                        }
                    }
                }

                else -> {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            TopAppBar(
                                title = {},
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                ),
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.back),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->

                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.detail_failed_to_load))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoveChip(move: DisplayMove, method: String, bgColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = move.name.replace("-", " ").replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (method == "level-up" && move.level != null) {
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(bgColor.copy(alpha = 0.4f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${move.level}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InfoBlock(
    title: String,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        content()
    }
}

fun simplifyPokemonDescription(text: String): String {
    return text
        .replace("\n", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

data class AbilityInfo(
    val short: String,
    val detailed: String
)

/** Hand-written blurbs for well-known abilities: (short, detailed) string resources. */
private val abilityBlurbs: Map<String, Pair<Int, Int>> = mapOf(
    "overgrow" to (R.string.ability_overgrow_short to R.string.ability_overgrow_detail),
    "blaze" to (R.string.ability_blaze_short to R.string.ability_blaze_detail),
    "torrent" to (R.string.ability_torrent_short to R.string.ability_torrent_detail),
    "intimidate" to (R.string.ability_intimidate_short to R.string.ability_intimidate_detail),
    "levitate" to (R.string.ability_levitate_short to R.string.ability_levitate_detail),
    "pressure" to (R.string.ability_pressure_short to R.string.ability_pressure_detail),
    "static" to (R.string.ability_static_short to R.string.ability_static_detail),
    "swift-swim" to (R.string.ability_swift_swim_short to R.string.ability_swift_swim_detail),
    "chlorophyll" to (R.string.ability_chlorophyll_short to R.string.ability_chlorophyll_detail),
    "huge-power" to (R.string.ability_huge_power_short to R.string.ability_huge_power_detail),
    "guts" to (R.string.ability_guts_short to R.string.ability_guts_detail),
    "shed-skin" to (R.string.ability_shed_skin_short to R.string.ability_shed_skin_detail),
    "soundproof" to (R.string.ability_soundproof_short to R.string.ability_soundproof_detail),
    "adaptability" to (R.string.ability_adaptability_short to R.string.ability_adaptability_detail)
)

@Composable
fun getAbilityInfo(name: String): AbilityInfo? {
    val (short, detailed) = abilityBlurbs[name] ?: return null
    return AbilityInfo(stringResource(short), stringResource(detailed))
}

/** Type ids each type is strong / weak against (summary shown when a type chip is tapped). */
private val typeMatchups: Map<String, Pair<List<String>, List<String>>> = mapOf(
    "fire" to (listOf("grass", "bug", "ice") to listOf("water", "rock")),
    "water" to (listOf("fire", "rock") to listOf("electric", "grass")),
    "grass" to (listOf("water", "rock") to listOf("fire", "ice")),
    "electric" to (listOf("water", "flying") to listOf("ground")),
    "ice" to (listOf("dragon", "flying") to listOf("fire", "rock")),
    "fighting" to (listOf("normal", "rock") to listOf("psychic", "fairy")),
    "poison" to (listOf("grass", "fairy") to listOf("ground")),
    "ground" to (listOf("fire", "electric") to listOf("water", "grass")),
    "flying" to (listOf("grass", "fighting") to listOf("electric", "ice")),
    "psychic" to (listOf("fighting", "poison") to listOf("dark")),
    "bug" to (listOf("grass", "psychic") to listOf("fire")),
    "rock" to (listOf("fire", "flying") to listOf("water", "grass")),
    "ghost" to (listOf("psychic") to listOf("dark")),
    "dragon" to (listOf("dragon") to listOf("ice", "fairy")),
    "dark" to (listOf("psychic", "ghost") to listOf("fighting")),
    "steel" to (listOf("ice", "rock") to listOf("fire")),
    "fairy" to (listOf("dragon", "dark") to listOf("steel")),
    "normal" to (emptyList<String>() to listOf("fighting"))
)

/** "Strong vs Grass, Bug | Weak vs Water", built from localized type names. */
@Composable
fun typeHint(type: String): String {
    val (strong, weak) = typeMatchups[type.lowercase()] ?: return ""
    val weakNames = weak.map { localizedTypeName(it) }.joinToString(", ")
    return if (strong.isEmpty()) stringResource(R.string.type_hint_no_strengths, weakNames)
    else stringResource(R.string.type_hint, strong.map { localizedTypeName(it) }.joinToString(", "), weakNames)
}

@Composable
fun statLabel(stat: String): String = when (stat) {
    "hp" -> stringResource(R.string.stat_hp)
    "attack" -> stringResource(R.string.stat_attack)
    "defense" -> stringResource(R.string.stat_defense)
    "special-attack" -> stringResource(R.string.stat_sp_attack)
    "special-defense" -> stringResource(R.string.stat_sp_defense)
    "speed" -> stringResource(R.string.stat_speed)
    else -> stat
}
