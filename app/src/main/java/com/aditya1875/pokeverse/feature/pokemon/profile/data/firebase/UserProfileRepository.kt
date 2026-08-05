package com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.LevelConfig
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import com.aditya1875.pokeverse.utils.WeeklyReset
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

        val WEEKLY_XP = intPreferencesKey("weekly_xp")

        val LAST_WEEKLY_RESET = longPreferencesKey("last_weekly_reset")

        val GAMES_PLAYED = intPreferencesKey("games_played")
        val BEST_QUIZ = intPreferencesKey("best_quiz")
        val BEST_MATCH = intPreferencesKey("best_match")
        val BEST_GUESS = intPreferencesKey("best_guess")
        val BEST_TYPERUSH = intPreferencesKey("best_typerush")
        val BEST_DUEL = intPreferencesKey("best_duel")
        val BEST_WILDCATCH = intPreferencesKey("best_wildcatch")
        val IS_GUEST = booleanPreferencesKey("is_guest")
        val LAST_DAILY_DATE = stringPreferencesKey("last_daily_date")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val LAST_EXPLORATION_DATE = stringPreferencesKey("last_exploration_date")
        val LAST_FIRST_GAME_DATE = stringPreferencesKey("last_first_game_date")
        val LAST_EASTER_EGG_DATE = stringPreferencesKey("last_easter_egg_date")
        val LAST_ACTIVE_MS = longPreferencesKey("last_active_ms")
        val PHOTO_URL = stringPreferencesKey("photo_url")

        val RANK = intPreferencesKey("rank")
        val EMAIL = stringPreferencesKey("email")

        val DUEL_POINTS = intPreferencesKey("duel_points")
        val DUEL_WINS = intPreferencesKey("duel_wins")
        val DUEL_LOSSES = intPreferencesKey("duel_losses")
        val DUEL_STREAK = intPreferencesKey("duel_streak")
        val LAST_DUEL_DATE = stringPreferencesKey("last_duel_date")
    }

    val profileFlow: Flow<UserProfile> = ds.data.map { p ->
        val totalXp = p[K.TOTAL_XP] ?: 0
        val (level, currentXp, nextLevelXp) = LevelConfig.computeLevel(totalXp)
        val lastWeeklyReset = p[K.LAST_WEEKLY_RESET] ?: 0L
        UserProfile(
            uid = p[K.UID] ?: "guest",
            username = p[K.USERNAME] ?: "Trainer",
            totalXp = totalXp,
            // Weekly XP from before the Monday 00:00 IST boundary belongs to last week
            weeklyXp = if (WeeklyReset.isStale(lastWeeklyReset)) 0 else (p[K.WEEKLY_XP] ?: 0),
            lastWeeklyReset = lastWeeklyReset,
            level = level,
            currentXp = currentXp,
            nextLevelXp = nextLevelXp,
            gamesPlayed = p[K.GAMES_PLAYED] ?: 0,
            bestQuizScore = p[K.BEST_QUIZ] ?: 0,
            lastExplorationXpDate = p[K.LAST_EXPLORATION_DATE] ?: "",
            bestMatchScore = p[K.BEST_MATCH] ?: 0,
            bestGuessScore = p[K.BEST_GUESS] ?: 0,
            bestTypeRushScore = p[K.BEST_TYPERUSH] ?: 0,
            bestDuelScore = p[K.BEST_DUEL] ?: 0,
            bestWildCatchScore = p[K.BEST_WILDCATCH] ?: 0,
            isGuest = p[K.IS_GUEST] ?: true,
            lastDailyXpDate = p[K.LAST_DAILY_DATE] ?: "",
            lastFirstGameXpDate = p[K.LAST_FIRST_GAME_DATE] ?: "",
            lastEasterEggXpDate = p[K.LAST_EASTER_EGG_DATE] ?: "",
            dailyStreak = p[K.DAILY_STREAK] ?: 0,
            lastActiveDateMillis = p[K.LAST_ACTIVE_MS] ?: 0L,
            photoUrl = p[K.PHOTO_URL] ?: "",
            rank = p[K.RANK] ?: 0,
            email = p[K.EMAIL] ?: "",
            duelPoints = p[K.DUEL_POINTS] ?: 1000,
            duelWins = p[K.DUEL_WINS] ?: 0,
            duelLosses = p[K.DUEL_LOSSES] ?: 0,
            duelStreak = p[K.DUEL_STREAK] ?: 0,
            lastDuelDate = p[K.LAST_DUEL_DATE] ?: "",
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        ds.edit { p ->
            p[K.UID] = profile.uid
            p[K.USERNAME] = profile.username
            p[K.TOTAL_XP] = profile.totalXp
            p[K.WEEKLY_XP] = profile.weeklyXp
            p[K.LAST_WEEKLY_RESET] = profile.lastWeeklyReset
            p[K.GAMES_PLAYED] = profile.gamesPlayed
            p[K.BEST_QUIZ] = profile.bestQuizScore
            p[K.BEST_MATCH] = profile.bestMatchScore
            p[K.BEST_GUESS] = profile.bestGuessScore
            p[K.BEST_TYPERUSH] = profile.bestTypeRushScore
            p[K.BEST_DUEL] = profile.bestDuelScore
            p[K.BEST_WILDCATCH] = profile.bestWildCatchScore
            p[K.IS_GUEST] = profile.isGuest
            p[K.LAST_EXPLORATION_DATE] = profile.lastExplorationXpDate
            p[K.LAST_FIRST_GAME_DATE] = profile.lastFirstGameXpDate
            p[K.LAST_EASTER_EGG_DATE] = profile.lastEasterEggXpDate
            p[K.LAST_DAILY_DATE] = profile.lastDailyXpDate
            p[K.DAILY_STREAK] = profile.dailyStreak
            p[K.LAST_ACTIVE_MS] = profile.lastActiveDateMillis
            p[K.PHOTO_URL] = profile.photoUrl
            p[K.RANK] = profile.rank
            p[K.EMAIL] = profile.email
            p[K.DUEL_POINTS] = profile.duelPoints
            p[K.DUEL_WINS] = profile.duelWins
            p[K.DUEL_LOSSES] = profile.duelLosses
            p[K.DUEL_STREAK] = profile.duelStreak
            p[K.LAST_DUEL_DATE] = profile.lastDuelDate
        }
    }

    suspend fun loadFromFirestore(uid: String): UserProfile? {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (!doc.exists()) return null
            val totalXp = (doc.getLong("totalXp") ?: 0L).toInt()
            val (level, currentXp, nextLevelXp) = LevelConfig.computeLevel(totalXp)
            // Cloud Function writes Timestamps; the app writes Longs — handle both
            val lastWeeklyReset = when (val raw = doc.get("lastWeeklyReset")) {
                is Long -> raw
                is Number -> raw.toLong()
                is Timestamp -> raw.toDate().time
                else -> 0L
            }
            UserProfile(
                uid = uid,
                username = doc.getString("username") ?: "Trainer",
                totalXp = totalXp,
                weeklyXp = if (WeeklyReset.isStale(lastWeeklyReset)) 0
                           else (doc.getLong("weeklyXp") ?: 0L).toInt(),
                lastWeeklyReset = lastWeeklyReset,
                level = level,
                currentXp = currentXp,
                nextLevelXp = nextLevelXp,
                gamesPlayed = (doc.getLong("gamesPlayed") ?: 0L).toInt(),
                bestQuizScore = (doc.getLong("bestQuizScore") ?: 0L).toInt(),
                bestMatchScore = (doc.getLong("bestMatchScore") ?: 0L).toInt(),
                bestGuessScore = (doc.getLong("bestGuessScore") ?: 0L).toInt(),
                bestTypeRushScore = (doc.getLong("bestTypeRushScore") ?: 0L).toInt(),
                bestDuelScore = (doc.getLong("bestDuelScore") ?: 0L).toInt(),
                bestWildCatchScore = (doc.getLong("bestWildCatchScore") ?: 0L).toInt(),
                isGuest = false,
                lastDailyXpDate = doc.getString("lastDailyXpDate") ?: "",
                lastExplorationXpDate = doc.getString("lastExplorationXpDate") ?: "",
                lastFirstGameXpDate = doc.getString("lastFirstGameXpDate") ?: "",
                lastEasterEggXpDate = doc.getString("lastEasterEggXpDate") ?: "",
                dailyStreak = (doc.getLong("dailyStreak") ?: 0L).toInt(),
                lastActiveDateMillis = doc.getLong("lastActiveDateMs") ?: 0L,
                photoUrl = doc.getString("photoUrl") ?: "",
                rank = (doc.getLong("rank") ?: 0L).toInt(),
                email = doc.getString("email") ?: "",
                duelPoints = (doc.getLong("duelPoints") ?: 1000L).toInt(),
                duelWins = (doc.getLong("duelWins") ?: 0L).toInt(),
                duelLosses = (doc.getLong("duelLosses") ?: 0L).toInt(),
                duelStreak = (doc.getLong("duelStreak") ?: 0L).toInt(),
                lastDuelDate = doc.getString("lastDuelDate") ?: "",
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncToFirestore(profile: UserProfile) {
        val uid = auth.currentUser?.uid ?: return
        // Never upload weekly XP from before the Monday boundary — it would
        // overwrite the Cloud Function's weekly reset with last week's value.
        val p = if (WeeklyReset.isStale(profile.lastWeeklyReset)) {
            profile.copy(weeklyXp = 0, lastWeeklyReset = WeeklyReset.startOfCurrentWeekMillis())
        } else profile
        try {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "username" to p.username,
                    "photoUrl" to p.photoUrl,
                    "email" to p.email,
                    "totalXp" to p.totalXp,
                    "weeklyXp" to p.weeklyXp,
                    "lastWeeklyReset" to p.lastWeeklyReset,
                    "level" to p.level,
                    "gamesPlayed" to p.gamesPlayed,
                    "bestQuizScore" to p.bestQuizScore,
                    "bestMatchScore" to p.bestMatchScore,
                    "bestGuessScore" to p.bestGuessScore,
                    "bestTypeRushScore" to p.bestTypeRushScore,
                    "bestDuelScore" to p.bestDuelScore,
                    "bestWildCatchScore" to p.bestWildCatchScore,
                    "dailyStreak" to p.dailyStreak,
                    "lastDailyXpDate" to p.lastDailyXpDate,
                    "lastExplorationXpDate" to p.lastExplorationXpDate,
                    "lastFirstGameXpDate" to p.lastFirstGameXpDate,
                    "lastEasterEggXpDate" to p.lastEasterEggXpDate,
                    "lastActiveDateMs" to p.lastActiveDateMillis,
                    "duelPoints" to p.duelPoints,
                    "duelWins" to p.duelWins,
                    "duelLosses" to p.duelLosses,
                    "duelStreak" to p.duelStreak,
                    "lastDuelDate" to p.lastDuelDate,
                    "updatedAt" to Timestamp.now()
                ),
                SetOptions.merge()
            ).await()

            firestore.collection("leaderboard").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "displayName" to p.username,
                    // lowercase copy for case-insensitive friend search (prefix queries)
                    "displayNameLower" to p.username.lowercase(),
                    "photoUrl" to p.photoUrl,
                    "totalXp" to p.totalXp,
                    "weeklyXp" to p.weeklyXp,
                    "lastWeeklyReset" to p.lastWeeklyReset,
                    "level" to p.level,
                    "updatedAt" to Timestamp.now(),
                    "weeklyActive" to (p.weeklyXp > 0),
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
                "duel" -> K.BEST_DUEL
                "wildcatch" -> K.BEST_WILDCATCH
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
            "duel" -> "bestDuelScore"
            "wildcatch" -> "bestWildCatchScore"
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