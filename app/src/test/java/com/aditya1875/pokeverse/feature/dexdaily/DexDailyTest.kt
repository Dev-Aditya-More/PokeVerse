package com.aditya1875.pokeverse.feature.dexdaily

import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyCalendar
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyClue
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyCodec
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyPuzzles
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyRecord
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class DexDailyTest {

    @Test fun `numbers count from the epoch`() {
        assertEquals(1, DexDailyCalendar.number("2026-10-01"))
        assertEquals(32, DexDailyCalendar.number("2026-11-01"))
        assertEquals(366, DexDailyCalendar.number("2027-10-01"))
        assertEquals("2026-11-01", DexDailyCalendar.addDays("2026-10-31", 1))
        assertEquals("2026-02-28", DexDailyCalendar.addDays("2026-03-01", -1))
    }

    @Test fun `day math ignores daylight saving`() {
        // Europe moves clocks on 2026-10-25; that day must still be exactly one step.
        assertEquals(1, DexDailyCalendar.dayIndex("2026-10-26") - DexDailyCalendar.dayIndex("2026-10-25"))
    }

    @Test fun `today uses the player's own calendar`() {
        val instant = 1_793_577_600_000L // 2026-11-02T00:00:00Z
        assertEquals("2026-11-02", DexDailyCalendar.today(instant, TimeZone.getTimeZone("Asia/Kolkata")))
        assertEquals("2026-11-01", DexDailyCalendar.today(instant, TimeZone.getTimeZone("America/Sao_Paulo")))
    }

    @Test fun `same date gives everyone the same puzzle`() {
        assertEquals(DexDailyPuzzles.forDate("2026-11-05"), DexDailyPuzzles.forDate("2026-11-05"))
    }

    @Test fun `puzzle has six distinct options including the answer`() {
        for (i in 0 until 400) {
            val p = DexDailyPuzzles.forDate(DexDailyCalendar.addDays(DexDailyCalendar.EPOCH, i))
            assertEquals(DexDailyPuzzles.OPTION_COUNT, p.optionIds.toSet().size)
            assertTrue(p.answerId in p.optionIds)
        }
    }

    @Test fun `no Pokémon repeats within one pass of the pool`() {
        val size = DexDailyPuzzles.POOL.size
        val answers = (0 until size).map { DexDailyPuzzles.forDate(DexDailyCalendar.addDays(DexDailyCalendar.EPOCH, it)).answerId }
        assertEquals(size, answers.toSet().size)
    }

    private val puzzle = DexDailyPuzzles.forDate("2026-11-01")
    private val wrong = puzzle.optionIds.filter { it != puzzle.answerId }

    @Test fun `one guess - right answer solves for full XP`() {
        val r = DexDailyRules.guess(DexDailyRecord(), puzzle, puzzle.answerId)
        assertTrue(r.solved); assertEquals(30, DexDailyRules.xpFor(r))
        assertEquals(DexDailyClue.entries.toSet(), DexDailyRules.cluesFor(r)) // clues shown after the reveal
    }

    @Test fun `one guess - a miss ends the puzzle`() {
        val r = DexDailyRules.guess(DexDailyRecord(), puzzle, wrong[0])
        assertTrue(r.failed); assertTrue(r.finished); assertEquals(5, DexDailyRules.xpFor(r))
        // Finished: further guesses (even the answer) change nothing.
        assertEquals(r, DexDailyRules.guess(r, puzzle, puzzle.answerId))
    }

    @Test fun `hints reveal clues before guessing and halve the reward`() {
        assertTrue(DexDailyRules.cluesFor(DexDailyRecord()).isEmpty())
        val hinted = DexDailyRules.useHints(DexDailyRecord())
        assertEquals(DexDailyClue.entries.toSet(), DexDailyRules.cluesFor(hinted))
        val solved = DexDailyRules.guess(hinted, puzzle, puzzle.answerId)
        assertEquals(15, DexDailyRules.xpFor(solved))
        // Too late to take hints once the guess is in.
        val missed = DexDailyRules.guess(DexDailyRecord(), puzzle, wrong[0])
        assertEquals(missed, DexDailyRules.useHints(missed))
    }

    @Test fun `answers outside the options are ignored`() {
        assertEquals(DexDailyRecord(), DexDailyRules.guess(DexDailyRecord(), puzzle, 99999))
    }

    @Test fun `archive plays never pay XP`() {
        val r = DexDailyRules.guess(DexDailyRecord(archive = true), puzzle, puzzle.answerId)
        assertEquals(0, DexDailyRules.xpFor(r))
    }

    @Test fun `streak counts live solves and survives an unplayed morning`() {
        val solved = DexDailyRecord(solved = true)
        val records = mapOf("2026-10-29" to solved, "2026-10-30" to solved, "2026-10-31" to solved)
        assertEquals(3, DexDailyRules.streak(records, "2026-11-01"))           // today not played yet
        assertEquals(4, DexDailyRules.streak(records + ("2026-11-01" to solved), "2026-11-01"))
        val failedToday = DexDailyRecord(wrongIds = wrong.take(1))
        assertEquals(0, DexDailyRules.streak(records + ("2026-11-01" to failedToday), "2026-11-01"))
        val archived = mapOf("2026-10-31" to DexDailyRecord(solved = true, archive = true))
        assertEquals(0, DexDailyRules.streak(archived, "2026-11-01"))
        assertEquals(0, DexDailyRules.streak(mapOf("2026-10-30" to solved), "2026-11-01")) // gap yesterday
    }

    @Test fun `codec round-trips and skips junk`() {
        val records = mapOf(
            "2026-11-01" to DexDailyRecord(solved = true, hintsUsed = true),
            "2026-10-15" to DexDailyRecord(wrongIds = listOf(9), archive = true),
            "2026-10-16" to DexDailyRecord(),
        )
        assertEquals(records, DexDailyCodec.decode(DexDailyCodec.encode(records)))
        assertTrue(DexDailyCodec.decode("garbage;;2026-11-01|x|1").isEmpty())
        // Records saved by the first test builds (no hints field) still load.
        assertEquals(DexDailyRecord(solved = true), DexDailyCodec.decode("2026-11-01||1|0")["2026-11-01"])
        assertFalse(DexDailyCodec.decode(null).isNotEmpty())
    }
}
