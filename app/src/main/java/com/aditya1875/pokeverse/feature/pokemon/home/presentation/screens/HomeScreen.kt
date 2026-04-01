package com.aditya1875.pokeverse.feature.pokemon.home.presentation.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.core.navigation.components.Route
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.premium.components.PremiumBottomSheet
import com.aditya1875.pokeverse.feature.item.presentation.screens.ItemGridCard
import com.aditya1875.pokeverse.feature.item.presentation.screens.ItemGridSkeleton
import com.aditya1875.pokeverse.feature.item.presentation.screens.ItemListError
import com.aditya1875.pokeverse.feature.item.presentation.viewmodels.ItemListState
import com.aditya1875.pokeverse.feature.item.presentation.viewmodels.ItemViewModel
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components.CustomProgressIndicator
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.DailyTriviaFab
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.DailyTriviaSheet
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.FilterBar
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.HomeContentMode
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.HomePopupOrchestrator
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.ImprovedPokemonCard
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.SuggestionRow
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.DailyTriviaViewModel
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.PokemonListViewModel
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.SearchViewModel
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.TriviaUiState
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels.SettingsViewModel
import com.aditya1875.pokeverse.feature.team.presentation.viewmodels.FavouritesViewModel
import com.aditya1875.pokeverse.feature.team.presentation.viewmodels.TeamViewModel
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import com.aditya1875.pokeverse.utils.SearchResult
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.UiError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalSharedTransitionApi::class,
    ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class
)
@Composable
fun SharedTransitionScope.HomeScreen(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel = koinViewModel(),
    viewModel: PokemonListViewModel = koinViewModel(),
    teamViewModel: TeamViewModel = koinViewModel(),
    favouriteViewModel: FavouritesViewModel = koinViewModel(),
    searchViewModel: SearchViewModel = koinViewModel(),
    billingViewModel: BillingViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel(),
    triviaViewModel: DailyTriviaViewModel = koinViewModel(),
    itemViewModel: ItemViewModel = koinViewModel(),
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val pokemonList by viewModel.pokemonList.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val endReached = viewModel.endReached
    var query by rememberSaveable { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val searchUiState by searchViewModel.searchUiState.collectAsStateWithLifecycle()

    val triviaState by triviaViewModel.state.collectAsStateWithLifecycle()
    val showBadge by triviaViewModel.showBadge.collectAsStateWithLifecycle()
    var showTriviaSheet by remember { mutableStateOf(false) }

    val isTypeFiltering by viewModel.isTypeFiltering.collectAsStateWithLifecycle()

    var pendingXp by remember { mutableStateOf<XPResult?>(null) }

    var contentMode by rememberSaveable { mutableStateOf(HomeContentMode.POKEMON) }

    val soundManager: SoundManager = koinInject()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.refreshList() }
    )

    val originalAssetsEnabled by settingsViewModel.originalAssetsEnabled.collectAsStateWithLifecycle()
    val assetsBannerSeen by settingsViewModel.assetsBannerSeen.collectAsStateWithLifecycle()

    var isSearchFocused by remember { mutableStateOf(false) }

    var showFilters by rememberSaveable { mutableStateOf(false) }

    val pokemonGridState = rememberLazyGridState()
    val itemGridState = rememberLazyGridState()

    val itemListState by itemViewModel.listState.collectAsStateWithLifecycle()
    val filteredItems by itemViewModel.filteredItems.collectAsStateWithLifecycle()
    val searchQuery by itemViewModel.searchQuery.collectAsStateWithLifecycle()

    val textFieldValue =
        if (contentMode == HomeContentMode.POKEMON) query
        else searchQuery

    val displayList =
        if (searchQuery.isNotEmpty()) filteredItems
        else (itemListState as? ItemListState.Success)?.items ?: emptyList()

    val shouldLoadMorePokemons by remember {
        derivedStateOf {
            val lastVisible =
                pokemonGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisible >= pokemonList.size - 6 &&
                    !isLoading &&
                    !endReached
        }
    }

    val shouldLoadMoreItems by remember {
        derivedStateOf {
            val lastVisible = itemGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val successState = itemListState as? ItemListState.Success

            lastVisible >= displayList.size - 4 &&
                    successState != null &&
                    successState.canLoadMore &&
                    searchQuery.isBlank()
        }
    }

    val isSearching by searchViewModel.isSearching.collectAsStateWithLifecycle()

    val billingManager: IBillingManager = koinInject()

    val profile by profileViewModel.userProfile.collectAsStateWithLifecycle()

    val ratingPromptSeen by settingsViewModel.ratingPromptSeen.collectAsStateWithLifecycle()
    val premiumPromptShown by settingsViewModel.premiumPromptShown.collectAsStateWithLifecycle()
    val totalSessionMinutes by settingsViewModel.totalSessionMinutes.collectAsStateWithLifecycle()

    val subscriptionState by billingManager.subscriptionState.collectAsStateWithLifecycle()
    val isPremium = subscriptionState is SubscriptionState.Premium

    val context = LocalContext.current

    val activity = context as? Activity
    val monthly by billingViewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearly by billingViewModel.yearlyPrice.collectAsStateWithLifecycle()
    val lifetime by billingViewModel.lifetimePrice.collectAsStateWithLifecycle()
    val monthlyProduct by billingViewModel.monthlyProduct.collectAsStateWithLifecycle()
    val yearlyProduct by billingViewModel.yearlyProduct.collectAsStateWithLifecycle()
    val lifetimeProduct by billingViewModel.lifetimeProduct.collectAsStateWithLifecycle()
    val isBillingReady = monthlyProduct != null || yearlyProduct != null || lifetimeProduct != null

    var showPremiumSheet by remember { mutableStateOf(false) }

    LaunchedEffect(contentMode, shouldLoadMorePokemons) {
        if (contentMode == HomeContentMode.POKEMON && shouldLoadMorePokemons) {
            viewModel.loadPokemonList()
        }
    }

    LaunchedEffect(contentMode, shouldLoadMoreItems) {
        if (contentMode == HomeContentMode.ITEMS && shouldLoadMoreItems) {
            itemViewModel.loadMore()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPokemonList()
    }

    LaunchedEffect(Unit) {
        triviaViewModel.xpResult.collect { pendingXp = it }
    }

    HomePopupOrchestrator(
        assetsBannerSeen = assetsBannerSeen,
        originalAssetsEnabled = originalAssetsEnabled,
        ratingPromptSeen = ratingPromptSeen,
        premiumPromptShown = premiumPromptShown,
        totalSessionMinutes = totalSessionMinutes,
        isGuest = profile.isGuest,
        isPremium = isPremium,
        onEnableAssets = { settingsViewModel.toggleOriginalAssetsEnabled() },
        onDismissAssets = { settingsViewModel.dismissAssetsBanner() },
        onDismissRating = { settingsViewModel.markRatingPromptSeen() },
        onRateNow = {
            settingsViewModel.markRatingPromptSeen()
            val packageName = context.packageName

            val uri = "market://details?id=$packageName".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        },
        onDismissPremium = { settingsViewModel.markPremiumPromptShown() },
        onGoPremium = {
            settingsViewModel.markPremiumPromptShown()
            showPremiumSheet = true
        },
        onGoUpdate = {
            settingsViewModel.markUpdateDialogShown(BuildConfig.VERSION_CODE.toLong())
            val uri = "market://details?id=${context.packageName}".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val webUri =
                    "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        }
    )

    if (showPremiumSheet) {
        PremiumBottomSheet(
            onDismiss = { showPremiumSheet = false },
            onSubscribeMonthly = {
                showPremiumSheet = false
                activity?.let { billingViewModel.purchaseMonthly(it) }
            },
            onSubscribeYearly = {
                showPremiumSheet = false
                activity?.let { billingViewModel.purchaseYearly(it) }
            },
            onSubscribeLifetime = {
                showPremiumSheet = false
                activity?.let { billingViewModel.purchaseLifetime(it) }
            },
            monthlyPrice = monthly,
            yearlyPrice = yearly,
            isSubscribeEnabled = isBillingReady,
            lifetimePrice = lifetime
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = {
                            var showMenu by remember { mutableStateOf(false) }

                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showMenu = true }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Dexverse",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontSize = 22.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )

                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Pokémon") },
                                        trailingIcon = {
                                            if (contentMode == HomeContentMode.POKEMON) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        },
                                        onClick = {
                                            contentMode = HomeContentMode.POKEMON
                                            showMenu = false
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            if (isPremium) {
                                                Text("Items", color = MaterialTheme.colorScheme.onSurface)
                                            } else {
                                                Text(
                                                    "Items",
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (isPremium) {
                                                if (contentMode == HomeContentMode.ITEMS) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null
                                                    )
                                                }
                                            } else {
                                                Icon(Icons.Default.Lock, contentDescription = null)
                                            }
                                        },
                                        onClick = {
                                            if (isPremium) {
                                                contentMode = HomeContentMode.ITEMS
                                                showMenu = false
                                            } else{
                                                showPremiumSheet = true
                                            }
                                        },
                                        modifier = Modifier.alpha(if (isPremium) 1f else 0.5f),
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                floatingActionButton = {

                    val fabVisible by remember {
                        derivedStateOf {
                            when (contentMode) {
                                HomeContentMode.POKEMON ->
                                    pokemonGridState.firstVisibleItemIndex > 5

                                HomeContentMode.ITEMS ->
                                    itemGridState.firstVisibleItemIndex > 5
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (!profile.isGuest) {
                            DailyTriviaFab(
                                showBadge = showBadge,
                                onClick = {
                                    val alreadyDone =
                                        triviaState is TriviaUiState.Ready &&
                                                (triviaState as TriviaUiState.Ready).trivia.isAnswered

                                    if (!alreadyDone) {
                                        soundManager.play(SoundManager.Sound.WHOS_THAT_POKEMON)
                                    }

                                    coroutineScope.launch {
                                        if (!alreadyDone) delay(400)

                                        showTriviaSheet = true
                                        triviaViewModel.loadTrivia()
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = fabVisible,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    coroutineScope.launch {
                                        when (contentMode) {
                                            HomeContentMode.POKEMON -> {
                                                pokemonGridState.animateScrollToItem(0)
                                            }
                                            HomeContentMode.ITEMS -> {
                                                itemGridState.animateScrollToItem(0)
                                            }
                                        }
                                    }
                                },
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
                },
                floatingActionButtonPosition = FabPosition.Center
            ) { paddingValues ->

                val focusManager = LocalFocusManager.current

                if (showTriviaSheet && !profile.isGuest) {
                    DailyTriviaSheet(
                        state = triviaState,
                        onDismiss = {
                            showTriviaSheet = false
                            triviaViewModel.dismiss()
                        },
                        onAnswer = { correct ->
                            triviaViewModel.submitTriviaAnswer(correct)
                        }
                    )
                }

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

                        AnimatedVisibility(
                            visible = showFilters,
                            enter = slideInVertically(
                                initialOffsetY = { -it / 2 }
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    easing = FastOutSlowInEasing
                                )
                            ),
                            exit = slideOutVertically(
                                targetOffsetY = { -it / 2 }
                            ) + fadeOut(
                                animationSpec = tween(
                                    durationMillis = 200,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        ) {
                            val filterState by viewModel.filters.collectAsStateWithLifecycle()

                            FilterBar(
                                currentFilter = filterState,
                                onRegionChange = { viewModel.setRegionFilter(it) },
                                onTypeChange = { viewModel.setTypeFilter(it) },
                                isTypeFiltering = isTypeFiltering
                            )
                        }


                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = {
                                if (contentMode == HomeContentMode.POKEMON) {
                                    query = it
                                    searchViewModel.onQueryChange(it)
                                } else {
                                    itemViewModel.onSearchChange(it)
                                }
                            },
                            label = {
                                if (contentMode == HomeContentMode.POKEMON) {
                                    Text("Search a Monster..")
                                } else {
                                    Text("Search a held Item..")
                                }
                            },
                            singleLine = true,
                            leadingIcon =
                                if (contentMode == HomeContentMode.POKEMON) {
                                    {
                                        IconButton(onClick = { showFilters = !showFilters }, Modifier.animateContentSize()) {
                                            Icon(
                                                imageVector = Icons.Default.FilterList,
                                                contentDescription = "Filters",
                                                tint = if (showFilters)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else null ,

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
                                            if (contentMode == HomeContentMode.POKEMON) {
                                                query = ""
                                                searchViewModel.onQueryChange("")
                                            } else {
                                                itemViewModel.onSearchChange("")
                                            }
                                        }) {
                                            Icon(Icons.Default.Close, "Clear")
                                        }
                                    }

                                    else -> {
                                        Icon(Icons.Default.Search, "Search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .onFocusChanged { focusState ->
                                    isSearchFocused = focusState.isFocused
                                }
                        )


                        AnimatedVisibility(
                            visible = contentMode == HomeContentMode.POKEMON &&
                                    isSearchFocused &&
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
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.6f
                                                )
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
                                                            Route.Details.createDetails(
                                                                searchUiState.suggestions[searchResult].pokemon.name
                                                            )
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
                                    CustomProgressIndicator(
                                        size = 80.dp
                                    )
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
                                                color = MaterialTheme.colorScheme.onBackground.copy(
                                                    alpha = 0.7f
                                                )
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
                                            contentPadding = PaddingValues(
                                                horizontal = 22.dp,
                                                vertical = 10.dp
                                            )
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

                                    when (contentMode) {

                                        HomeContentMode.POKEMON -> {

                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(1),
                                                state = pokemonGridState,
                                                contentPadding = PaddingValues(
                                                    start = 16.dp,
                                                    end = 16.dp,
                                                    top = 8.dp,
                                                    bottom = 120.dp
                                                ),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {

                                                items(pokemonList, key = { it.name }) { pokemon ->

                                                    val isFavorite by favouriteViewModel
                                                        .isInFavorites(pokemon.name)
                                                        .collectAsStateWithLifecycle(false)

                                                    val isInTeam by teamViewModel
                                                        .isInAnyTeam(pokemon.name)
                                                        .collectAsStateWithLifecycle(false)

                                                    ImprovedPokemonCard(
                                                        pokemon = pokemon,
                                                        isInTeam = isInTeam,
                                                        isInFavorites = isFavorite,
                                                        onAddToFavorites = {
                                                            favouriteViewModel.addToFavorites(pokemon)
                                                        },
                                                        onRemoveFromFavorites = {
                                                            favouriteViewModel.removeFromFavoritesByName(pokemon.name)
                                                        },
                                                        isAssetEnabled = originalAssetsEnabled,
                                                        onClick = {
                                                            navController.navigate(
                                                                Route.Details.createDetails(pokemon.name)
                                                            )
                                                        }
                                                    )
                                                }

                                                if (isLoading && pokemonList.isNotEmpty()) {
                                                    item(span = { GridItemSpan(1) }) {
                                                        Box(
                                                            Modifier
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

                                        HomeContentMode.ITEMS -> {

                                            when (itemListState) {

                                                is ItemListState.Loading -> ItemGridSkeleton()

                                                is ItemListState.Error -> ItemListError(
                                                    message = (itemListState as ItemListState.Error).message
                                                ) {
                                                    itemViewModel.loadItems()
                                                }

                                                is ItemListState.Success -> {
                                                    val successState = itemListState as ItemListState.Success

                                                    LazyVerticalGrid(
                                                        columns = GridCells.Fixed(2),
                                                        state = itemGridState,
                                                        contentPadding = PaddingValues(
                                                            start = 16.dp,
                                                            end = 16.dp,
                                                            top = 8.dp,
                                                            bottom = 120.dp
                                                        ),
                                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        items(displayList, key = { it.id }) { item ->
                                                            ItemGridCard(
                                                                item = item,
                                                                onClick = {
                                                                    navController.navigate(
                                                                        Route.ItemDetail.createRoute(
                                                                            item.name
                                                                        )
                                                                    )
                                                                },
                                                                animatedVisibilityScope = animatedVisibilityScope
                                                            )
                                                        }

                                                        if (successState.isLoadingMore) {
                                                            item(span = { GridItemSpan(2) }) {
                                                                Box(
                                                                    Modifier
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

                                    PullRefreshIndicator(
                                        refreshing = when (contentMode) {
                                            HomeContentMode.POKEMON -> isLoading
                                            HomeContentMode.ITEMS -> itemListState is ItemListState.Loading
                                        },
                                        state = pullRefreshState,
                                        modifier = Modifier.align(Alignment.TopCenter),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
