package com.aditya1875.pokeverse.feature.challenges.domain

import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import com.aditya1875.pokeverse.utils.LegendaryPokemon
import kotlin.random.Random

/**
 * Challenges, modelled on how Pokémon GO pays research: small tasks every day for a couple of balls, a bonus
 * for finishing the whole set, and long collection goals that pay the most. The point is to reward the
 * Pokémon a trainer would otherwise skip: nothing here pays extra for a Legendary, and the big goals ask for
 * many *different* Commons, Uncommons and Rares.
 *
 * Pure Kotlin so the rules are unit-tested; storage lives in the repositories.
 */
enum class Difficulty { EASY, MEDIUM, HARD, MILESTONE }

/** What a challenge counts. */
enum class ChallengeGoal {
    /** Catches today, any Pokémon. */
    CATCH_ANY,
    /** Species caught today for the first time. */
    CATCH_NEW,
    /** Catches today of [Challenge.minTier] or rarer. */
    CATCH_TIER,
    /** Best run of catches in a row today. */
    CATCH_STREAK,
    /** Species in the Pokédex, ever. */
    DEX_TOTAL,
    /** Different species of exactly [Challenge.minTier] caught, ever. */
    DEX_TIER,
    /** Pokémon caught in Wild Catch over the whole account. */
    CATCH_TOTAL,
    /** Longest run of catches in a row, ever. */
    STREAK_EVER,
    /** Pokémon evolved. */
    EVOLVED,
    /** Non-legendary species of one region's Pokédex caught, ever (see [Region]). */
    DEX_REGION,
}

/** How the long-term goals are grouped on screen, in the order they are shown. */
enum class ChallengeCategory { POKEDEX, COLLECTION, REGIONS, CATCHING, EVOLUTION }

/** A region's slice of the national Pokédex. Only what Wild Catch can spawn (up to #809). */
enum class Region(val from: Int, val to: Int) {
    KANTO(1, 151), JOHTO(152, 251), HOENN(252, 386), SINNOH(387, 493), UNOVA(494, 649), KALOS(650, 721), ALOLA(722, 809);

    /** Species the region asks for: every one except Legendaries and Mythicals, which no goal depends on. */
    val required: List<Int> get() = (from..to).filter { !LegendaryPokemon.isLegendary(it) }
}

/**
 * What a challenge pays. Daily ones pay balls; the long-term goals can also pay Dex Coins, Evo Shards and a
 * cosmetic ([cosmetic] is a Cosmetics id). The name is historical: it began as balls only.
 */
data class BallReward(
    val great: Int = 0,
    val ultra: Int = 0,
    val coins: Int = 0,
    val shards: Int = 0,
    val cosmetic: String? = null,
) {
    operator fun plus(other: BallReward) = BallReward(
        great + other.great, ultra + other.ultra, coins + other.coins, shards + other.shards, other.cosmetic ?: cosmetic,
    )
    val isEmpty: Boolean get() = great == 0 && ultra == 0 && coins == 0 && shards == 0 && cosmetic == null
}

data class Challenge(
    /** Stable key; a daily one carries its date, so each day's copy is claimed on its own. */
    val id: String,
    val difficulty: Difficulty,
    val goal: ChallengeGoal,
    val target: Int,
    val reward: BallReward,
    val minTier: CatchTier = CatchTier.COMMON,
    val daily: Boolean = true,
    /** For [ChallengeGoal.DEX_REGION] only. */
    val region: Region? = null,
) {
    val category: ChallengeCategory get() = when (goal) {
        ChallengeGoal.DEX_TOTAL -> ChallengeCategory.POKEDEX
        ChallengeGoal.DEX_TIER -> ChallengeCategory.COLLECTION
        ChallengeGoal.DEX_REGION -> ChallengeCategory.REGIONS
        ChallengeGoal.EVOLVED -> ChallengeCategory.EVOLUTION
        else -> ChallengeCategory.CATCHING
    }

    /** Goals on the same track are a ladder: only the lowest open one is shown. */
    val track: String get() = when (goal) {
        ChallengeGoal.DEX_TIER -> "tier:$minTier"
        ChallengeGoal.DEX_REGION -> "region:$region"
        else -> goal.name
    }
}

