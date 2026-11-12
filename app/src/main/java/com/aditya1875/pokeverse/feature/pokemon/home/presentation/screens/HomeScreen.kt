package com.aditya1875.pokeverse.feature.pokemon.home.presentation.screens

import androidx.compose.runtime.mutableIntStateOf
import com.aditya1875.pokeverse.feature.core.ui.components.LoadErrorState
import com.aditya1875.pokeverse.utils.toLoadErrorKind
import com.aditya1875.pokeverse.feature.pokemon.dex.DexFilter
import androidx.compose.ui.text.style.TextAlign
import android.app.Activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgressStrip
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexViewModel
import com.aditya1875.pokeverse.feature.pokemon.dex.pokemonIdFromUrl
import androidx.navigation.NavHostController

import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.core.navigation.components.Route
import com.aditya1875.pokeverse.feature.core.ui.components.NewBadge
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.badges.domain.GymBadge
import com.aditya1875.pokeverse.feature.game.premium.components.PremiumBottomSheet
import com.aditya1875.pokeverse.feature.badges.presentation.screens.BadgeDetailSheet
import com.aditya1875.pokeverse.feature.badges.presentation.screens.BadgeGridCard
import com.aditya1875.pokeverse.feature.badges.presentation.screens.BadgeRegionFilter
import com.aditya1875.pokeverse.feature.badges.presentation.viewmodels.BadgesViewModel
import com.aditya1875.pokeverse.feature.characters.domain.PokeCharacter
import com.aditya1875.pokeverse.feature.characters.presentation.screens.CharacterDetailSheet
import com.aditya1875.pokeverse.feature.characters.presentation.screens.CharacterGridCard
import com.aditya1875.pokeverse.feature.characters.presentation.screens.CharacterRoleFilter
import com.aditya1875.pokeverse.feature.characters.presentation.viewmodels.CharactersViewModel
import com.aditya1875.pokeverse.feature.berry.presentation.screens.BerryGridCard
import com.aditya1875.pokeverse.feature.berry.presentation.screens.BerryGridSkeleton
import com.aditya1875.pokeverse.feature.berry.presentation.screens.BerryListError
import com.aditya1875.pokeverse.feature.berry.presentation.viewmodels.BerryListState
import com.aditya1875.pokeverse.feature.berry.presentation.viewmodels.BerryViewModel
import com.aditya1875.pokeverse.feature.item.presentation.screens.ItemGridCard
import com.aditya1875.pokeverse.feature.item.presentation.screens.ItemGridSkeleton
import com.aditya1875.pokeverse.feature.item.presentation.screens.ItemListError
import com.aditya1875.pokeverse.feature.item.presentation.viewmodels.ItemListState
import com.aditya1875.pokeverse.feature.item.presentation.viewmodels.ItemViewModel
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components.CustomProgressIndicator
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.DailyHoppingPokemon
import com.aditya1875.pokeverse.feature.dexdaily.presentation.DexDailyCard
import com.aditya1875.pokeverse.feature.dexdaily.presentation.DexDailySheet
import com.aditya1875.pokeverse.feature.dexdaily.presentation.DexDailyViewModel
import com.aditya1875.pokeverse.feature.game.premium.LocalPremium
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.FilterBar
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.DexverseMenu
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.HomeContentMode
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.HomePopupOrchestrator
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.ImprovedPokemonCard
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.SuggestionRow
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.PokemonListViewModel
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.ScreenState
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.SearchViewModel
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels.SettingsViewModel
import com.aditya1875.pokeverse.feature.team.presentation.viewmodels.FavouritesViewModel
import com.aditya1875.pokeverse.feature.team.presentation.viewmodels.TeamViewModel
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import com.aditya1875.pokeverse.utils.IReviewManager
import com.aditya1875.pokeverse.utils.SearchResult
import com.aditya1875.pokeverse.utils.rememberAdaptiveHPadding
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.UiError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.aditya1875.pokeverse.feature.dexmode.data.DexModeStore
import com.aditya1875.pokeverse.feature.dexmode.presentation.DexModeNudgeDot
import androidx.compose.material.icons.filled.PhoneAndroid
import com.aditya1875.pokeverse.feature.referral.presentation.ReferralViewModel
import com.aditya1875.pokeverse.feature.referral.presentation.InviteFriendsSheet
import com.aditya1875.pokeverse.feature.feedback.presentation.FeedbackViewModel
import com.aditya1875.pokeverse.feature.feedback.presentation.FeedbackSheet

