package com.aditya1875.pokeverse.domain.xp

import com.aditya1875.pokeverse.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.data.remote.model.LevelConfig
import com.aditya1875.pokeverse.data.remote.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class XPManager(
    private val repository: UserProfileRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun awardDailyXP(): XPResult? {
        val profile = repository.profileFlow.first()
        val today = dateFormat.format(Date())

        // Already claimed today
        if (profile.lastDailyXpDate == today) return null

        val yesterday = dateFormat.format(Date(System.currentTimeMillis() - 86_400_000L))
        val newStreak = if (profile.lastDailyXpDate == yesterday) profile.dailyStreak + 1 else 1
        val streakBonus = minOf((newStreak - 1) * XPValues.DAILY_STREAK_BONUS, 50)
        val totalGained = XPValues.DAILY_LOGIN + streakBonus

        val label = if (streakBonus > 0)
            "Daily Showup +${XPValues.DAILY_LOGIN} XP  🔥 Streak Bonus +$streakBonus XP"
        else
            "Daily Showup +${XPValues.DAILY_LOGIN} XP"

        return applyXP(profile, totalGained, label) { updated ->
            updated.copy(
                lastDailyXpDate = today,
                dailyStreak = newStreak,
                lastActiveDateMillis = System.currentTimeMillis()
            )
        }
    }

    suspend fun awardGameXP(event: XPEvent): XPResult {
        val profile = repository.profileFlow.first()
        val today = dateFormat.format(Date())

        val (gained, label) = when (event) {
            is XPEvent.QuizAnswer -> {
                if (event.correct) XPValues.QUIZ_CORRECT to "Correct Answer +${XPValues.QUIZ_CORRECT} XP"
                else 0 to ""
            }
            is XPEvent.QuizComplete -> {
                val perfect = event.score == event.total && event.total > 0
                val bonus = if (perfect) XPValues.QUIZ_PERFECT else 0
                val total = XPValues.QUIZ_COMPLETE + bonus
                val lbl = if (perfect) "Quiz Complete +$total XP ⭐ Perfect!" else "Quiz Complete +$total XP"
                total to lbl
            }
            is XPEvent.MatchComplete -> {
                val underPar = event.moves <= event.par
                val bonus = if (underPar) XPValues.MATCH_UNDER_PAR else 0
                val total = XPValues.MATCH_COMPLETE + bonus
                val lbl = if (underPar) "Match Complete +$total XP 🏆 Under Par!" else "Match Complete +$total XP"
                total to lbl
            }
            is XPEvent.GuessCorrect -> {
                val streakBonus = when {
                    event.streak >= 5 -> XPValues.GUESS_STREAK_5
                    event.streak >= 2 -> XPValues.GUESS_STREAK_2
                    else -> 0
                }
                val total = XPValues.GUESS_CORRECT + streakBonus
                val lbl = if (streakBonus > 0) "Correct Guess +$total XP 🔥 x ${event.streak}" else "Correct Guess +$total XP"
                total to lbl
            }
            is XPEvent.GuessComplete -> XPValues.GUESS_COMPLETE to "Round Complete +${XPValues.GUESS_COMPLETE} XP"
            is XPEvent.FirstGameOfDay -> XPValues.FIRST_GAME_OF_DAY to "First Game Today! +${XPValues.FIRST_GAME_OF_DAY} XP 🎮"
            is XPEvent.DailyLogin -> XPValues.DAILY_LOGIN to "Showed up today! + ${XPValues.DAILY_LOGIN} XP"
        }

        if (gained == 0) return noOpResult(profile)

        val isFirstGame = profile.lastDailyXpDate != today || profile.gamesPlayed == 0
        val firstGameBonus = if (isFirstGame && event !is XPEvent.FirstGameOfDay) XPValues.FIRST_GAME_OF_DAY else 0
        val finalGained = gained + firstGameBonus
        val finalLabel = if (firstGameBonus > 0) "$label  +$firstGameBonus First Game Bonus" else label

        return applyXP(profile, finalGained, finalLabel) { updated ->
            updated.copy(gamesPlayed = updated.gamesPlayed + 1)
        }
    }

    private suspend fun applyXP(
        profile: UserProfile,
        gained: Int,
        label: String,
        extraUpdate: (UserProfile) -> UserProfile = { it }
    ): XPResult {
        val newTotal = profile.totalXp + gained
        val (newLevel, newCurrent, newNext) = LevelConfig.computeLevel(newTotal)
        val leveledUp = newLevel > profile.level

        val updated = extraUpdate(
            profile.copy(
                totalXp = newTotal,
                currentXp = newCurrent,
                nextLevelXp = newNext,
                level = newLevel
            )
        )

        repository.saveProfile(updated)

        if (!updated.isGuest) {
            scope.launch { repository.syncToFirestore(updated) }
        }

        if (profile.isGuest) {
            return noOpResult(profile)
        }

        return XPResult(
            xpGained = gained,
            newTotalXp = newTotal,
            newLevel = newLevel,
            newCurrentXp = newCurrent,
            newNextLevelXp = newNext,
            leveledUp = leveledUp,
            label = label
        )
    }

    private fun noOpResult(profile: UserProfile) = XPResult(
        xpGained = 0,
        newTotalXp = profile.totalXp,
        newLevel = profile.level,
        newCurrentXp = profile.currentXp,
        newNextLevelXp = profile.nextLevelXp,
        leveledUp = false,
        label = ""
    )
}