/** Everything progress is measured from: today's counters and the Pokédex. */
data class ChallengeProgress(
    val day: String = "",
    val catches: Int = 0,
    val newSpecies: Int = 0,
    /** Catches today per category. */
    val tierCatches: Map<CatchTier, Int> = emptyMap(),
    val bestStreak: Int = 0,
    val dexCaught: Int = 0,
    /** Different species caught per category, ever. */
    val dexTiers: Map<CatchTier, Int> = emptyMap(),
    /** Catches over the whole account, and the longest run in a row ever. */
    val lifetimeCatches: Int = 0,
    val bestStreakEver: Int = 0,
    val evolved: Int = 0,
    /** Non-legendary species caught per region. */
    val regionCaught: Map<Region, Int> = emptyMap(),
)

object ChallengeRules {

    /** Finishing all of a day's challenges pays this on top. */
    val SWEEP_BONUS = BallReward(ultra = 1)
    const val SWEEP_ID_PREFIX = "sweep:"
    const val DAILY_COUNT = 3

    /** Daily claim ids older than this many days are forgotten, so the claimed list can't grow forever. */
    const val KEEP_DAYS = 14

    private fun d(day: String, slot: Int, diff: Difficulty, goal: ChallengeGoal, target: Int, reward: BallReward,
                  tier: CatchTier = CatchTier.COMMON) =
        Challenge("d:$day:$slot", diff, goal, target, reward, tier)

    private val easyPool: List<(String) -> Challenge> = listOf(
        { day -> d(day, 0, Difficulty.EASY, ChallengeGoal.CATCH_ANY, 4, BallReward(great = 2)) },
        { day -> d(day, 0, Difficulty.EASY, ChallengeGoal.CATCH_ANY, 6, BallReward(great = 3)) },
        { day -> d(day, 0, Difficulty.EASY, ChallengeGoal.CATCH_TIER, 2, BallReward(great = 2), CatchTier.UNCOMMON) },
    )
    private val mediumPool: List<(String) -> Challenge> = listOf(
        { day -> d(day, 1, Difficulty.MEDIUM, ChallengeGoal.CATCH_NEW, 2, BallReward(great = 2, ultra = 1)) },
        { day -> d(day, 1, Difficulty.MEDIUM, ChallengeGoal.CATCH_TIER, 1, BallReward(ultra = 1), CatchTier.RARE) },
        { day -> d(day, 1, Difficulty.MEDIUM, ChallengeGoal.CATCH_STREAK, 3, BallReward(great = 1, ultra = 1)) },
    )
    private val hardPool: List<(String) -> Challenge> = listOf(
        { day -> d(day, 2, Difficulty.HARD, ChallengeGoal.CATCH_TIER, 3, BallReward(great = 2, ultra = 2), CatchTier.UNCOMMON) },
        { day -> d(day, 2, Difficulty.HARD, ChallengeGoal.CATCH_NEW, 4, BallReward(ultra = 2)) },
        { day -> d(day, 2, Difficulty.HARD, ChallengeGoal.CATCH_STREAK, 5, BallReward(ultra = 2)) },
        { day -> d(day, 2, Difficulty.HARD, ChallengeGoal.CATCH_TIER, 1, BallReward(ultra = 2), CatchTier.EPIC) },
    )

    /** The day's three challenges, one easy, one medium, one hard. The same for everyone on the same date. */
    fun dailyFor(day: String): List<Challenge> {
        val rng = Random(day.hashCode().toLong())
        return listOf(easyPool, mediumPool, hardPool).map { pool -> pool[rng.nextInt(pool.size)](day) }
    }