@Suppress("EffectKeys")
@OptIn(
    ExperimentalSharedTransitionApi::class,
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
    dexDailyViewModel: DexDailyViewModel = koinViewModel(),
    referralViewModel: ReferralViewModel = koinViewModel(),
    feedbackViewModel: FeedbackViewModel = koinViewModel(),
    itemViewModel: ItemViewModel = koinViewModel(),
    berryViewModel: BerryViewModel = koinViewModel(),
    badgesViewModel: BadgesViewModel = koinViewModel(),
    charactersViewModel: CharactersViewModel = koinViewModel(),
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val pokemonList by viewModel.pokemonList.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val endReached = viewModel.endReached
    var query by rememberSaveable { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val searchUiState by searchViewModel.searchUiState.collectAsStateWithLifecycle()

    val dexDailyOpen by dexDailyViewModel.openDate.collectAsStateWithLifecycle()
    // After midnight (app left open or resumed), the card moves on to the new puzzle.
    val dexLifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(dexLifecycleOwner) {
        dexLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { dexDailyViewModel.refreshDay() }
    }

    val isTypeFiltering by viewModel.isTypeFiltering.collectAsStateWithLifecycle()

    var pendingXp by remember { mutableStateOf<XPResult?>(null) }

    var contentMode by rememberSaveable { mutableStateOf(HomeContentMode.POKEMON) }

    val soundManager: SoundManager = koinInject()
    val reviewManager: IReviewManager = koinInject()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading && pokemonList.isNotEmpty(),
        onRefresh = { viewModel.refreshList() }
    )

    val originalAssetsEnabled = true
    val mysteryMode by settingsViewModel.mysterySilhouettes.collectAsStateWithLifecycle()
    val dexViewModel: PokedexViewModel = koinViewModel()
    val challengesViewModel: com.aditya1875.pokeverse.feature.challenges.presentation.ChallengesViewModel = koinViewModel()
    val challengesUi by challengesViewModel.ui.collectAsStateWithLifecycle()
    var challengesOpen by rememberSaveable { mutableStateOf(false) }
    var rewardBurst by remember { mutableStateOf<com.aditya1875.pokeverse.feature.challenges.domain.BallReward?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) { challengesViewModel.paid.collect { rewardBurst = it } }
    val dexProgress by dexViewModel.progress.collectAsStateWithLifecycle()

    // A message from the developer (published to everyone), shown once per device.
    val inboxViewModel: com.aditya1875.pokeverse.feature.inbox.presentation.viewmodels.InboxViewModel = koinViewModel()
    val announcement by inboxViewModel.popup.collectAsStateWithLifecycle()
    announcement?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { inboxViewModel.dismissPopup() },
            title = { Text("${msg.emoji} ${msg.title}") },
            text = { Text(msg.body) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { inboxViewModel.dismissPopup() }) {
                    Text(stringResource(R.string.battle_help_got_it))
                }
            },
        )
    }
    var dexFilter by rememberSaveable { mutableStateOf(DexFilter.ALL) }
    // Seen/Caught need the whole region in memory, not just the pages scrolled so far.
    LaunchedEffect(dexFilter, isLoading, viewModel.endReached) {
        if (dexFilter != DexFilter.ALL && !isLoading) viewModel.loadRemaining()
    }
    // A request from another screen (Profile's Seen/Caught cards) to open the list filtered.
    var dexScrollTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        dexViewModel.requestedFilter.collect { requested ->
            if (requested != null) {
                dexFilter = requested
                dexViewModel.consumeRequestedFilter()
                dexScrollTick++
            }
        }
    }
    val visiblePokemon = remember(pokemonList, dexFilter, dexProgress) {
        when (dexFilter) {
            DexFilter.ALL -> pokemonList
            DexFilter.SEEN -> pokemonList.filter { pokemonIdFromUrl(it.url) in dexProgress.seen }
            DexFilter.CAUGHT -> pokemonList.filter { pokemonIdFromUrl(it.url) in dexProgress.roster }
        }
    }

    var isSearchFocused by remember { mutableStateOf(false) }

    var showFilters by rememberSaveable { mutableStateOf(false) }

    val pokemonGridState = rememberLazyGridState()
    // After a filter is chosen, bring the matching Pokémon into view right under the cards.
    LaunchedEffect(dexScrollTick) {
        if (dexScrollTick == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(80)
        val strip = pokemonGridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "dex_progress" }
        pokemonGridState.animateScrollToItem(strip?.index ?: 0)
    }
    val itemGridState = rememberLazyGridState()
    val berryGridState = rememberLazyGridState()

    val itemListState by itemViewModel.listState.collectAsStateWithLifecycle()
    val filteredItems by itemViewModel.filteredItems.collectAsStateWithLifecycle()
    val searchQuery by itemViewModel.searchQuery.collectAsStateWithLifecycle()

    val berryListState by berryViewModel.listState.collectAsStateWithLifecycle()
    val filteredBerries by berryViewModel.filteredBerries.collectAsStateWithLifecycle()
    val berrySearchQuery by berryViewModel.searchQuery.collectAsStateWithLifecycle()

    val badges by badgesViewModel.badges.collectAsStateWithLifecycle()
    val badgeSearchQuery by badgesViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedRegion by badgesViewModel.selectedRegion.collectAsStateWithLifecycle()
    var badgeDetail by remember { mutableStateOf<GymBadge?>(null) }

    val characters by charactersViewModel.characters.collectAsStateWithLifecycle()
    val characterSearchQuery by charactersViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedRole by charactersViewModel.selectedRole.collectAsStateWithLifecycle()
    var characterDetail by remember { mutableStateOf<PokeCharacter?>(null) }

    val canClaimEasterEgg by profileViewModel.canClaimEasterEgg.collectAsStateWithLifecycle()

    val textFieldValue = when (contentMode) {
        HomeContentMode.POKEMON -> query
        HomeContentMode.BERRIES -> berrySearchQuery
        HomeContentMode.BADGES -> badgeSearchQuery
        HomeContentMode.CHARACTERS -> characterSearchQuery
        else -> searchQuery
    }

    val displayList =
        if (searchQuery.isNotEmpty()) filteredItems
        else (itemListState as? ItemListState.Success)?.items ?: emptyList()

    val listFilters by viewModel.filters.collectAsStateWithLifecycle()

    // Near the end of what is ON SCREEN. This used to compare the grid position against the size of the whole
    // unfiltered list, which is wrong the moment Seen/Caught narrows the grid, and it read `endReached` as a value
    // copied at the first composition (so it never saw it change). A type filter already shows that type's complete
    // list, so there is nothing more to page in while one is on.
    val shouldLoadMorePokemons by remember(visiblePokemon, listFilters.selectedType) {
        derivedStateOf {
            val lastVisible =
                pokemonGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisible >= visiblePokemon.size - 6 &&
                    !isLoading &&
                    !viewModel.endReached &&
                    listFilters.selectedType == null
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

    val totalSessionMinutes by settingsViewModel.totalSessionMinutes.collectAsStateWithLifecycle()

    val subscriptionState by billingManager.subscriptionState.collectAsStateWithLifecycle()
    val isPremium = subscriptionState is SubscriptionState.Premium

    val context = LocalContext.current

    val activity = context as? Activity
    val monthly by billingViewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearly by billingViewModel.yearlyPrice.collectAsStateWithLifecycle()
    val lifetime by billingViewModel.lifetimePrice.collectAsStateWithLifecycle()
    val isBillingReady = monthly.isNotBlank() || yearly.isNotBlank() || lifetime.isNotBlank()

    var showPremiumSheet by remember { mutableStateOf(false) }

    // Keyed on everything that can change the answer, not just the yes/no flag. Keyed on the flag alone, applying a
    // filter while it was already "yes" changed nothing the effect could see, so it never restarted and paging
    // silently stopped until the player scrolled away and back.
    LaunchedEffect(contentMode, shouldLoadMorePokemons, isLoading, visiblePokemon.size, listFilters, dexFilter) {
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
        dexDailyViewModel.xp.collect { pendingXp = it }
    }

    LaunchedEffect(Unit) {
        profileViewModel.xpEvent.collect { pendingXp = it }
    }

    // Referrals: remember a friend's code from the install link, and pay out
    // XP for friends who joined with ours since the last check.
    LaunchedEffect(Unit) { referralViewModel.captureInstallReferrer() }
    LaunchedEffect(profile.isGuest) { if (!profile.isGuest) referralViewModel.claimRewards() }
    LaunchedEffect(Unit) { referralViewModel.xp.collect { pendingXp = it } }
    LaunchedEffect(Unit) { feedbackViewModel.xp.collect { pendingXp = it } }

    var showInviteSheet by rememberSaveable { mutableStateOf(false) }
    var invitePrefillCode by rememberSaveable { mutableStateOf<String?>(null) }
    var showFeedbackSheet by rememberSaveable { mutableStateOf(false) }

    if (showInviteSheet) {
        InviteFriendsSheet(
            viewModel = referralViewModel,
            prefillCode = invitePrefillCode,
            onDismiss = {
                showInviteSheet = false
                invitePrefillCode = null
            }
        )
    }
    if (showFeedbackSheet) {
        FeedbackSheet(viewModel = feedbackViewModel, onDismiss = { showFeedbackSheet = false })
    }

    val dexModeStore: DexModeStore = koinInject()
    val dexModeDiscovered by dexModeStore.discovered.collectAsStateWithLifecycle()

    HomePopupOrchestrator(
        totalSessionMinutes = totalSessionMinutes,
        isGuest = profile.isGuest,
        isPremium = isPremium,
        dailyStreak = profile.dailyStreak,
        onRateNow = {
            activity?.let { reviewManager.requestReview(it) }
        },
        onGoPremium = {
            showPremiumSheet = true
        },
        onOpenDexMode = { navController.navigate(Route.DexMode.route) },
        onOpenInvite = { prefill ->
            invitePrefillCode = prefill
            showInviteSheet = true
        },
        onOpenFeedback = { showFeedbackSheet = true },
        onOpenRewards = { navController.navigate(Route.Rewards.route) },
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
            lifetimePrice = lifetime,
            isSubscribeEnabled = isBillingReady
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
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

                                if (!dexModeDiscovered) {
                                    DexModeNudgeDot(Modifier.padding(start = 6.dp))
                                }

                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            DexverseMenu(
                                expanded = showMenu,
                                contentMode = contentMode,
                                onDismiss = { showMenu = false },
                                onContentMode = { contentMode = it; showMenu = false },
                                onDexMode = { navController.navigate(Route.DexMode.route); showMenu = false },
                                onFaceMatch = { navController.navigate(Route.FaceMatch.route); showMenu = false },
                                onCompare = { navController.navigate(Route.ComparePokemon.route); showMenu = false },
                                onStatCalculator = { navController.navigate(Route.StatCalculator.route); showMenu = false },
                            )
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
                // The challenges button sits where the Card Clash button used to be.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    com.aditya1875.pokeverse.feature.challenges.presentation.ChallengesFab(
                        waiting = challengesUi.waiting,
                        onClick = { challengesOpen = true },
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { paddingValues ->

            val focusManager = LocalFocusManager.current

            if (challengesOpen) {
                com.aditya1875.pokeverse.feature.challenges.presentation.ChallengesSheet(
                    ui = challengesUi,
                    onClaim = challengesViewModel::claim,
                    onClaimSweep = challengesViewModel::claimSweep,
                    onClaimAll = challengesViewModel::claimAll,
                    onDismiss = { challengesOpen = false },
                )
            }
            rewardBurst?.let { r ->
                com.aditya1875.pokeverse.feature.challenges.presentation.RewardBurst(r) { rewardBurst = null }
            }

            dexDailyOpen?.let { date ->
                DexDailySheet(dexDailyViewModel, date, onDismiss = { dexDailyViewModel.close() })
            }

            badgeDetail?.let { badge ->
                BadgeDetailSheet(badge = badge, onDismiss = { badgeDetail = null })
            }

            characterDetail?.let { character ->
                CharacterDetailSheet(character = character, onDismiss = { characterDetail = null })
            }

            if (canClaimEasterEgg) {
                Box(modifier = Modifier.zIndex(100f)) {
                    DailyHoppingPokemon(
                        onClicked = { profileViewModel.claimEasterEggXP() }
                    )
                }
            }

            Box(modifier = Modifier.zIndex(200f)) {
                XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {}
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
                            isTypeFiltering = isTypeFiltering,
                            dexFilter = dexFilter,
                            onDexFilterChange = { dexFilter = it }
                        )
                    }


                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = {
                            when (contentMode) {
                                HomeContentMode.POKEMON -> {
                                    query = it
                                    searchViewModel.onQueryChange(it)
                                }
                                HomeContentMode.ITEMS -> itemViewModel.onSearchChange(it)
                                HomeContentMode.BERRIES -> berryViewModel.onSearchChange(it)
                                HomeContentMode.BADGES -> badgesViewModel.onSearchChange(it)
                                HomeContentMode.CHARACTERS -> charactersViewModel.onSearchChange(it)
                            }
                        },
                        label = {
                            when (contentMode) {
                                HomeContentMode.POKEMON -> Text(stringResource(R.string.home_search_pokemon))
                                HomeContentMode.ITEMS -> Text(stringResource(R.string.home_search_item))
                                HomeContentMode.BERRIES -> Text(stringResource(R.string.home_search_berries))
                                HomeContentMode.BADGES -> Text(stringResource(R.string.home_search_badges))
                                HomeContentMode.CHARACTERS -> Text(stringResource(R.string.home_search_characters))
                            }
                        },
                        singleLine = true,
                        leadingIcon =
                            if (contentMode == HomeContentMode.POKEMON) {
                                {
                                    IconButton(
                                        onClick = { showFilters = !showFilters },
                                        Modifier.animateContentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = stringResource(R.string.home_filters),
                                            tint = if (showFilters)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else null,

                        trailingIcon = {
                            val activeQuery = when (contentMode) {
                                HomeContentMode.POKEMON -> query
                                HomeContentMode.BERRIES -> berrySearchQuery
                                HomeContentMode.BADGES -> badgeSearchQuery
                                HomeContentMode.CHARACTERS -> characterSearchQuery
                                else -> searchQuery
                            }
                            when {
                                isSearching -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                }

                                activeQuery.isNotEmpty() -> {
                                    IconButton(onClick = {
                                        when (contentMode) {
                                            HomeContentMode.POKEMON -> {
                                                query = ""
                                                searchViewModel.onQueryChange("")
                                            }
                                            HomeContentMode.ITEMS -> itemViewModel.onSearchChange("")
                                            HomeContentMode.BERRIES -> berryViewModel.onSearchChange("")
                                            HomeContentMode.BADGES -> badgesViewModel.onSearchChange("")
                                            HomeContentMode.CHARACTERS -> charactersViewModel.onSearchChange("")
                                        }
                                    }) {
                                        Icon(Icons.Default.Close, stringResource(R.string.home_clear_search))
                                    }
                                }

                                else -> {
                                    Icon(Icons.Default.Search, stringResource(R.string.home_search))
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
                                            stringResource(R.string.home_no_pokemon_found),
                                            color = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.6f
                                            )
                                        )
                                    }
                                }

                                searchUiState.suggestions.isNotEmpty() -> {
                                    LazyColumn {
                                        items(searchUiState.suggestions) { suggestion ->
                                            SuggestionRow(
                                                searchResult = SearchResult(
                                                    pokemon = suggestion.pokemon,
                                                    score = suggestion.score,
                                                    baseName = suggestion.baseName,
                                                    formLabel = suggestion.formLabel
                                                ),
                                                onClick = {
                                                    isSearchFocused = false
                                                    query = ""
                                                    navController.navigate(
                                                        Route.Details.createDetails(
                                                            suggestion.pokemon.name
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

                    when (screenState) {
                        ScreenState.LOADING -> {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomProgressIndicator(size = 80.dp)
                            }
                        }

                        ScreenState.ERROR -> LoadErrorState(
                            kind = uiState.error?.toLoadErrorKind(),
                            onRetry = { viewModel.retry() },
                            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                        )

                        ScreenState.CONTENT ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                val hPadding = rememberAdaptiveHPadding()

                                when (contentMode) {

                                    HomeContentMode.POKEMON -> {

                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(minSize = 320.dp),
                                            state = pokemonGridState,
                                            contentPadding = PaddingValues(
                                                start = hPadding,
                                                end = hPadding,
                                                top = 8.dp,
                                                bottom = 120.dp
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {

                                            item(key = "dex_daily", span = { GridItemSpan(maxLineSpan) }) {
                                                DexDailyCard(
                                                    viewModel = dexDailyViewModel,
                                                    isPremium = LocalPremium.current,
                                                    onPlay = { firstOpen ->
                                                        if (firstOpen) soundManager.play(SoundManager.Sound.WHOS_THAT_POKEMON)
                                                        dexDailyViewModel.open(dexDailyViewModel.today.value)
                                                    },
                                                    onOpenArchive = { navController.navigate(Route.DexDailyArchive.route) }
                                                )
                                            }

                                            item(key = "dex_progress", span = { GridItemSpan(maxLineSpan) }) {
                                                DexProgressStrip(
                                                    dexProgress,
                                                    onPlayWildCatch = { navController.navigate(Route.WildCatchPlay.route) },
                                                    activeFilter = dexFilter,
                                                    // Tapping the active card again clears the filter.
                                                    onFilter = { picked ->
                                                        dexFilter = if (dexFilter == picked) DexFilter.ALL else picked
                                                        dexScrollTick++
                                                    }
                                                )
                                            }

                                            if (dexFilter != DexFilter.ALL && visiblePokemon.isEmpty() && viewModel.endReached && !isLoading) {
                                                item(key = "dex_empty", span = { GridItemSpan(maxLineSpan) }) {
                                                    Text(
                                                        text = stringResource(
                                                            if (dexFilter == DexFilter.CAUGHT) R.string.dex_empty_caught else R.string.dex_empty_seen
                                                        ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp)
                                                    )
                                                }
                                            }

                                            items(visiblePokemon, key = { it.name }) { pokemon ->

                                                val isFavorite by favouriteViewModel
                                                    .isInFavorites(pokemon.name)
                                                    .collectAsStateWithLifecycle(false)

                                                val isInTeam by teamViewModel
                                                    .isInAnyTeam(pokemon.name)
                                                    .collectAsStateWithLifecycle(false)

                                                val dexId = pokemonIdFromUrl(pokemon.url)
                                                // Real artwork once the entry is registered (opened or met), not before.
                                                val inDexSeen = dexProgress.isRevealed(dexId)
                                                val inDexCaught = dexId in dexProgress.roster

                                                ImprovedPokemonCard(
                                                    pokemon = pokemon,
                                                    seen = !mysteryMode || inDexSeen || isInTeam || isFavorite,
                                                    caught = inDexCaught,
                                                    isInTeam = isInTeam,
                                                    isInFavorites = isFavorite,
                                                    onAddToFavorites = {
                                                        favouriteViewModel.addToFavorites(
                                                            pokemon
                                                        )
                                                    },
                                                    onRemoveFromFavorites = {
                                                        favouriteViewModel.removeFromFavoritesByName(
                                                            pokemon.name
                                                        )
                                                    },
                                                    isAssetEnabled = originalAssetsEnabled,
                                                    onClick = {
                                                        navController.navigate(
                                                            Route.Details.createDetails(pokemon.name)
                                                        )
                                                    },
                                                    modifier = Modifier.animateItem(
                                                        fadeInSpec = tween(280),
                                                        fadeOutSpec = tween(200),
                                                        placementSpec = tween(320, easing = FastOutSlowInEasing)
                                                    )
                                                )
                                            }

                                            if (isLoading && pokemonList.isNotEmpty()) {
                                                item(span = { GridItemSpan(maxLineSpan) }) {
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
                                                val successState =
                                                    itemListState as ItemListState.Success

                                                LazyVerticalGrid(
                                                    columns = GridCells.Adaptive(minSize = 180.dp),
                                                    state = itemGridState,
                                                    contentPadding = PaddingValues(
                                                        start = hPadding,
                                                        end = hPadding,
                                                        top = 8.dp,
                                                        bottom = 120.dp
                                                    ),
                                                    verticalArrangement = Arrangement.spacedBy(
                                                        10.dp
                                                    ),
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        10.dp
                                                    ),
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    items(
                                                        displayList,
                                                        key = { it.id }) { item ->
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
                                                        item(span = { GridItemSpan(maxLineSpan) }) {
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

                                    HomeContentMode.BERRIES -> {
                                        when (val bs = berryListState) {
                                            is BerryListState.Loading -> BerryGridSkeleton()
                                            is BerryListState.Error -> BerryListError(
                                                message = bs.message,
                                                onRetry = { berryViewModel.loadBerries() }
                                            )
                                            is BerryListState.Success -> {
                                                val displayBerries =
                                                    if (berrySearchQuery.isNotEmpty()) filteredBerries
                                                    else bs.berries
                                                LazyVerticalGrid(
                                                    columns = GridCells.Adaptive(minSize = 180.dp),
                                                    state = berryGridState,
                                                    contentPadding = PaddingValues(
                                                        start = hPadding,
                                                        end = hPadding,
                                                        top = 8.dp,
                                                        bottom = 120.dp
                                                    ),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    items(displayBerries, key = { it.id }) { berry ->
                                                        BerryGridCard(
                                                            berry = berry,
                                                            onClick = {
                                                                navController.navigate(
                                                                    Route.BerryDetail.createRoute(berry.name)
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    HomeContentMode.BADGES -> {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            BadgeRegionFilter(
                                                regions = badgesViewModel.regions,
                                                selected = selectedRegion,
                                                onSelect = { badgesViewModel.onRegionSelect(it) }
                                            )
                                            LazyVerticalGrid(
                                                columns = GridCells.Adaptive(minSize = 160.dp),
                                                contentPadding = PaddingValues(
                                                    start = hPadding,
                                                    end = hPadding,
                                                    top = 8.dp,
                                                    bottom = 120.dp
                                                ),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                items(badges, key = { "${it.region}_${it.name}" }) { badge ->
                                                    BadgeGridCard(
                                                        badge = badge,
                                                        onClick = { badgeDetail = badge }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HomeContentMode.CHARACTERS -> {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            CharacterRoleFilter(
                                                roles = charactersViewModel.roles,
                                                selected = selectedRole,
                                                onSelect = { charactersViewModel.onRoleSelect(it) }
                                            )
                                            LazyVerticalGrid(
                                                columns = GridCells.Adaptive(minSize = 160.dp),
                                                contentPadding = PaddingValues(
                                                    start = hPadding,
                                                    end = hPadding,
                                                    top = 8.dp,
                                                    bottom = 120.dp
                                                ),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                items(characters, key = { it.name }) { character ->
                                                    CharacterGridCard(
                                                        character = character,
                                                        onClick = { characterDetail = character }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                PullRefreshIndicator(
                                    refreshing = when (contentMode) {
                                        HomeContentMode.POKEMON -> isLoading
                                        HomeContentMode.ITEMS -> itemListState is ItemListState.Loading
                                        HomeContentMode.BERRIES -> berryListState is BerryListState.Loading
                                        // Badges & characters are bundled data, never loading
                                        else -> false
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