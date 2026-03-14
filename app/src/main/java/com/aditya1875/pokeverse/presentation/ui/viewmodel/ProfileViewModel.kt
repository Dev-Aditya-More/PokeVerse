package com.aditya1875.pokeverse.presentation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.domain.xp.XPEvent
import com.aditya1875.pokeverse.domain.xp.XPManager
import com.aditya1875.pokeverse.domain.xp.XPResult
import android.app.Activity
import com.aditya1875.pokeverse.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.data.remote.model.UserProfile
import com.aditya1875.pokeverse.presentation.auth.AuthManager
import com.aditya1875.pokeverse.presentation.auth.AuthResult
import com.aditya1875.pokeverse.presentation.auth.AuthState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class ProfileViewModel(
    private val authManager: AuthManager,
    private val repository: UserProfileRepository,
    private val xpManager: XPManager,
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> =
        repository.profileFlow
            .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    val authState: StateFlow<AuthState> = authManager.authState
    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser

    private val _xpEvent = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpEvent: SharedFlow<XPResult> = _xpEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                user?.let {
                    syncFromCloud(it.uid)
                }
            }
        }
    }

    fun onAppLaunch() {
        viewModelScope.launch {
            val uid = authManager.currentUser.value?.uid

            if (uid != null) {
                syncFromCloud(uid)

                val result = xpManager.awardDailyXP()
                result?.let { _xpEvent.emit(it) }
            }
        }
    }

    private suspend fun syncFromCloud(uid: String) {
        val cloudProfile = repository.loadFromFirestore(uid) ?: return
        val local = userProfile.value
        if (cloudProfile.totalXp >= local.totalXp) {
            repository.saveProfile(cloudProfile)
        } else {
            // Local is ahead of cloud — push local up to cloud
            repository.syncToFirestore(local)
        }
    }

    fun updateBestScore(game: String, score: Int) {
        viewModelScope.launch { repository.updateBestScore(game, score) }
    }

    fun incrementGamesPlayed() {
        viewModelScope.launch { repository.incrementGamesPlayed() }
    }

    suspend fun signInWithGoogle(activity: Activity): AuthResult {
        val result = authManager.signInWithGoogle()
        if (result is AuthResult.Success) {
            viewModelScope.launch {
                val uid = result.user.uid
                val cloudProfile = repository.loadFromFirestore(uid)
                val local = userProfile.value

                val merged = when {
                    cloudProfile == null -> {
                        // Brand new user — use local state, stamp real uid
                        local.copy(
                            uid = uid,
                            username = result.user.displayName ?: local.username,
                            photoUrl = result.user.photoUrl?.toString() ?: "",
                            isGuest = false
                        )
                    }
                    cloudProfile.totalXp >= local.totalXp -> {
                        // Cloud is ahead or equal — use cloud, refresh photo/name
                        cloudProfile.copy(
                            photoUrl = result.user.photoUrl?.toString() ?: cloudProfile.photoUrl,
                            username = result.user.displayName ?: cloudProfile.username,
                            isGuest = false
                        )
                    }
                    else -> {
                        // Local is ahead — keep local, stamp uid/name/photo
                        local.copy(
                            uid = uid,
                            username = result.user.displayName ?: local.username,
                            photoUrl = result.user.photoUrl?.toString() ?: "",
                            isGuest = false
                        )
                    }
                }
                repository.saveProfile(merged)
                repository.syncToFirestore(merged)
            }
        }
        return result
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            repository.clearLocal()
        }
    }

    fun updateUsername(newName: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(username = newName)
            repository.saveProfile(updated)

            if (!updated.isGuest) {
                repository.syncToFirestore(updated)
            }
        }
    }

    fun updatePhoto(url: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(photoUrl = url)

            repository.saveProfile(updated)

            if (!updated.isGuest) {
                repository.syncToFirestore(updated)
            }
        }
    }

    fun isSignedIn(): Boolean = authManager.isSignedIn()
}