    /**
     * Long-term goals, in tracks: Pokédex size, rarity collections, lifetime catches, catch streaks,
     * evolutions, and one goal per region. Rewards grow with the effort and the big ones pay Dex Coins, Evo
     * Shards and a title. None of them depends on a Legendary.
     */
    val milestones: List<Challenge> = listOf(
        m("dex10", ChallengeGoal.DEX_TOTAL, 10, BallReward(great = 3)),
        m("dex25", ChallengeGoal.DEX_TOTAL, 25, BallReward(great = 2, ultra = 2)),
        m("dex50", ChallengeGoal.DEX_TOTAL, 50, BallReward(great = 3, ultra = 3)),
        m("dex100", ChallengeGoal.DEX_TOTAL, 100, BallReward(ultra = 5, coins = 150)),
        m("dex250", ChallengeGoal.DEX_TOTAL, 250, BallReward(ultra = 8, coins = 400)),
        m("dex500", ChallengeGoal.DEX_TOTAL, 500, BallReward(ultra = 12, coins = 900)),
        m("dex700", ChallengeGoal.DEX_TOTAL, 700, BallReward(ultra = 20, coins = 1500, cosmetic = "title_dexcompleter")),
        m("common20", ChallengeGoal.DEX_TIER, 20, BallReward(great = 4), CatchTier.COMMON),
        m("common60", ChallengeGoal.DEX_TIER, 60, BallReward(great = 4, ultra = 3), CatchTier.COMMON),
        m("common120", ChallengeGoal.DEX_TIER, 120, BallReward(ultra = 8, coins = 300), CatchTier.COMMON),
        m("uncommon15", ChallengeGoal.DEX_TIER, 15, BallReward(great = 3, ultra = 2), CatchTier.UNCOMMON),
        m("uncommon50", ChallengeGoal.DEX_TIER, 50, BallReward(ultra = 6), CatchTier.UNCOMMON),
        m("uncommon120", ChallengeGoal.DEX_TIER, 120, BallReward(ultra = 10, coins = 500), CatchTier.UNCOMMON),
        m("rare10", ChallengeGoal.DEX_TIER, 10, BallReward(ultra = 4), CatchTier.RARE),
        m("rare30", ChallengeGoal.DEX_TIER, 30, BallReward(ultra = 8), CatchTier.RARE),
        m("rare80", ChallengeGoal.DEX_TIER, 80, BallReward(ultra = 14, coins = 700), CatchTier.RARE),
        m("epic3", ChallengeGoal.DEX_TIER, 3, BallReward(ultra = 5), CatchTier.EPIC),
        m("epic8", ChallengeGoal.DEX_TIER, 8, BallReward(ultra = 10, coins = 500), CatchTier.EPIC),
        m("epic15", ChallengeGoal.DEX_TIER, 15, BallReward(ultra = 16, coins = 1000, cosmetic = "title_apex"), CatchTier.EPIC),

        m("catch100", ChallengeGoal.CATCH_TOTAL, 100, BallReward(great = 5, coins = 100)),
        m("catch500", ChallengeGoal.CATCH_TOTAL, 500, BallReward(ultra = 8, coins = 300)),
        m("catch1000", ChallengeGoal.CATCH_TOTAL, 1000, BallReward(ultra = 12, coins = 600, cosmetic = "title_collector")),
        m("catch2500", ChallengeGoal.CATCH_TOTAL, 2500, BallReward(ultra = 20, coins = 1500, cosmetic = "title_hoarder")),

        m("streak10", ChallengeGoal.STREAK_EVER, 10, BallReward(ultra = 3, coins = 100)),
        m("streak20", ChallengeGoal.STREAK_EVER, 20, BallReward(ultra = 6, coins = 300)),
        m("streak35", ChallengeGoal.STREAK_EVER, 35, BallReward(ultra = 10, coins = 700, cosmetic = "title_untouchable")),

        m("evo5", ChallengeGoal.EVOLVED, 5, BallReward(great = 3, shards = 40)),
        m("evo20", ChallengeGoal.EVOLVED, 20, BallReward(ultra = 5, shards = 120, coins = 300)),
        m("evo50", ChallengeGoal.EVOLVED, 50, BallReward(ultra = 10, shards = 300, coins = 800, cosmetic = "title_evolutionist")),

        r(Region.KANTO, BallReward(ultra = 8, coins = 600, cosmetic = "title_kanto")),
        r(Region.JOHTO, BallReward(ultra = 6, coins = 500, cosmetic = "title_johto")),
        r(Region.HOENN, BallReward(ultra = 8, coins = 650, cosmetic = "title_hoenn")),
        r(Region.SINNOH, BallReward(ultra = 7, coins = 550, cosmetic = "title_sinnoh")),
        r(Region.UNOVA, BallReward(ultra = 10, coins = 800, cosmetic = "title_unova")),
        r(Region.KALOS, BallReward(ultra = 5, coins = 450, cosmetic = "title_kalos")),
        r(Region.ALOLA, BallReward(ultra = 8, coins = 600, cosmetic = "title_alola")),
    )

