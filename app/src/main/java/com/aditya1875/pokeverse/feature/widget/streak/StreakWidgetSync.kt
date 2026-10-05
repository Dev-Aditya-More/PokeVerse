package com.aditya1875.pokeverse.feature.widget.streak

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the streak widget current while the app is running. It redraws only when
 * a field the widget actually shows changes, so normal XP gains mid-level cost
 * one cheap update and unrelated profile writes cost nothing.
 *
 * Date-driven changes (midnight, "hours left") are covered by the provider's
 * updatePeriodMillis while the app is closed.
 */
object StreakWidgetSync {

    private data class Shown(
        val lastDailyXpDate: String,
        val dailyStreak: Int,
        val hasRested: Boolean
    )

    fun start(context: Context, repository: UserProfileRepository) {
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            repository.profileFlow
                .map {
                    Shown(
                        lastDailyXpDate = it.lastDailyXpDate,
                        dailyStreak = it.dailyStreak,
                        hasRested = it.restedXp > 0
                    )
                }
                .distinctUntilChanged()
                .catch { Log.w(TAG, "Profile flow failed; widget will refresh on its schedule", it) }
                .collect {
                    runCatching { StreakWidget().updateAll(appContext) }
                        .onFailure { e -> Log.w(TAG, "Widget update failed", e) }
                }
        }
    }

    private const val TAG = "StreakWidgetSync"
}
