package com.aditya1875.pokeverse.feature.game.core.domain

/** What happened when a waiting player went to join another waiting player's room. */
enum class YieldResult {
    /** Own room closed, seat in the other room taken: carry on as the guest there. */
    JOINED,

    /** Someone joined this player's own room first (or it could not be closed): that match is the real one. */
    STAYED,

    /** Own room closed but the other room was taken meanwhile: open a new search. */
    LOST,
}

/**
 * The rules both online games (Card Clash, Team Battle) share for pairing strangers.
 *
 * The problem they solve: a player who finds nobody opens a room and waits. If two players tap "play" at the
 * same moment, neither sees the other's room yet, so BOTH open one and sit alone, while a third player
 * arriving a few seconds later would match instantly. Waiting players therefore keep looking, and when two
 * of them find each other exactly one must give way and join the other, otherwise they would join each
 * other's rooms at the same time and end up in two matches. The room ids decide it: every phone compares
 * the same two ids, so they all agree who yields without talking to each other.
 *
 * Pure Kotlin so every one of these decisions is unit tested.
 */
object Matchmaking {

    /** A room somebody is waiting in. [createdAtMs] is only used to tell abandoned rooms from live ones. */
    data class OpenRoom(val id: String, val hostId: String, val createdAtMs: Long)

    /** How often a waiting player looks for another waiting player. */
    const val LOOK_EVERY_SECONDS = 2

    /**
     * The rooms a player could really be matched with or counted as "waiting": someone else's, and not left
     * behind by a player who closed the app (such a room never gets an answer).
     */
    fun live(rooms: Collection<OpenRoom>, myUid: String, nowMs: Long, staleAfterMs: Long): List<OpenRoom> =
        rooms.filter { it.hostId != myUid && nowMs - it.createdAtMs < staleAfterMs }

    /**
     * Which of [others] the owner of [myRoomId] should go and join, or null to keep waiting. Only the room
     * with the LARGER id gives way, and it joins the smallest one, so of any two waiting players exactly one
     * moves, and with many waiting players they all drain towards the smallest room.
     */
    fun roomToYieldTo(myRoomId: String, others: Collection<OpenRoom>): String? =
        others.map { it.id }.filter { it < myRoomId }.minOrNull()

    /** Whether it is time for a waiting player to look again, [elapsedSeconds] after their search began. */
    fun shouldLookNow(elapsedSeconds: Int): Boolean = elapsedSeconds > 0 && elapsedSeconds % LOOK_EVERY_SECONDS == 0

    /** How many other players are waiting right now: what the "players in the pool" line shows. */
    fun waitingCount(rooms: Collection<OpenRoom>, myUid: String, nowMs: Long, staleAfterMs: Long): Int =
        live(rooms, myUid, nowMs, staleAfterMs).map { it.hostId }.distinct().size
}
