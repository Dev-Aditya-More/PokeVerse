package com.aditya1875.pokeverse.feature.dexdaily.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

/*
 * Dex Daily: one silhouette puzzle a day, the same for every player on the same date.
 *
 * Everything here is pure Kotlin (no Android, no I/O) so the rules are unit-tested.
 * Dates are "yyyy-MM-dd" strings in the player's local calendar (like Wordle: a new
 * puzzle at local midnight). Locale.US keeps the digits ASCII in every language.
 */

object DexDailyCalendar {
    /**
     * Puzzle #1. Set a month before launch on purpose: the Pro archive opens with a full
     * month of past puzzles to play instead of an empty calendar.
     */
    const val EPOCH = "2026-10-01"
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Arithmetic in UTC so daylight-saving shifts never make a day 23 or 25 hours long. */
    private fun utc() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    fun today(nowMs: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.format(Date(nowMs))

    /** 0 for [EPOCH], negative before it. */
    fun dayIndex(date: String): Int =
        ((utc().parse(date)!!.time - utc().parse(EPOCH)!!.time) / DAY_MS).toInt()

    fun number(date: String): Int = dayIndex(date) + 1

    fun addDays(date: String, days: Int): String =
        utc().run { format(Date(parse(date)!!.time + days * DAY_MS)) }

    fun isPlayable(date: String, today: String): Boolean = date >= EPOCH && date <= today

    /** Milliseconds until the player's next local midnight (when the next puzzle unlocks). */
    fun msUntilNextPuzzle(nowMs: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Long {
        val next = Calendar.getInstance(zone).apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return next.timeInMillis - nowMs
    }
}

data class DexDailyPuzzle(
    val date: String,
    val number: Int,
    val answerId: Int,
    /** All choices including the answer, in display order. */
    val optionIds: List<Int>,
)

object DexDailyPuzzles {
    const val OPTION_COUNT = 6
    private const val SEED = 0x0DE7DA11L

    /** Memorable, recognisable Pokémon: starters, legendaries, fan favourites. */
    val POOL: List<Int> = listOf(
        6, 9, 3, 65, 68, 94, 130, 131, 143, 144, 145, 146, 149, 150, 151,
        157, 160, 196, 197, 212, 230, 243, 244, 245, 248, 249, 250, 251,
        254, 257, 260, 282, 306, 373, 376, 380, 381, 382, 383, 384, 385, 386,
        392, 395, 398, 445, 448, 461, 462, 463, 464, 466, 467, 483, 484, 485,
        486, 487, 491, 492, 493,
        497, 500, 503, 534, 609, 637, 641, 642, 643, 644, 645, 646, 647, 648, 649,
        654, 658, 681, 700, 716, 717, 718, 719, 720, 721,
        724, 727, 730, 745, 746, 785, 786, 787, 788, 800, 801, 802,
        812, 818, 823, 887, 888, 889, 890, 891, 892, 893, 894, 895, 896, 897, 898,
    )

    /**
     * The pool is walked as a shuffled cycle: every Pokémon appears once before any repeats
     * (the old daily picked at random, so repeats a few days apart were common).
     */
    fun forDate(date: String): DexDailyPuzzle {
        val index = DexDailyCalendar.dayIndex(date)
        val cycle = Math.floorDiv(index, POOL.size)
        val order = POOL.shuffled(Random(SEED + cycle))
        val answer = order[Math.floorMod(index, POOL.size)]
        val rng = Random(SEED * 31 + index)
        val decoys = POOL.filter { it != answer }.shuffled(rng).take(OPTION_COUNT - 1)
        return DexDailyPuzzle(date, index + 1, answer, (decoys + answer).shuffled(rng))
    }

    fun generationOf(id: Int): Int = when (id) {
        in 1..151 -> 1; in 152..251 -> 2; in 252..386 -> 3
        in 387..493 -> 4; in 494..649 -> 5; in 650..721 -> 6
        in 722..809 -> 7; in 810..905 -> 8; else -> 9
    }

    /** Official artwork by id: no API call needed (archive thumbnails, share card). */
    fun artworkUrl(id: Int) =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
}

/** A player's progress on one date's puzzle. */
data class DexDailyRecord(
    /** The wrong pick, if the single guess missed (a list so the stored format can grow). */
    val wrongIds: List<Int> = emptyList(),
    val solved: Boolean = false,
    /** Played later from the archive: never counts for XP or the streak. */
    val archive: Boolean = false,
    /** Pro revealed the clues before guessing (still a solve, smaller reward). */
    val hintsUsed: Boolean = false,
) {
    val failed: Boolean get() = !solved && wrongIds.size >= DexDailyRules.MAX_TRIES
    val finished: Boolean get() = solved || failed
}

enum class DexDailyClue { TYPES, GENERATION, FIRST_LETTER }

object DexDailyRules {
    /** One guess: with six names to pick from, a second chance made the puzzle trivial. */
    const val MAX_TRIES = 1

    /** Clues show once the puzzle is over, or earlier if a Pro player chose to reveal them. */
    fun cluesFor(record: DexDailyRecord): Set<DexDailyClue> =
        if (record.finished || record.hintsUsed) DexDailyClue.entries.toSet() else emptySet()

    /** Revealing hints is only possible before the guess (Pro gating happens in the UI layer). */
    fun useHints(record: DexDailyRecord): DexDailyRecord =
        if (record.finished) record else record.copy(hintsUsed = true)

    /** Applies a guess. Finished puzzles and repeated picks are ignored (no double counting). */
    fun guess(record: DexDailyRecord, puzzle: DexDailyPuzzle, optionId: Int): DexDailyRecord = when {
        record.finished || optionId in record.wrongIds || optionId !in puzzle.optionIds -> record
        optionId == puzzle.answerId -> record.copy(solved = true)
        else -> record.copy(wrongIds = record.wrongIds + optionId)
    }

    /** XP for finishing today's puzzle on the day. A pure shadow read pays most; trying still pays a little. */
    fun xpFor(record: DexDailyRecord): Int = when {
        !record.finished || record.archive -> 0
        !record.solved -> 5
        record.hintsUsed -> 15
        else -> 30
    }

    /**
     * Consecutive days solved on the day, ending today (or yesterday while today's puzzle
     * is still open, so the streak doesn't look broken in the morning).
     */
    fun streak(records: Map<String, DexDailyRecord>, today: String): Int {
        fun solvedLive(date: String) = records[date]?.let { it.solved && !it.archive } == true
        var day = if (solvedLive(today) || records[today]?.finished != true) today else return 0
        if (!solvedLive(day)) day = DexDailyCalendar.addDays(day, -1)
        var count = 0
        while (solvedLive(day)) {
            count++
            day = DexDailyCalendar.addDays(day, -1)
        }
        return count
    }
}

/**
 * Compact storage format for all records: "date|wrong,ids|s|a|h;..." (solved, archive, hints).
 * Hand-rolled instead of JSON so a missing field can never come back as a surprise null.
 */
object DexDailyCodec {
    fun encode(records: Map<String, DexDailyRecord>): String =
        records.entries.sortedBy { it.key }.joinToString(";") { (date, r) ->
            "$date|${r.wrongIds.joinToString(",")}|${if (r.solved) 1 else 0}|${if (r.archive) 1 else 0}|${if (r.hintsUsed) 1 else 0}"
        }

    fun decode(raw: String?): Map<String, DexDailyRecord> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            // 4 fields: written by the first test builds, before hints existed.
            if (parts.size !in 4..5 || parts[0].length != 10) return@mapNotNull null
            parts[0] to DexDailyRecord(
                wrongIds = parts[1].split(",").mapNotNull { it.toIntOrNull() },
                solved = parts[2] == "1",
                archive = parts[3] == "1",
                hintsUsed = parts.getOrNull(4) == "1",
            )
        }.toMap()
    }
}
