package com.aditya1875.pokeverse.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.aditya1875.pokeverse.components.AnimatedBackground
import com.aditya1875.pokeverse.components.CustomProgressIndicator
import com.aditya1875.pokeverse.components.FilterBar
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.utils.TeamMapper.toEntity
import com.aditya1875.pokeverse.utils.UiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val viewModel: PokemonViewModel = koinViewModel()
    val pokemonList by viewModel.pokemonList.collectAsStateWithLifecycle()
    val isLoading = viewModel.isLoading
    val endReached = viewModel.endReached
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val team by viewModel.team.collectAsStateWithLifecycle()
    val teamMembershipMap = remember(team) {
        team.associate { it.name to true }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= pokemonList.size - 5 && !isLoading && !endReached
        }
    }

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
        modifier = Modifier
            .fillMaxSize()

    ) {
        AnimatedBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Pokéverse",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 26.sp, letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                // FAB will float ABOVE LazyColumn automatically
                val fabVisible by remember { derivedStateOf { listState.firstVisibleItemIndex > 5 } }
                AnimatedVisibility(
                    visible = fabVisible,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                        containerColor = Color(0xFF802525),
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top", tint = Color.White)
                    }
                }
            },


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
                    val filterState by viewModel.filters.collectAsState()
                    val cleanedQuery = query.trim().lowercase()
                    val performSearch = {
                        if (cleanedQuery.isNotBlank()) {
                            viewModel.fetchPokemonData(cleanedQuery)
                            navController.navigate("pokemon_detail/$cleanedQuery")
                        }
                    }

                    FilterBar(
                        currentFilter = filterState,
                        onRegionChange = { viewModel.setRegionFilter(it) },
                    )

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search a Pokémon", color = Color.White) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch{
                                        performSearch()
                                        delay(150)
                                        keyboardController?.hide()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
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
                            .padding(bottom = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.DarkGray,
                            unfocusedTrailingIconColor = Color.DarkGray,
                            unfocusedLabelColor = Color.DarkGray,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            focusedTrailingIconColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        )
                    )

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
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF1C1C1C), Color(0xFF101010))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
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
                                            color = Color.White,
                                            letterSpacing = 0.5.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFFB0B0B0),
                                            lineHeight = 20.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Button(
                                        onClick = { viewModel.loadPokemonList() },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF802525),
                                            contentColor = Color.White
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

                                // Compute visible item indices using derivedStateOf
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
                                        // Stagger delay
                                        val itemAnimDelay = index
                                        var isVisible by remember { mutableStateOf(false) }

                                        // Animate entry
                                        val alphaa by animateFloatAsState(
                                            targetValue = if (isVisible) 1f else 0f,
                                            animationSpec = tween(durationMillis = 400, delayMillis = itemAnimDelay.toInt()),
                                            label = "alphaAnim"
                                        )

                                        val transY by animateDpAsState(
                                            targetValue = if (isVisible) 0.dp else 20.dp,
                                            animationSpec = tween(durationMillis = 400, delayMillis = itemAnimDelay.toInt()),
                                            label = "slideAnim"
                                        )

                                        // Trigger entry when composed
                                        LaunchedEffect(Unit) {
                                            isVisible = true
                                        }

                                        // Press animation
                                        var isPressed by remember { mutableStateOf(false) }
                                        val scale by animateFloatAsState(
                                            targetValue = if (isPressed) 0.97f else 1f,
                                            animationSpec = tween(durationMillis = 100),
                                            label = "cardScale"
                                        )

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    alpha = alphaa
                                                    translationY = transY.toPx()
                                                    scaleX = scale
                                                    scaleY = scale
                                                }
                                                .clickable {
                                                    isPressed = true
                                                    coroutineScope.launch {
                                                        delay(150)
                                                        isPressed = false
                                                        withContext(Dispatchers.Main) {
                                                            navController.navigate("pokemon_detail/${pokemon.name}")
                                                        }
                                                    }
                                                }
                                                .animateItem()
                                            ,
                                            elevation = CardDefaults.cardElevation(4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(end = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {

                                                val isInTeam = teamMembershipMap[pokemon.name] ?: false
                                                Text(
                                                    text = pokemon.name.replaceFirstChar { it.uppercase() },
                                                    modifier = Modifier.padding(16.dp),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = Color.White
                                                )

                                                IconButton(
                                                    onClick = {
                                                        if (isInTeam) {
                                                            viewModel.removeFromTeamByName(pokemon.name)
                                                            Toast.makeText(
                                                                context,
                                                                "Removed from team",
                                                                Toast.LENGTH_SHORT
                                                            ).show()

                                                        } else {
                                                            if (team.size >= 6) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Team already has 6 Pokémon!",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            } else {
                                                                viewModel.addToTeam(pokemon)
                                                                Toast.makeText(
                                                                    context,
                                                                    "Added to team",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    },
                                                    enabled = isInTeam || team.size < 6
                                                ) {
                                                    Icon(
                                                        imageVector = if (isInTeam) Icons.Default.Star else Icons.Default.StarBorder,
                                                        contentDescription = if (isInTeam) "Remove from Team" else "Add to Team",
                                                        tint = if (isInTeam) Color.Yellow else Color.White.copy(
                                                            alpha = if (team.size >= 6) 0.2f else 1f
                                                        ),
                                                        modifier = Modifier.graphicsLayer(
                                                            shadowElevation = 8f,
                                                            shape = CircleShape,
                                                            clip = true
                                                        )
                                                    )
                                                }
                                            }
                                        }
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
