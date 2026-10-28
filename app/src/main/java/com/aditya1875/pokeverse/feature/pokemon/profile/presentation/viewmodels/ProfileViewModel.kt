package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import com.aditya1875.pokeverse.feature.pokemon.onboarding.Starter
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileViewModel(
    private val authManager: AuthManager,
    private val repository: UserProfileRepository,
    private val xpManager: XPManager,
    private val pokedex: PokedexRepository,
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> =
        repository.profileFlow.stateIn(
            viewModelScope, SharingStarted.Companion.Eagerly,
            UserProfile()
        )

    val canClaimEasterEgg: StateFlow<Boolean> = userProfile.map { profile ->
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        profile.lastEasterEggXpDate != today
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    private var publishedPremium: Pair<String, Boolean>? = null

    /** Mirrors premium status to the leaderboard for the flair; writes only when (user, status) changes. */
    fun publishPremiumStatus(isPremium: Boolean) {
        val uid = authManager.currentUser.value?.uid ?: return
        val key = uid to isPremium
        if (publishedPremium == key) return
        publishedPremium = key
        viewModelScope.launch { repository.publishPremiumFlag(isPremium) }
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
                    favoriteType = local.favoriteType,
                    playStyle = local.playStyle,
                    starter = local.starter,
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

    // Unlike username, a blank bio is valid — it's how a user clears it.
    fun updateBio(newBio: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(bio = newBio.trim().take(60))
            repository.saveProfile(updated)
            if (!updated.isGuest) repository.syncToFirestore(updated)
        }
    }

    /** One write for the whole Trainer Card, so name/bio/style never land half-updated. */
    fun saveTrainerCard(name: String, favoriteType: String, playStyle: String, catchphrase: String) {
        viewModelScope.launch {
            val current = userProfile.value
            val updated = current.copy(
                username = name.trim().ifBlank { current.username },
                favoriteType = favoriteType,
                playStyle = playStyle,
                bio = catchphrase.trim().take(60)
            )
            repository.saveProfile(updated)
            if (!updated.isGuest) repository.syncToFirestore(updated)
        }
    }

    /**
     * Saves the professor-intro result. Suspends (rather than launching in viewModelScope) because the
     * intro's nav entry is popped right after, which would cancel a launched write.
     * The starter's type becomes the favorite type until the trainer picks their own.
     */
    suspend fun saveStarter(name: String, starter: Starter) {
        val current = userProfile.value
        val updated = current.copy(
            username = name.trim().ifBlank { current.username },
            starter = starter.id,
            favoriteType = current.favoriteType.ifEmpty { starter.type }
        )
        repository.saveProfile(updated)
        if (!updated.isGuest) repository.syncToFirestore(updated)
        pokedex.chooseStarter(starter.dexId)
    }

    fun updateBestScore(game: String, score: Int) {
        viewModelScope.launch { repository.updateBestScore(game, score) }
    }

    fun incrementGamesPlayed() {
        viewModelScope.launch { repository.incrementGamesPlayed() }
    }

    /** Today's fact was scratched open (the card's store guarantees once a day). */
    fun claimDailyFactXp() {
        viewModelScope.launch {
            val result = xpManager.awardGameXP(XPEvent.DailyFact)
            if (result.xpGained > 0) _xpEvent.emit(result)
        }
    }

    fun claimEasterEggXP() {
        viewModelScope.launch {
            val result = xpManager.awardGameXP(XPEvent.EasterEggClaim)
            if (result.xpGained > 0) {
                _xpEvent.emit(result)
            }
        }
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
                            favoriteType = local.favoriteType,
                            playStyle = local.playStyle,
                            starter = local.starter,
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