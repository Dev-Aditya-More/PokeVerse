package com.aditya1875.pokeverse.data.firebase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aditya1875.pokeverse.data.remote.model.LevelConfig
import com.aditya1875.pokeverse.data.remote.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.profileDataStore: DataStore<Preferences>
        by preferencesDataStore("user_profile")

class UserProfileRepository(private val context: Context) {

    private val ds = context.profileDataStore
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private object K {
        val UID = stringPreferencesKey("uid")
        val USERNAME = stringPreferencesKey("username")
        val TOTAL_XP = intPreferencesKey("total_xp")
        val GAMES_PLAYED = intPreferencesKey("games_played")
        val BEST_QUIZ = intPreferencesKey("best_quiz")
        val BEST_MATCH = intPreferencesKey("best_match")
        val BEST_GUESS = intPreferencesKey("best_guess")
        val BEST_TYPERUSH = intPreferencesKey("best_typerush")
        val IS_GUEST = booleanPreferencesKey("is_guest")
        val LAST_DAILY_DATE = stringPreferencesKey("last_daily_date")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val LAST_ACTIVE_MS = longPreferencesKey("last_active_ms")
        val PHOTO_URL = stringPreferencesKey("photo_url")
        val RANK = intPreferencesKey("rank")
        val EMAIL = stringPreferencesKey("email")
    }

    val profileFlow: Flow<UserProfile> = ds.data.map { p ->
        val totalXp = p[K.TOTAL_XP] ?: 0
        val (level, currentXp, nextLevelXp) = LevelConfig.computeLevel(totalXp)
        UserProfile(
            uid = p[K.UID] ?: "guest",
            username = p[K.USERNAME] ?: "Trainer",
            totalXp = totalXp,
            level = level,
            currentXp = currentXp,
            nextLevelXp = nextLevelXp,
            gamesPlayed = p[K.GAMES_PLAYED] ?: 0,
            bestQuizScore = p[K.BEST_QUIZ] ?: 0,
            bestMatchScore = p[K.BEST_MATCH] ?: 0,
            bestGuessScore = p[K.BEST_GUESS] ?: 0,
            bestTypeRushScore = p[K.BEST_TYPERUSH] ?: 0,
            isGuest = p[K.IS_GUEST] ?: true,
            lastDailyXpDate = p[K.LAST_DAILY_DATE] ?: "",
            dailyStreak = p[K.DAILY_STREAK] ?: 0,
            lastActiveDateMillis = p[K.LAST_ACTIVE_MS] ?: 0L,
            photoUrl = p[K.PHOTO_URL] ?: "",
            rank = p[K.RANK] ?: 0,
            email = p[K.EMAIL] ?: "",
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        ds.edit { p ->
            p[K.UID] = profile.uid
            p[K.USERNAME] = profile.username
            p[K.TOTAL_XP] = profile.totalXp
            p[K.GAMES_PLAYED] = profile.gamesPlayed
            p[K.BEST_QUIZ] = profile.bestQuizScore
            p[K.BEST_MATCH] = profile.bestMatchScore
            p[K.BEST_GUESS] = profile.bestGuessScore
            p[K.BEST_TYPERUSH] = profile.bestTypeRushScore
            p[K.IS_GUEST] = profile.isGuest
            p[K.LAST_DAILY_DATE] = profile.lastDailyXpDate
            p[K.DAILY_STREAK] = profile.dailyStreak
            p[K.LAST_ACTIVE_MS] = profile.lastActiveDateMillis
            p[K.PHOTO_URL] = profile.photoUrl
            p[K.RANK] = profile.rank
            p[K.EMAIL] = profile.email
        }
    }

    suspend fun loadFromFirestore(uid: String): UserProfile? {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (!doc.exists()) return null
            val totalXp = (doc.getLong("totalXp") ?: 0L).toInt()
            val (level, currentXp, nextLevelXp) = LevelConfig.computeLevel(totalXp)
            UserProfile(
                uid = uid,
                username = doc.getString("username") ?: "Trainer",
                totalXp = totalXp,
                level = level,
                currentXp = currentXp,
                nextLevelXp = nextLevelXp,
                gamesPlayed = (doc.getLong("gamesPlayed") ?: 0L).toInt(),
                bestQuizScore = (doc.getLong("bestQuizScore") ?: 0L).toInt(),
                bestMatchScore = (doc.getLong("bestMatchScore") ?: 0L).toInt(),
                bestGuessScore = (doc.getLong("bestGuessScore") ?: 0L).toInt(),
                bestTypeRushScore = (doc.getLong("bestTypeRushScore") ?: 0L).toInt(),
                isGuest = false,
                lastDailyXpDate = doc.getString("lastDailyXpDate") ?: "",
                dailyStreak = (doc.getLong("dailyStreak") ?: 0L).toInt(),
                lastActiveDateMillis = doc.getLong("lastActiveDateMs") ?: 0L,
                photoUrl = doc.getString("photoUrl") ?: "",
                rank = (doc.getLong("rank") ?: 0L).toInt(),
                email = doc.getString("email") ?: "",
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncToFirestore(profile: UserProfile) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "username" to profile.username,
                    "photoUrl" to profile.photoUrl,
                    "email" to profile.email,
                    "totalXp" to profile.totalXp,
                    "level" to profile.level,
                    "gamesPlayed" to profile.gamesPlayed,
                    "bestQuizScore" to profile.bestQuizScore,
                    "bestMatchScore" to profile.bestMatchScore,
                    "bestGuessScore" to profile.bestGuessScore,
                    "bestTypeRushScore" to profile.bestTypeRushScore,
                    "dailyStreak" to profile.dailyStreak,
                    "lastDailyXpDate" to profile.lastDailyXpDate,
                    "lastActiveDateMs" to profile.lastActiveDateMillis,
                    "updatedAt" to Timestamp.now(),
                ),
                SetOptions.merge()
            ).await()

            firestore.collection("leaderboard").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "displayName" to profile.username,
                    "photoUrl" to profile.photoUrl,
                    "totalXp" to profile.totalXp,
                    "level" to profile.level,
                    "updatedAt" to Timestamp.now(),
                ),
                SetOptions.merge()
            ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun updateBestScore(game: String, score: Int) {
        if (score <= 0) return

        var didUpdate = false
        ds.edit { p ->
            val key = when (game) {
                "quiz" -> K.BEST_QUIZ
                "match" -> K.BEST_MATCH
                "guess" -> K.BEST_GUESS
                "typerush" -> K.BEST_TYPERUSH
                else -> return@edit
            }
            if (score > (p[key] ?: 0)) {
                p[key] = score
                didUpdate = true
            }
        }

        if (!didUpdate) return

        val uid = auth.currentUser?.uid ?: return
        val field = when (game) {
            "quiz" -> "bestQuizScore"
            "match" -> "bestMatchScore"
            "guess" -> "bestGuessScore"
            "typerush" -> "bestTypeRushScore"
            else -> return
        }
        try {
            firestore.collection("users").document(uid)
                .set(mapOf(field to score), SetOptions.merge()).await()
        } catch (_: Exception) {
        }
    }

    suspend fun incrementGamesPlayed() {
        var newCount = 0
        ds.edit { p ->
            newCount = (p[K.GAMES_PLAYED] ?: 0) + 1
            p[K.GAMES_PLAYED] = newCount
        }
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid)
                .set(mapOf("gamesPlayed" to newCount), SetOptions.merge()).await()
        } catch (_: Exception) {
        }
    }

    suspend fun clearLocal() {
        ds.edit { it.clear() }
    }
}