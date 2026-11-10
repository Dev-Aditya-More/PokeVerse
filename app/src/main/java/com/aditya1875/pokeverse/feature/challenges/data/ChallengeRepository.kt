package com.aditya1875.pokeverse.feature.challenges.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeProgress
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.challengeStore: DataStore<Preferences> by preferencesDataStore("challenges")

/**
 * Today's challenge counters. They live on the device and reset with the date: a daily challenge is about
 * today's play, and what gets paid is guarded separately by the wallet's claim ids, so nothing here can pay twice.
 */
class ChallengeRepository(context: Context) {

    private val ds = context.applicationContext.challengeStore

    private object K {
        val DAY = stringPreferencesKey("day")
        val CATCHES = intPreferencesKey("catches")
        val NEW = intPreferencesKey("new_species")
        val STREAK = intPreferencesKey("best_streak")
        fun tier(t: CatchTier) = intPreferencesKey("tier_${t.ordinal}")
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Today's counters; the Pokédex half of [ChallengeProgress] is filled in by the caller. */
    val today: Flow<ChallengeProgress> = ds.data.map { p ->
        val day = today()
        if (p[K.DAY] != day) ChallengeProgress(day = day)
        else ChallengeProgress(
            day = day,
            catches = p[K.CATCHES] ?: 0,
            newSpecies = p[K.NEW] ?: 0,
            bestStreak = p[K.STREAK] ?: 0,
            tierCatches = CatchTier.entries.associateWith { p[K.tier(it)] ?: 0 }.filterValues { it > 0 },
        )
    }

    /** A catch just happened. [streak] is the run of catches in a row including this one. */
    suspend fun recordCatch(tier: CatchTier, isNewSpecies: Boolean, streak: Int) {
        ds.edit { p ->
            val day = today()
            if (p[K.DAY] != day) {
                p.clear()
                p[K.DAY] = day
            }
            p[K.CATCHES] = (p[K.CATCHES] ?: 0) + 1
            if (isNewSpecies) p[K.NEW] = (p[K.NEW] ?: 0) + 1
            p[K.tier(tier)] = (p[K.tier(tier)] ?: 0) + 1
            p[K.STREAK] = maxOf(p[K.STREAK] ?: 0, streak)
        }
    }

    suspend fun clearLocal() {
        ds.edit { it.clear() }
    }
}
