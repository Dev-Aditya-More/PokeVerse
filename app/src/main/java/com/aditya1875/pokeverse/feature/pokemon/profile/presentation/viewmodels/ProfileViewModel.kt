package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels

import android.app.Activity
import android.content.Context
import android.net.Uri
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    private val _photoUploading = MutableStateFlow(false)
    val photoUploading: StateFlow<Boolean> = _photoUploading.asStateFlow()

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
                    photoUrl = local.photoUrl.ifEmpty { cloudProfile.photoUrl },
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

                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .update("fcmToken", token)
                    }

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

    fun uploadAndSetPhoto(context: Context, uri: Uri) {
        val uid = authManager.currentUser.value?.uid ?: return
        viewModelScope.launch {
            _photoUploading.value = true
            try {
                val ref = FirebaseStorage.getInstance()
                    .reference.child("profile_photos/$uid.jpg")
                val stream = context.contentResolver.openInputStream(uri) ?: return@launch
                ref.putStream(stream).await()
                stream.close()
                val downloadUrl = ref.downloadUrl.await().toString()
                val updated = userProfile.value.copy(photoUrl = downloadUrl)
                repository.saveProfile(updated)
                repository.syncToFirestore(updated)
            } catch (_: Exception) {
            } finally {
                _photoUploading.value = false
            }
        }
    }

    fun isSignedIn(): Boolean = authManager.isSignedIn()
}