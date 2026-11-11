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
import com.aditya1875.pokeverse.feature.progression.data.ProgressionRepository
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.ProfilePhotoEncoder
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import com.aditya1875.pokeverse.feature.dexdaily.data.DexDailyRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileViewModel(
    private val authManager: AuthManager,
    private val repository: UserProfileRepository,
    private val xpManager: XPManager,
    private val pokedex: PokedexRepository,
    private val challenges: com.aditya1875.pokeverse.feature.challenges.data.ChallengeRepository,
    private val progression: ProgressionRepository,
    private val dexDaily: DexDailyRepository,
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

    // Declared before `init` on purpose: init launches a coroutine that locks it right away.
    private val cloudSyncMutex = Mutex()

    init {
        // isGuest is the local mirror of "no Firebase session". An app update or a backup restore
        // can leave it saying signed-in while Firebase has no user: the profile then hid BOTH the
        // sign-in and sign-out cards and the leaderboard stayed locked until data was cleared.
        // Once Firebase has settled, repair it a single time (progress is kept; signing back in
        // merges with the cloud copy as usual). One-shot so it can't race signOut's clearLocal().
        viewModelScope.launch {
            authManager.sessionResolved.first { it }
            if (authManager.currentUser.value == null && !repository.profileFlow.first().isGuest) {
                repository.markGuest()
            }
        }

        viewModelScope.launch {
            currentUser.filterNotNull().first()
            ensureSyncedFromCloud()
        }
    }

    /**
     * Pulls the account's cloud copy onto this phone once per session. Everything that pays XP waits
     * on this, so a phone that is a day behind can't pay "today's" reward the account already
     * received on another device. The mutex makes the second caller wait for the first to finish.
     */

    private suspend fun ensureSyncedFromCloud() = cloudSyncMutex.withLock {
        if (hasSyncedThisSession) return@withLock
        val uid = authManager.currentUser.value?.uid ?: return@withLock
        hasSyncedThisSession = true
        syncFromCloud(uid)
        pokedex.pullAndMerge(uid, replaceLocal = false)
        dexDaily.sync(replaceLocal = false)
    }

    fun onAppLaunch() {
        viewModelScope.launch {
            // Firebase restores its session asynchronously; don't award against a half-known account.
            withTimeoutOrNull(SESSION_WAIT_MS) { authManager.sessionResolved.first { it } }
            ensureSyncedFromCloud()
            syncProgression()
            val result = xpManager.awardDailyXP()
            result?.let { _xpEvent.emit(it) }
        }
    }

    /**
     * Merges the cloud wallet into this device and pays any level rewards not yet claimed
     * (which is how players who levelled up before Dex Coins existed get their back-pay).
     */
    private suspend fun syncProgression() {
        val uid = authManager.currentUser.value?.uid ?: return
        // Read the stored profile, not the StateFlow, which can lag a save made a moment ago (right after sign-in).
        val profile = repository.profileFlow.first()
        if (profile.isGuest) return
        progression.pullAndMerge(uid)
        progression.claimLevelRewards(profile.level)
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
            repository.saveProfile(
                cloudProfile.copy(
                    username = if (local.username.isNotBlank() && local.username != "Trainer")
                        local.username
                    else
                        cloudProfile.username,
                    photoUrl = local.photoUrl.ifEmpty { cloudProfile.photoUrl },
                    favoriteType = cloudProfile.favoriteType.ifEmpty { local.favoriteType },
                    playStyle = cloudProfile.playStyle.ifEmpty { local.playStyle },
                    starter = cloudProfile.starter.ifEmpty { local.starter },
                ).withPaidGatesFrom(local)
            )
        } else {
            // This phone is ahead, but it may still be behind on "already paid today" marks another
            // device set; adopt those before publishing so they can't be rolled back.
            val merged = local.withPaidGatesFrom(cloudProfile)
            repository.saveProfile(merged)
            repository.syncToFirestore(merged)
        }
    }

    // ── Username update, explicit user action, always wins ───────────────────
    fun updateUsername(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val updated = userProfile.value.copy(username = newName.trim())
            repository.saveProfile(updated)
            if (!updated.isGuest) repository.syncToFirestore(updated)
        }
    }

    // Unlike username, a blank bio is valid, it's how a user clears it.
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

    /** Changes the partner Pokémon: [partnerId] is a starter id or "dexId:name:type" (see Partner). */
    fun setPartner(partnerId: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(starter = partnerId)
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

                // An account that already exists in the cloud always comes back exactly as the player left it.
                // Whatever this phone built as a guest (name, photo, starter, XP) must never leak into it.
                // Only a brand-new account adopts the guest's progress.
                val merged = if (cloudProfile != null) {
                    cloudProfile.copy(
                        uid = uid,
                        isGuest = false,
                        username = cloudProfile.username.takeIf { it.isCustomUsername() }
                            ?: result.user.displayName ?: cloudProfile.username,
                        photoUrl = cloudProfile.photoUrl.ifEmpty { result.user.photoUrl?.toString() ?: "" },
                        starter = cloudProfile.starter.ifEmpty { "pikachu" }
                    )
                } else {
                    local.copy(
                        uid = uid,
                        username = result.user.displayName ?: local.username,
                        photoUrl = result.user.photoUrl?.toString() ?: "",
                        isGuest = false
                    )
                }
                repository.saveProfile(merged)
                repository.syncToFirestore(merged)
                syncProgression()
                // Existing account: it comes back with its own Pokédex and puzzle history, not the guest's.
                // New account: it adopts what this phone built and publishes it.
                pokedex.pullAndMerge(uid, replaceLocal = cloudProfile != null)
                dexDaily.sync(replaceLocal = cloudProfile != null)
                if (cloudProfile != null) {
                    Starter.fromId(merged.starter)?.let { pokedex.chooseStarter(it.dexId) }
                }

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
            progression.clearLocal()
            pokedex.clearLocal()
            challenges.clearLocal()
            dexDaily.clearLocal()
        }
    }

    fun uploadAndSetPhoto(context: Context, uri: Uri) {
        val uid = authManager.currentUser.value?.uid ?: return
        viewModelScope.launch {
            _photoUploading.value = true
            try {
                val ref = FirebaseStorage.getInstance()
                    .reference.child("profile_photos/$uid.jpg")
                val bytes = ProfilePhotoEncoder.encode(context, uri) ?: return@launch
                // Explicit content type: the Storage rule only accepts images.
                ref.putBytes(bytes, storageMetadata { contentType = "image/jpeg" }).await()
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

private const val SESSION_WAIT_MS = 4_000L

/**
 * Keeps every "already paid" mark at its most recent value across two copies of the same account.
 * Dates are yyyy-MM-dd, so string order is date order. A rolled-back mark would let a reward be
 * claimed a second time, which is the one thing this merge must never allow.
 */
private fun UserProfile.withPaidGatesFrom(o: UserProfile): UserProfile {
    fun later(a: String, b: String) = if (a >= b) a else b
    val otherDailyIsNewer = o.lastDailyXpDate > lastDailyXpDate
    // Same day on both sides: keep the larger tally so a game's daily rate tier can't reset either.
    val gameXpDate = later(dailyGameXpDate, o.dailyGameXpDate)
    val gameXp = when {
        dailyGameXpDate == o.dailyGameXpDate -> maxOf(dailyGameXp, o.dailyGameXp)
        dailyGameXpDate > o.dailyGameXpDate -> dailyGameXp
        else -> o.dailyGameXp
    }
    return copy(
        lastDailyXpDate = later(lastDailyXpDate, o.lastDailyXpDate),
        dailyStreak = if (otherDailyIsNewer) o.dailyStreak else dailyStreak,
        lastFirstGameXpDate = later(lastFirstGameXpDate, o.lastFirstGameXpDate),
        lastExplorationXpDate = later(lastExplorationXpDate, o.lastExplorationXpDate),
        lastEasterEggXpDate = later(lastEasterEggXpDate, o.lastEasterEggXpDate),
        lastDailyFactXpDate = later(lastDailyFactXpDate, o.lastDailyFactXpDate),
        lastDexDailyXpDate = later(lastDexDailyXpDate, o.lastDexDailyXpDate),
        feedbackThanksClaimed = feedbackThanksClaimed || o.feedbackThanksClaimed,
        dailyGameXpDate = gameXpDate,
        dailyGameXp = gameXp,
    )
}

