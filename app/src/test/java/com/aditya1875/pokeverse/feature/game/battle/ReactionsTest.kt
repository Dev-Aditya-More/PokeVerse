package com.aditya1875.pokeverse.feature.game.battle

import com.aditya1875.pokeverse.feature.game.battle.domain.Reactions
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ReactionsTest {

    @Test fun `every auto reaction is a real emoji`() {
        val r = Random(7)
        repeat(500) {
            Reactions.Moment.entries.forEach { m ->
                Reactions.auto(m, r)?.let { assertTrue(Reactions.emoji(it) != null) }
            }
        }
    }

    @Test fun `the rival stays quiet much of the time`() {
        val r = Random(11)
        val said = (1..1000).count { Reactions.auto(Reactions.Moment.RIVAL_HIT_HARD, r) != null }
        assertTrue("said $said of 1000", said in 300..600)
    }

    @Test fun `a losing rival never gloats`() {
        val r = Random(3)
        repeat(300) {
            Reactions.auto(Reactions.Moment.RIVAL_LOSING_LAST, r)?.let {
                assertTrue(it == Reactions.YIKES || it == Reactions.GG)
            }
        }
    }
}
