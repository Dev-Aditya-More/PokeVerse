package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.presentation.auth.AuthManager
import com.aditya1875.pokeverse.presentation.auth.AuthResult
import com.aditya1875.pokeverse.presentation.auth.AuthState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authManager: AuthManager,
    private val repository: UserProfileRepository,
    private val xpManager: XPManager,
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> =
        repository.profileFlow.stateIn(
            viewModelScope, SharingStarted.Companion.Eagerly,
            UserProfile()
        )

    val authState: StateFlow<AuthState> = authManager.authState
    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser

    private val _xpEvent = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpEvent: SharedFlow<XPResult> = _xpEvent.asSharedFlow()

    private var hasSyncedThisSession = false

    init {
        viewModelScope.launch {
            currentUser.filterNotNull().first().let { user ->
                if (!hasSyncedThisSession) {
                    hasSyncedThisSession = true
                    syncFromCloud(user.uid)
                }
            }
        }
    }

    fun onAppLaunch() {
        viewModelScope.launch {
            val uid = authManager.currentUser.value?.uid
            if (uid != null && !hasSyncedThisSession) {
                hasSyncedThisSession = true
                syncFromCloud(uid)
            }
            val result = xpManager.awardDailyXP()
            result?.let { _xpEvent.emit(it) }
        }
    }

    private fun String.isCustomUsername() =
        isNotBlank() && this != "Trainer" && this != "Guest Trainer"

    private suspend fun syncFromCloud(uid: String) {
        val cloudProfile = repository.loadFromFirestore(uid) ?: return
        val local = userProfile.value

        if (cloudProfile.totalXp >= local.totalXp) {
            val localDateIsNewer = local.lastDailyXpDate > cloudProfile.lastDailyXpDate
            repository.saveProfile(
                cloudProfile.copy(
                    username = if (local.username.isNotBlank() && local.username != "Trainer")
                        local.username
                    else
                        cloudProfile.username,
                    lastDailyXpDate = if (localDateIsNewer) local.lastDailyXpDate
                    else cloudProfile.lastDailyXpDate,
                    dailyStreak     = if (localDateIsNewer) local.dailyStreak
                    else cloudProfile.dailyStreak,
                )
            )
        } else {
            repository.syncToFirestore(local)
        }
    }

    // ── Username update — explicit user action, always wins ───────────────────
    fun updateUsername(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val updated = userProfile.value.copy(username = newName.trim())
            repository.saveProfile(updated)
            if (!updated.isGuest) repository.syncToFirestore(updated)
        }
    }

    fun updateBestScore(game: String, score: Int) {
        viewModelScope.launch { repository.updateBestScore(game, score) }
    }

    fun incrementGamesPlayed() {
        viewModelScope.launch { repository.incrementGamesPlayed() }
    }

    suspend fun signInWithGoogle(activity: Activity): AuthResult {
        val result = authManager.signInWithGoogle(activity)
        if (result is AuthResult.Success) {
            viewModelScope.launch {
                val uid = result.user.uid
                val cloudProfile = repository.loadFromFirestore(uid)
                val local = userProfile.value

                val merged = when {
                    cloudProfile == null ->
                        local.copy(
                            uid = uid,
                            username = result.user.displayName ?: local.username,
                            photoUrl = result.user.photoUrl?.toString() ?: "",
                            isGuest = false
                        )
                    cloudProfile.totalXp >= local.totalXp -> {
                        val localDateIsNewer = local.lastDailyXpDate > cloudProfile.lastDailyXpDate
                        cloudProfile.copy(
                            photoUrl = userProfile.value.photoUrl.ifEmpty { result.user.photoUrl?.toString() ?: cloudProfile.photoUrl },
                            username = if (local.username.isCustomUsername()) local.username
                            else cloudProfile.username,
                            isGuest = false,
                            lastDailyXpDate = if (localDateIsNewer) local.lastDailyXpDate
                            else cloudProfile.lastDailyXpDate,
                            dailyStreak = if (localDateIsNewer) local.dailyStreak
                            else cloudProfile.dailyStreak,
                        )
                    }
                    else -> local.copy(
                        uid = uid,
                        photoUrl = result.user.photoUrl?.toString() ?: "",
                        isGuest = false
                    )
                }
                repository.saveProfile(merged)
                repository.syncToFirestore(merged)
                hasSyncedThisSession = true
            }
        }
        return result
    }

    fun signOut() {
        hasSyncedThisSession = false
        viewModelScope.launch {
            authManager.signOut()
            repository.clearLocal()
        }
    }

    fun updatePhoto(url: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(photoUrl = url)
            repository.saveProfile(updated)
        }
    }

    fun isSignedIn(): Boolean = authManager.isSignedIn()
}