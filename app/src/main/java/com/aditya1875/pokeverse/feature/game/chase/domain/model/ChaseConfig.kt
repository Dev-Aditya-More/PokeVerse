package com.aditya1875.pokeverse.feature.game.chase.domain.model

/**
 * Every tuning number for Rocket Chase in one place. All vertical values are
 * fractions of the visible track height (0 = top edge, 1 = bottom edge), so the
 * game plays identically on every screen size.
 */
data class ChaseConfig(
    val laneCount: Int = 3,

    // ── Geometry ────────────────────────────────────────────────────────────
    val playerY: Float = 0.78f,
    val spawnY: Float = -0.12f,
    val despawnY: Float = 1.15f,
    /** How close (vertically) an entity must be to Pikachu to touch it. */
    val hitRangeY: Float = 0.05f,
    /** How close (in lanes) Pikachu must be — < 0.5 so a half-finished dodge still counts as a dodge. */
    val laneHitTolerance: Float = 0.42f,
    /** Top slide speed in lanes per second — caps a far lane jump so it's visibly a dash, not a teleport. */
    val laneSwitchSpeed: Float = 7f,
    /** How hard Pikachu eases toward its target lane (higher = snappier). ~0.15 s to cover 90% of one lane. */
    val laneEasing: Float = 15f,

    // ── Pace ────────────────────────────────────────────────────────────────
    /** Track scroll speed, in screens per second. */
    val startSpeed: Float = 0.55f,
    val maxSpeed: Float = 1.35f,
    /** Distance over which speed and density ramp from start to max. */
    val rampMeters: Float = 2500f,
    val metersPerScreen: Float = 20f,
    /** Vertical gap between obstacle rows, shrinking as difficulty ramps. */
    val rowGapStart: Float = 0.45f,
    val rowGapMin: Float = 0.27f,
    /** Chance a row blocks two lanes instead of one, at max difficulty. */
    val doubleBlockChanceMax: Float = 0.55f,

    // ── Lives ───────────────────────────────────────────────────────────────
    val maxLives: Int = 3,
    val invulnerableMs: Long = 1200L,
    val reviveInvulnerableMs: Long = 2000L,
    /** Running this far without a hit shakes Team Rocket off by one step (+1 life). */
    val lifeRegenMeters: Float = 400f,
    val maxRevives: Int = 1,

    // ── Team Rocket nets ────────────────────────────────────────────────────
    val netsStartMeters: Float = 250f,
    val netIntervalStartMs: Long = 5000L,
    val netIntervalMinMs: Long = 2400L,
    val netWarningMs: Long = 1000L,
    /** A net that lands on Pikachu snares it: no lane changes until it breaks free or this runs out (-1 life). */
    val snareMs: Long = 2200L,
    /** Taps needed to break out of a net. */
    val snareStruggles: Int = 5,

    // ── Pickups & Thunderbolt ───────────────────────────────────────────────
    val pickupChance: Float = 0.45f,
    val thunderStoneChance: Float = 0.1f,
    val berryPoints: Int = 10,
    /** Each Oran Berry fills this much of the Thunderbolt meter; a Thunder Stone fills it outright. */
    val berryCharge: Float = 0.2f,
    val thunderClearPoints: Int = 5,
    val thunderFlashMs: Long = 450L,

    // ── Agility (lifeline) ──────────────────────────────────────────────────
    val agilityMs: Long = 10_000L,
    /** Free uses per run. */
    val agilityFreeCharges: Int = 1,
    /** Extra uses a run can earn (rewarded ad / premium) — caps it so it can't be spammed. */
    val agilityMaxExtra: Int = 2,
    /** Pikachu zaps one attacker at a time, this often. */
    val agilityStrikeIntervalMs: Long = 280L,
    /** Track runs this much faster during the dash. */
    val agilitySpeedBoost: Float = 1.6f,
    /** Pickups this close (vertically) are pulled in from any lane. */
    val agilityMagnetRangeY: Float = 0.12f,
    /** During Agility, rows are pickup rain: this many berries per row. */
    val agilityPickupsPerRow: Int = 2,
    val agilityThunderStoneChance: Float = 0.25f,
    val agilityZapPoints: Int = 5,
    /** How long a zap bolt stays drawn. */
    val strikeFlashMs: Long = 220L,

    /** Frames longer than this are clamped so a hitch never teleports obstacles through Pikachu. */
    val maxFrameMs: Long = 50L
)