    private fun r(region: Region, reward: BallReward) =
        Challenge("m:region_${region.name.lowercase()}", Difficulty.MILESTONE, ChallengeGoal.DEX_REGION, region.required.size, reward,
            daily = false, region = region)

    private fun m(key: String, goal: ChallengeGoal, target: Int, reward: BallReward, tier: CatchTier = CatchTier.COMMON) =
        Challenge("m:$key", Difficulty.MILESTONE, goal, target, reward, tier, daily = false)

    /** How far [challenge] has got, never past its target. */
    fun progressOf(challenge: Challenge, p: ChallengeProgress): Int {
        val raw = when (challenge.goal) {
            ChallengeGoal.CATCH_ANY -> p.catches
            ChallengeGoal.CATCH_NEW -> p.newSpecies
            ChallengeGoal.CATCH_TIER -> p.tierCatches.filterKeys { it >= challenge.minTier }.values.sum()
            ChallengeGoal.CATCH_STREAK -> p.bestStreak
            ChallengeGoal.DEX_TOTAL -> p.dexCaught
            ChallengeGoal.DEX_TIER -> p.dexTiers[challenge.minTier] ?: 0
            ChallengeGoal.CATCH_TOTAL -> p.lifetimeCatches
            ChallengeGoal.STREAK_EVER -> p.bestStreakEver
            ChallengeGoal.EVOLVED -> p.evolved
            ChallengeGoal.DEX_REGION -> p.regionCaught[challenge.region] ?: 0
        }
        return raw.coerceIn(0, challenge.target)
    }

    fun isComplete(challenge: Challenge, p: ChallengeProgress): Boolean =
        progressOf(challenge, p) >= challenge.target

    fun sweepId(day: String) = "$SWEEP_ID_PREFIX$day"

    /** Whether today's bonus can be taken: every daily claimed, the bonus itself not yet. */
    fun sweepReady(day: String, claimed: Set<String>): Boolean =
        dailyFor(day).all { it.id in claimed } && sweepId(day) !in claimed

    /** The next unclaimed milestone of each goal kind, so the list shows what's next instead of 13 rows. */
    fun nextMilestones(claimed: Set<String>, p: ChallengeProgress): List<Challenge> {
        val open = milestones.filter { it.id !in claimed }
        // One per track, the lowest target still open.
        return open.groupBy { it.track }
            .values.map { track -> track.minBy { it.target } }
            .sortedBy { progressOf(it, p).toFloat() / it.target }.reversed()
    }

    /** What the claimed set should keep: milestones forever, recent days only. Day strings sort by date. */
    fun prune(claimed: Set<String>, today: String): Set<String> {
        val cutoff = daysBefore(today, KEEP_DAYS)
        return claimed.filter { id ->
            val day = when {
                id.startsWith("d:") -> id.split(":").getOrNull(1)
                id.startsWith(SWEEP_ID_PREFIX) -> id.removePrefix(SWEEP_ID_PREFIX)
                else -> null
            }
            day == null || day >= cutoff
        }.toSet()
    }

    /** "yyyy-MM-dd" minus [days], as the same format. */
    fun daysBefore(day: String, days: Int): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            val parts = day.split("-").mapNotNull { it.toIntOrNull() }
            if (parts.size == 3) set(parts[0], parts[1] - 1, parts[2], 0, 0, 0)
            add(java.util.Calendar.DAY_OF_YEAR, -days)
        }
        return "%04d-%02d-%02d".format(
            cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    /** The category of a species the trainer has caught: Legendary is known by id, the rest from what was recorded. */
    fun tierOfCaught(id: Int, recorded: Map<Int, CatchTier>): CatchTier? =
        if (LegendaryPokemon.isLegendary(id)) CatchTier.LEGENDARY else recorded[id]

    /** Species caught per category; species with no recorded category (caught before this was tracked) are left out. */
    fun tierCounts(caught: Set<Int>, recorded: Map<Int, CatchTier>): Map<CatchTier, Int> =
        caught.mapNotNull { tierOfCaught(it, recorded) }.groupingBy { it }.eachCount()

    /** Non-legendary species caught in each region. */
    fun regionCounts(caught: Set<Int>): Map<Region, Int> =
        Region.entries.associateWith { region -> caught.count { it in region.from..region.to && !LegendaryPokemon.isLegendary(it) } }
}
