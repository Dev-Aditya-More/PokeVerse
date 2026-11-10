package com.aditya1875.pokeverse.feature.game.battle.data

import android.content.Context
import java.time.LocalDate

/**
 * Remembers, on this phone, how many online battles each rival has already paid out today, so two accounts
 * can't trade wins for rewards all day. The limit itself is [com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineBattleRules.REWARDED_PER_RIVAL_PER_DAY].
 */
class OnlineRewardLedger(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("online_battle_ledger", Context.MODE_PRIVATE)

    private fun today() = LocalDate.now().toString()

    /** How many battles against [rivalUid] have paid out today. */
    fun paidToday(rivalUid: String): Int =
        if (prefs.getString(KEY_DAY, null) == today()) prefs.getInt(rivalKey(rivalUid), 0) else 0

    fun recordPaid(rivalUid: String) {
        val day = today()
        val edit = prefs.edit()
        if (prefs.getString(KEY_DAY, null) != day) {
            // A new day: yesterday's counts are no longer needed.
            edit.clear().putString(KEY_DAY, day)
            edit.putInt(rivalKey(rivalUid), 1)
        } else {
            edit.putInt(rivalKey(rivalUid), prefs.getInt(rivalKey(rivalUid), 0) + 1)
        }
        edit.apply()
    }

    private fun rivalKey(uid: String) = "rival_$uid"

    private companion object {
        const val KEY_DAY = "day"
    }
}
