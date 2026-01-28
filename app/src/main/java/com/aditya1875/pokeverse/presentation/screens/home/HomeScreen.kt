package com.aditya1875.pokeverse.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.presentation.screens.detail.components.CustomProgressIndicator
import com.aditya1875.pokeverse.presentation.screens.home.components.FilterBar
import com.aditya1875.pokeverse.presentation.screens.home.components.ImprovedPokemonCard
import com.aditya1875.pokeverse.presentation.screens.home.components.SuggestionRow
import com.aditya1875.pokeverse.presentation.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.utils.SearchResult
import com.aditya1875.pokeverse.utils.UiError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val viewModel: PokemonViewModel = koinViewModel()
    val pokemonList by viewModel.pokemonList.collectAsStateWithLifecycle()
    val isLoading = viewModel.isLoading
    val endReached = viewModel.endReached
    var query by rememberSaveable { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val team by viewModel.team.collectAsStateWithLifecycle()
    val teamMembershipMap = remember(team) {
        team.associate { it.name to true }
    }
    var isSearchFocused by remember { mutableStateOf(false) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= pokemonList.size - 5 && !isLoading && !endReached
        }
    }

    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadPokemonList()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPokemonList()
        query = ""
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Pokeverse",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 26.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            floatingActionButton = {
                val fabVisible by remember { derivedStateOf { listState.firstVisibleItemIndex > 5 } }
                AnimatedVisibility(
                    visible = fabVisible,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Scroll to top",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        ) { paddingValues ->
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    val filterState by viewModel.filters.collectAsStateWithLifecycle()
                    val performSearch = {
                        val cleaned = query.trim().lowercase()
                        if (cleaned.length >= 2) {
                            isSearchFocused = false
                            navController.navigate("pokemon_detail/$cleaned")
                        }
                    }

                    FilterBar(
                        currentFilter = filterState,
                        onRegionChange = { viewModel.setRegionFilter(it) },
                    )

                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            viewModel.onSearchQueryChanged(it)
                        },
                        label = {
                            Text(
                                "Search a Pokémon",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        trailingIcon = {
                            when {
                                isSearching -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                }
                                query.isNotEmpty() -> {
                                    IconButton(onClick = {
                                        query = ""
                                        viewModel.onSearchQueryChanged("")
                                    }) {
                                        Icon(
                                            Icons.Default.Close,
                                            "Clear",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                else -> {
                                    Icon(
                                        Icons.Default.Search,
                                        "Search",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        keyboardActions = KeyboardActions {
                            coroutineScope.launch {
                                performSearch()
                                delay(150)
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .padding(bottom = 4.dp)
                            .onFocusChanged { focusState ->
                                isSearchFocused = focusState.isFocused
                            },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        )
                    )

                    AnimatedVisibility(
                        visible = isSearchFocused &&
                                (searchUiState.showSuggestions || searchUiState.isLoading),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .heightIn(max = 400.dp)
                        ) {
                            when {
                                searchUiState.isLoading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                searchUiState.suggestions.isEmpty() && query.length >= 2 -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No Pokémon found",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                searchUiState.suggestions.isNotEmpty() -> {
                                    LazyColumn {
                                        items(searchUiState.suggestions.count()) { searchResult ->
                                            SuggestionRow(
                                                searchResult = SearchResult(
                                                    pokemon = searchUiState.suggestions[searchResult].pokemon,
                                                    score = searchUiState.suggestions[searchResult].score,
                                                    baseName = searchUiState.suggestions[searchResult].baseName,
                                                    formLabel = searchUiState.suggestions[searchResult].formLabel
                                                ),
                                                onClick = {
                                                    isSearchFocused = false
                                                    query = ""
                                                    navController.navigate(
                                                        "pokemon_detail/${
                                                            searchUiState.suggestions[searchResult].pokemon.name
                                                        }"
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    when {
                        isLoading && pokemonList.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CustomProgressIndicator()
                            }
                        }

                        uiState.error != null && pokemonList.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(15.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.nointrnet),
                                        contentDescription = "No Internet",
                                        modifier = Modifier
                                            .size(260.dp)
                                            .graphicsLayer {
                                                alpha = 0.95f
                                                scaleX = 1.05f
                                                scaleY = 1.05f
                                            },
                                        contentScale = ContentScale.Fit
                                    )

                                    val (title, subtitle) = when (uiState.error) {
                                        is UiError.Network -> "No Internet Connection" to "Check your network and try again."
                                        is UiError.Unexpected -> "Something went wrong" to "An unexpected error occurred."
                                        else -> "Unknown Error" to "Please try again later."
                                    }

                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            letterSpacing = 0.5.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        ),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Button(
                                        onClick = { viewModel.loadPokemonList() },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(8.dp),
                                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Retry",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Retry",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = 0.3.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val animatedIndices = remember { mutableStateSetOf<Int>() }

                                val visibleIndices by remember {
                                    derivedStateOf {
                                        listState.layoutInfo.visibleItemsInfo.map { it.index }.toSet()
                                    }
                                }

                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(pokemonList) { index, pokemon ->
                                        LaunchedEffect(index, visibleIndices) {
                                            if (visibleIndices.contains(index)) {
                                                animatedIndices.add(index)
                                            }
                                        }

                                        val isFavorite by viewModel.isInFavorites(pokemon.name)
                                            .collectAsStateWithLifecycle(false)

                                        val isInTeam by viewModel.isInTeam(pokemon.name)
                                            .collectAsStateWithLifecycle(false)

                                        ImprovedPokemonCard(
                                            pokemon = pokemon,
                                            isInTeam = isInTeam,
                                            isInFavorites = isFavorite,
                                            teamSize = team.size,
                                            onAddToTeam = { viewModel.addToTeam(pokemon) },
                                            onRemoveFromTeam = {
                                                viewModel.removeFromTeamByName(
                                                    pokemon.name
                                                )
                                            },
                                            onAddToFavorites = { viewModel.addToFavorites(pokemon) },
                                            onRemoveFromFavorites = {
                                                viewModel.removeFromFavoritesByName(
                                                    pokemon.name
                                                )
                                            },
                                            onClick = { navController.navigate("pokemon_detail/${pokemon.name}") }
                                        )
                                    }

                                    if (isLoading && pokemonList.isNotEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CustomProgressIndicator()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
