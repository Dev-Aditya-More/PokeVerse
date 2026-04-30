package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.GameStatsSection
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.GuestLoginCard
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.ProfileActions
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.ProfileHeader
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.SignedInCard
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.StatsSection
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.XPProgress
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import com.aditya1875.pokeverse.presentation.auth.AuthState
import com.aditya1875.pokeverse.utils.rememberAdaptiveHPaddingProfile
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    onEditName: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.updatePhoto(it.toString())
        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = hPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                ProfileHeader(
                    profile = profile,
                    currentUser = currentUser,
                    onEditName = onEditName,
                    onEditPhoto = {
                        imagePicker.launch("image/*")
                    }
                )
            }

            if (authState is AuthState.Authenticated) {
                item {
                    XPProgress(profile = profile)
                }
            }

            if (profile.isGuest && authState !is AuthState.Authenticated) {
                item {
                    GuestLoginCard()
                }
            }

            if (authState is AuthState.Authenticated) {
                item {
                    SignedInCard(
                        user = currentUser,
                        onSignOut = { viewModel.signOut() }
                    )
                }
            }

            if (!profile.isGuest) {
                item {
                    StatsSection(profile = profile)
                }
            }

            item {
                GameStatsSection(profile = profile)
            }

            item {
                ProfileActions(onSettingsClick = onSettingsClick)
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}