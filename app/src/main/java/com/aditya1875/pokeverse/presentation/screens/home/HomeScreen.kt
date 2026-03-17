package com.aditya1875.pokeverse.presentation.screens.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.aditya1875.pokeverse.presentation.screens.detail.components.CustomProgressIndicator
import com.aditya1875.pokeverse.presentation.screens.home.components.FilterBar
import com.aditya1875.pokeverse.presentation.screens.home.components.ImprovedPokemonCard
import com.aditya1875.pokeverse.navigation.components.Route
import com.aditya1875.pokeverse.presentation.screens.home.components.SuggestionRow
import com.aditya1875.pokeverse.presentation.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.utils.SearchResult
import com.aditya1875.pokeverse.utils.UiError
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.data.billing.IBillingManager
import com.aditya1875.pokeverse.data.billing.SubscriptionState
import com.aditya1875.pokeverse.domain.xp.XPResult
import com.aditya1875.pokeverse.presentation.screens.home.components.AssetsOnboardingBanner
import com.aditya1875.pokeverse.presentation.screens.home.components.DailyTriviaFab
import com.aditya1875.pokeverse.presentation.screens.home.components.DailyTriviaSheet
import com.aditya1875.pokeverse.presentation.screens.home.components.HomePopupOrchestrator
import com.aditya1875.pokeverse.presentation.screens.leaderboard.components.XPOverlay
import com.aditya1875.pokeverse.presentation.ui.viewmodel.DailyTriviaViewModel
import com.aditya1875.pokeverse.presentation.ui.viewmodel.ProfileViewModel
import com.aditya1875.pokeverse.presentation.ui.viewmodel.SettingsViewModel
import com.aditya1875.pokeverse.presentation.ui.viewmodel.TriviaUiState
import com.aditya1875.pokeverse.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.core.net.toUri
import com.aditya1875.pokeverse.presentation.screens.home.components.HomeContentMode
import com.aditya1875.pokeverse.presentation.screens.premium.components.PremiumBottomSheet
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterialApi::class
)
@Composable
fun HomeScreen(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    viewModel: PokemonViewModel = koinViewModel()
) {
    val pokemonList by viewModel.pokemonList.collectAsStateWithLifecycle()
    val isLoading = viewModel.isLoading
    val endReached = viewModel.endReached
    var query by rememberSaveable { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()

    val triviaViewModel: DailyTriviaViewModel = koinViewModel()
    val triviaState by triviaViewModel.state.collectAsStateWithLifecycle()
    val showBadge by triviaViewModel.showBadge.collectAsStateWithLifecycle()
    var showTriviaSheet by remember { mutableStateOf(false) }

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

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisibleItemIndex >= pokemonList.size - 5 &&
                    !isLoading &&
                    !endReached
        }
    }

    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    val billingManager: IBillingManager = koinInject()

    val billingViewModel: BillingViewModel = koinViewModel()

    val profileViewModel: ProfileViewModel = koinViewModel()
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
    val monthlyProduct by billingViewModel.monthlyProduct.collectAsStateWithLifecycle()
    val yearlyProduct by billingViewModel.yearlyProduct.collectAsStateWithLifecycle()
    val isBillingReady = monthlyProduct != null || yearlyProduct != null

    var showPremiumSheet by remember { mutableStateOf(false) }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadPokemonList()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPokemonList()
        query = ""

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
            monthlyPrice = monthly,
            yearlyPrice = yearly,
            isSubscribeEnabled = isBillingReady
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
                            Text(
                                "Dexverse",
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

                    val fabVisible by remember {
                        derivedStateOf { listState.firstVisibleItemIndex > 5 }
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

                                    // Play sound only if trivia not completed
                                    if (!alreadyDone) {
                                        soundManager.play(SoundManager.Sound.WHOS_THAT_POKEMON)
                                    }

                                    coroutineScope.launch {
                                        if (!alreadyDone) delay(400)

                                        showTriviaSheet = true
                                        triviaViewModel.loadTrivia()
                                    }
                                }
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
                                        listState.animateScrollToItem(0)
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
                                onTypeChange = { viewModel.setTypeFilter(it) }
                            )
                        }


                        OutlinedTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                viewModel.onSearchQueryChanged(it)
                            },
                            label = { Text("Search a Monster..") },
                            singleLine = true,
                            leadingIcon = {
                                IconButton(onClick = { showFilters = !showFilters }) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filters",
                                        tint = if (showFilters)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
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
                                    val animatedIndices = remember { mutableStateSetOf<Int>() }

                                    val visibleIndices by remember {
                                        derivedStateOf {
                                            listState.layoutInfo.visibleItemsInfo.map { it.index }
                                                .toSet()
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

                                            val isInTeam by viewModel.isInAnyTeam(pokemon.name)
                                                .collectAsStateWithLifecycle(false)

                                            ImprovedPokemonCard(
                                                pokemon = pokemon,
                                                isInTeam = isInTeam,
                                                isInFavorites = isFavorite,
                                                onAddToFavorites = {
                                                    viewModel.addToFavorites(
                                                        pokemon
                                                    )
                                                },
                                                onRemoveFromFavorites = {
                                                    viewModel.removeFromFavoritesByName(pokemon.name)
                                                },
                                                isAssetEnabled = originalAssetsEnabled,
                                                onClick = {
                                                    navController.navigate(
                                                        Route.Details.createDetails(
                                                            pokemon.name
                                                        )
                                                    )
                                                },
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
                                                    CustomProgressIndicator(
                                                        resId = R.raw.pokemon_animation
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    PullRefreshIndicator(
                                        refreshing = isLoading,
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
