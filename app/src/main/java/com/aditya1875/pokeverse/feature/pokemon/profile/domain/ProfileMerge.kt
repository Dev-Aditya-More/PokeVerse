package com.aditya1875.pokeverse.feature.pokemon.profile.domain

import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.LevelConfig
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile

/**
 * Combines two copies of the same account (this phone and the cloud, or two phones) so that progress can only
 * go up. A phone that is behind (restored from an old backup, started offline, a second device) must never
 * pull the account's XP, best scores or counters down when it syncs, which is how players used to lose data.
 *
 * - Counters and best scores: the larger value.
 * - "Already paid on this date" gates: the later date, so no reward is paid twice.
 * - Weekly XP: the newer week wins; the same week keeps the larger tally.
 * - Spendable or resettable values (rested XP, streak): from the copy that is further along.
 * - Name, partner and photo: this copy's, unless it is still a placeholder and the other has a real one.
 * - Bio, favourite type, play style: always this copy's (an empty one may have been cleared on purpose).
 *
 * Pure, so the rules are unit tested.
 */
fun UserProfile.progressMergedWith(other: UserProfile): UserProfile {
    fun later(a: String, b: String) = if (a >= b) a else b
    fun isPlaceholderName(n: String) = n.isBlank() || n == "Trainer" || n == "Guest Trainer"

    val ahead = if (other.totalXp > totalXp) other else this
    val totalXp = maxOf(totalXp, other.totalXp)
    val (level, currentXp, nextLevelXp) = LevelConfig.computeLevel(totalXp)

    val weekly = when {
        lastWeeklyReset == other.lastWeeklyReset -> maxOf(weeklyXp, other.weeklyXp) to lastWeeklyReset
        other.lastWeeklyReset > lastWeeklyReset -> other.weeklyXp to other.lastWeeklyReset
        else -> weeklyXp to lastWeeklyReset
    }
    val gameXp = when {
        dailyGameXpDate == other.dailyGameXpDate -> maxOf(dailyGameXp, other.dailyGameXp)
        dailyGameXpDate > other.dailyGameXpDate -> dailyGameXp
        else -> other.dailyGameXp
    }

    return copy(
        username = if (isPlaceholderName(username) && !isPlaceholderName(other.username)) other.username else username,
        // Bio, favourite type and play style stay this copy's: an empty one may have been cleared on purpose.
        starter = starter.ifEmpty { other.starter },
        photoUrl = photoUrl.ifEmpty { other.photoUrl },
        email = email.ifEmpty { other.email },

        totalXp = totalXp,
        level = level,
        currentXp = currentXp,
        nextLevelXp = nextLevelXp,
        weeklyXp = weekly.first,
        lastWeeklyReset = weekly.second,

        gamesPlayed = maxOf(gamesPlayed, other.gamesPlayed),
        battlesPlayed = maxOf(battlesPlayed, other.battlesPlayed),
        bestQuizScore = maxOf(bestQuizScore, other.bestQuizScore),
        bestMatchScore = maxOf(bestMatchScore, other.bestMatchScore),
        bestGuessScore = maxOf(bestGuessScore, other.bestGuessScore),
        bestTypeRushScore = maxOf(bestTypeRushScore, other.bestTypeRushScore),
        bestDuelScore = maxOf(bestDuelScore, other.bestDuelScore),
        bestWildCatchScore = maxOf(bestWildCatchScore, other.bestWildCatchScore),
        bestSurvivorScore = maxOf(bestSurvivorScore, other.bestSurvivorScore),
        bestChaseScore = maxOf(bestChaseScore, other.bestChaseScore),
        bestBattleScore = maxOf(bestBattleScore, other.bestBattleScore),
        duelWins = maxOf(duelWins, other.duelWins),
        duelLosses = maxOf(duelLosses, other.duelLosses),

        lastDailyXpDate = later(lastDailyXpDate, other.lastDailyXpDate),
        dailyStreak = if (other.lastDailyXpDate > lastDailyXpDate) other.dailyStreak else dailyStreak,
        lastFirstGameXpDate = later(lastFirstGameXpDate, other.lastFirstGameXpDate),
        lastExplorationXpDate = later(lastExplorationXpDate, other.lastExplorationXpDate),
        lastEasterEggXpDate = later(lastEasterEggXpDate, other.lastEasterEggXpDate),
        lastDailyFactXpDate = later(lastDailyFactXpDate, other.lastDailyFactXpDate),
        lastDexDailyXpDate = later(lastDexDailyXpDate, other.lastDexDailyXpDate),
        lastDuelDate = later(lastDuelDate, other.lastDuelDate),
        feedbackThanksClaimed = feedbackThanksClaimed || other.feedbackThanksClaimed,
        dailyGameXpDate = later(dailyGameXpDate, other.dailyGameXpDate),
        dailyGameXp = gameXp,

        restedXp = ahead.restedXp,
        lastActiveDateMillis = maxOf(lastActiveDateMillis, other.lastActiveDateMillis),
        duelPoints = ahead.duelPoints,
        duelStreak = ahead.duelStreak,
    )
}
