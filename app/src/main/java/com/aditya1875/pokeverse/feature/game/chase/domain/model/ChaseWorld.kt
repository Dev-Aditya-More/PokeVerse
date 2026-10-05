package com.aditya1875.pokeverse.feature.game.chase.domain.model

enum class EntityKind(val isObstacle: Boolean) {
    KOFFING(isObstacle = true),
    EKANS(isObstacle = true),
    WOBBUFFET(isObstacle = true),
    /** A net that already landed — purely visual, it scrolls away harmlessly. */
    LANDED_NET(isObstacle = false),
    ORAN_BERRY(isObstacle = false),
    THUNDER_STONE(isObstacle = false);

    val isPickup: Boolean get() = this == ORAN_BERRY || this == THUNDER_STONE

    companion object {
        val obstacles = entries.filter { it.isObstacle }
    }
}

data class ChaseEntity(
    val id: Long,
    val kind: EntityKind,
    val lane: Int,
    val y: Float
)

/** Team Rocket has aimed a net at [lane]; it lands when [remainingMs] hits zero. */
data class NetWarning(
    val lane: Int,
    val remainingMs: Long,
    val totalMs: Long
) {
    val progress: Float get() = 1f - remainingMs / totalMs.toFloat()
}

enum class ChaseStatus { Running, Caught }

/** Spawner bookkeeping — kept in the snapshot so the engine itself stays stateless. */
data class SpawnState(
    val rowProgress: Float = 0f,
    val lastFreeLanes: Set<Int> = emptySet(),
    val nextNetInMs: Long = 0L,
    val nextEntityId: Long = 0L
)

/**
 * One immutable frame of the chase. The engine produces a new one every tick;
 * the renderer and HUD only ever read it.
 */
data class ChaseWorld(
    val status: ChaseStatus = ChaseStatus.Running,
    val targetLane: Int,
    /** Pikachu's current, possibly mid-slide, lane position (e.g. 1.4 = between lanes 1 and 2). */
    val playerX: Float,
    val speed: Float,
    val meters: Float = 0f,
    val bonusPoints: Int = 0,
    val berries: Int = 0,
    val lives: Int,
    val maxLives: Int,
    /** Thunderbolt charge, 0..1. Usable at 1. */
    val charge: Float = 0f,
    val entities: List<ChaseEntity> = emptyList(),
    val warnings: List<NetWarning> = emptyList(),
    val invulnerableMs: Long = 0L,
    val thunderFlashMs: Long = 0L,
    val elapsedMs: Long = 0L,
    val metersSinceHit: Float = 0f,
    val revivesUsed: Int = 0,
    val spawn: SpawnState = SpawnState()
) {
    val score: Int get() = meters.toInt() + bonusPoints
    val canThunderbolt: Boolean get() = charge >= 1f && status == ChaseStatus.Running
    /** 0 = Team Rocket far behind, 1 = about to grab Pikachu. Drives the balloon's position. */
    val rocketCloseness: Float get() = (maxLives - lives).toFloat() / maxLives
}

/** Things that happened during a step — the UI turns these into sound and haptics. */
sealed interface ChaseEvent {
    data class Hit(val livesLeft: Int) : ChaseEvent
    data object Caught : ChaseEvent
    data class Collected(val kind: EntityKind) : ChaseEvent
    data object NetIncoming : ChaseEvent
    data object NetDodged : ChaseEvent
    data class ThunderUsed(val cleared: Int) : ChaseEvent
    data object ShookOffRocket : ChaseEvent
}

data class ChaseStep(val world: ChaseWorld, val events: List<ChaseEvent> = emptyList())
