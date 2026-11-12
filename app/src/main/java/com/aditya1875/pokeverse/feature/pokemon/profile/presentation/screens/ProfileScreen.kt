package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.screens

import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgressStrip
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.FactOfTheDayCard
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.GameStatsSection
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.GuestLoginCard
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.ProfileHeader
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.SignedInCard
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.StatsSection
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import com.aditya1875.pokeverse.presentation.auth.AuthState
import com.aditya1875.pokeverse.utils.rememberAdaptiveHPaddingProfile
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.aditya1875.pokeverse.feature.progression.data.ProgressionRepository
import com.aditya1875.pokeverse.feature.progression.data.ProgressionState
import com.aditya1875.pokeverse.feature.progression.presentation.RewardsEntryCard

@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onEditName: () -> Unit,
    onPlayWildCatch: () -> Unit,
    /** Opens Home showing only the Pokémon behind the tapped Seen or Caught card. */
    onOpenDexList: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val photoUploading by viewModel.photoUploading.collectAsStateWithLifecycle()
    val progression by koinInject<ProgressionRepository>().state.collectAsStateWithLifecycle(ProgressionState())
    val dexViewModel: PokedexViewModel = koinViewModel()
    val dexProgress by dexViewModel.progress.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadAndSetPhoto(context, it) }
    }

    var pendingXp by remember { mutableStateOf<XPResult?>(null) }

    LaunchedEffect(profile.isGuest) {
        if (!profile.isGuest) {
            viewModel.xpEvent.collect { result ->
                pendingXp = result
            }
        }
    }

    val hPadding = rememberAdaptiveHPaddingProfile()

    XPOverlay(
        result = pendingXp,
        onDismiss = { pendingXp = null }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = hPadding)
        ) {
            // ── Sticky header: avatar card + XP progress ──────────────────
            Spacer(Modifier.height(8.dp))
            ProfileHeader(
                profile = profile,
                progression = progression,
                currentUser = currentUser,
                onEditName = onEditName,
                photoUploading = photoUploading,
                onEditPhoto = {
                    if (!photoUploading) imagePicker.launch("image/*")
                },
                onSettingsClick = onSettingsClick
            )
            Spacer(Modifier.height(12.dp))

            // ── Scrollable section: account cards + stats + actions ────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!profile.isGuest) {
                    item { StatsSection(profile = profile) }
                }

                if (!profile.isGuest) {
                    item { RewardsEntryCard(progression = progression, onClick = onRewardsClick) }
                }

                item {
                    // Plain read-out here: no onFilter, so the bars have no arrows and don't open the Pokédex list.
                    // (Home keeps its tappable bars.)
                    DexProgressStrip(
                        dexProgress,
                        onPlayWildCatch = onPlayWildCatch,
                    )
                }

                item { FactOfTheDayCard(onRevealed = viewModel::claimDailyFactXp) }

                if (profile.isGuest && authState !is AuthState.Authenticated) {
                    item { GuestLoginCard() }
                }

                if (authState is AuthState.Authenticated) {
                    item {
                        SignedInCard(
                            user = currentUser,
                            onSignOut = { viewModel.signOut() }
                        )
                    }
                }

                item { GameStatsSection(profile = profile) }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}