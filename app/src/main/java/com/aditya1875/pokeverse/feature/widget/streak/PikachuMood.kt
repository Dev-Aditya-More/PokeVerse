package com.aditya1875.pokeverse.feature.widget.streak

/** How Pikachu feels about your streak right now — drives the art, card colour and speech bubble. */
enum class PikachuMood { Happy, Waiting, Panicking, Sleeping }

fun StreakStatus.mood(): PikachuMood = when (this) {
    is StreakStatus.SafeToday -> PikachuMood.Happy
    is StreakStatus.AtRisk -> if (isUrgent) PikachuMood.Panicking else PikachuMood.Waiting
    StreakStatus.NotStarted -> PikachuMood.Sleeping
}

/** Days of streak currently on the line (0 when there isn't one). */
fun StreakStatus.days(): Int = when (this) {
    is StreakStatus.SafeToday -> days
    is StreakStatus.AtRisk -> days
    StreakStatus.NotStarted -> 0
}

/**
 * Costumes Pikachu unlocks as the streak grows. Losing the streak takes the
 * outfit away — the reason the widget is worth glancing at every day.
 * Ordered by [minDays].
 */
enum class PikachuLook(val minDays: Int) {
    Classic(0),
    Cap(3),
    PopStar(7),
    Libre(14),
    PhD(30),
    RockStar(60),
    Belle(100);

    companion object {
        fun forStreak(days: Int): PikachuLook = entries.last { days >= it.minDays }

        /** The next outfit and how many more days it takes, or null once everything is unlocked. */
        fun next(days: Int): Pair<PikachuLook, Int>? =
            entries.firstOrNull { it.minDays > days }?.let { it to (it.minDays - days) }
    }
}
