package com.aditya1875.pokeverse.feature.pokemon.dex

import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeRules
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionEconomy
import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionRules
import androidx.datastore.preferences.core.booleanPreferencesKey
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dexStore: DataStore<Preferences> by preferencesDataStore("pokedex_progress")

/**
 * What this trainer has encountered. Every caught Pokémon is also seen, so [seen] is a superset of
 * [caught]. Ids are national-dex numbers (1..[TOTAL]); alternate forms are not tracked.
 */
data class DexProgress(
    val seen: Set<Int> = emptySet(),
    val caught: Set<Int> = emptySet(),
    /**
     * Forms that were evolved *into something else*. They stay in the Pokédex ([caught] never shrinks)
     * but are no longer on the trainer's roster, since evolving uses the old form up.
     */
    val consumed: Set<Int> = emptySet(),
    /** The category each caught species was in when it was caught (Legendary is known by id, so it isn't stored). */
    val tiers: Map<Int, CatchTier> = emptyMap(),
    /** False only for the placeholder emitted before the stored progress has been read. */
    val loaded: Boolean = true,
) {
    val seenCount: Int get() = seen.size
    /** How many you have right now. An evolved form takes the place of the one it came from, so this does not drop. */
    val caughtCount: Int get() = roster.size

    /** The Pokémon the trainer actually has right now: caught, and not evolved away. Teams and Team Battle use this. */
    val roster: Set<Int> get() = caught - consumed

    /** How many different species of each category the Pokédex holds (evolved-away forms still count). */
    val tierCounts: Map<CatchTier, Int> get() = ChallengeRules.tierCounts(caught, tiers)

    companion object {
        const val TOTAL = 1025

        /** Wild Catch spawns #1..this, and it is the only way to catch, so "caught" is out of this. */
        const val CATCHABLE = 809

        /** A full team; all six caught earns the "Trainer's party" mark. */
        const val PARTY_SIZE = 6

        /** Pikachu: the starter for anyone who skips or predates the professor intro. */
        const val DEFAULT_STARTER_ID = 25
        val EMPTY = DexProgress(loaded = false)
    }
}

/** The Evo Shards a trainer has earned and spent, and the forms they have evolved into. */
data class EvolutionWallet(
    val earned: Int = 0,
    val spent: Int = 0,
    val evolved: Set<Int> = emptySet(),
) {
    val balance: Int get() = (earned - spent).coerceAtLeast(0)
}

sealed interface EvolveResult {
    data object Success : EvolveResult
    data object NotEnough : EvolveResult
    data object AlreadyHave : EvolveResult
    data object NotCaught : EvolveResult
    /** The evolution needs a stone or held item the trainer does not have. */
    data object NoItem : EvolveResult
}

/**
 * Pokédex progress, kept on the device and mirrored to the signed-in account (users/{uid}.dexSeen and
 * .dexCaught) so it survives a reinstall or a new phone. Seen = opened its detail page, met in Wild
 * Catch, favourited, put on a team, or chosen as the starter. Caught = caught in Wild Catch, plus the
 * starter you begin with. Entries are only ever added, so merging two copies is a union.
 */
class PokedexRepository(context: Context) {

    private val ds = context.applicationContext.dexStore
    private val seenKey = stringSetPreferencesKey("seen")
    // "caught_wild": earlier builds also counted teams and starters under "caught"; a new key drops that.
    private val caughtKey = stringSetPreferencesKey("caught_wild")
    private val starterChosenKey = booleanPreferencesKey("starter_chosen")
    private val evoEarnedKey = androidx.datastore.preferences.core.intPreferencesKey("evo_earned")
    private val evoSpentKey = androidx.datastore.preferences.core.intPreferencesKey("evo_spent")
    private val evolvedKey = stringSetPreferencesKey("evolved")
    private val consumedKey = stringSetPreferencesKey("consumed")
    /** "id:category" for every species caught in Wild Catch, so the Pokédex can count Common, Rare and so on. */
    private val tiersKey = stringSetPreferencesKey("tiers")
    private val evoDayKey = androidx.datastore.preferences.core.stringPreferencesKey("evo_day")
    private val evoDayEarnedKey = androidx.datastore.preferences.core.intPreferencesKey("evo_day_earned")

    /**
     * A request from another screen (the Profile's Seen/Caught cards) for Home to open with this filter
     * applied. Home consumes it, so it fires once.
     */
    val requestedFilter = kotlinx.coroutines.flow.MutableStateFlow<DexFilter?>(null)

    val progress: Flow<DexProgress> = ds.data.map { p ->
        DexProgress(
            seen = p[seenKey].toIds(),
            caught = p[caughtKey].toIds(),
            consumed = p[consumedKey].toIds(),
            tiers = p[tiersKey].toTiers(),
        )
    }

    // ── Evolution ───────────────────────────────────────────────────────────
    // Shards only ever go up on both sides (earned, spent), so merging two phones is a max, and the
    // forms you have evolved into are a set, so merging is a union.

    val wallet: Flow<EvolutionWallet> = ds.data.map { p ->
        EvolutionWallet(
            earned = p[evoEarnedKey] ?: 0,
            spent = p[evoSpentKey] ?: 0,
            evolved = p[evolvedKey].toIds(),
        )
    }

    /**
     * Adds up to [amount] shards, never more than today's cap allows, and returns what was actually
     * granted (0 once the day's limit is reached). The day's count lives on this device.
     */
    suspend fun awardShards(amount: Int): Int {
        if (amount <= 0) return 0
        var granted = 0
        val today = java.time.LocalDate.now().toString()
        ds.edit { p ->
            val earnedToday = if (p[evoDayKey] == today) p[evoDayEarnedKey] ?: 0 else 0
            granted = EvolutionEconomy.withinCap(earnedToday, amount)
            p[evoDayKey] = today
            p[evoDayEarnedKey] = earnedToday + granted
            if (granted > 0) p[evoEarnedKey] = ((p[evoEarnedKey] ?: 0) + granted).coerceAtMost(MAX_SHARDS)
        }
        if (granted > 0) scheduleSync()
        return granted
    }

    /** Debug/testing only: adds shards ignoring the daily cap. */
    suspend fun grantTestShards(amount: Int) {
        ds.edit { p -> p[evoEarnedKey] = ((p[evoEarnedKey] ?: 0) + amount).coerceAtMost(MAX_SHARDS) }
        scheduleSync()
    }

    /** Adds shards bought with coins. Unlike earned shards these skip the daily cap: the coins were the limit. */
    suspend fun grantPurchasedShards(amount: Int) {
        if (amount <= 0) return
        ds.edit { p -> p[evoEarnedKey] = ((p[evoEarnedKey] ?: 0) + amount).coerceAtMost(MAX_SHARDS) }
        scheduleSync()
    }

    /**
     * Evolves [fromId] into [toId] for [cost] shards (0 when an item pays for it), atomically: the shards,
     * the roster swap and the record of the evolution all change together or not at all.
     *
     * The old form is used up: it goes into [DexProgress.consumed], so it leaves the roster (it stays in the
     * Pokédex). That is also why an Eevee that became Vaporeon cannot then become Umbreon.
     */
    suspend fun evolve(fromId: Int, toId: Int, cost: Int): EvolveResult {
        if (toId !in 1..DexProgress.TOTAL) return EvolveResult.NotCaught
        var result: EvolveResult = EvolveResult.NotEnough
        ds.edit { p ->
            val caught = p[caughtKey].toIds()
            val consumed = p[consumedKey].toIds()
            val roster = caught - consumed
            val balance = (p[evoEarnedKey] ?: 0) - (p[evoSpentKey] ?: 0)
            when (EvolutionRules.check(balance, cost, fromId in roster, toId in roster)) {
                EvolutionRules.Check.OK -> {
                    p[evoSpentKey] = (p[evoSpentKey] ?: 0) + cost
                    p[caughtKey] = p[caughtKey].orEmpty() + toId.toString()
                    p[seenKey] = p[seenKey].orEmpty() + toId.toString()
                    p[evolvedKey] = p[evolvedKey].orEmpty() + toId.toString()
                    p[consumedKey] = ((consumed + fromId) - toId).mapTo(HashSet()) { it.toString() }
                    result = EvolveResult.Success
                }
                EvolutionRules.Check.NOT_CAUGHT -> result = EvolveResult.NotCaught
                EvolutionRules.Check.ALREADY_HAVE -> result = EvolveResult.AlreadyHave
                EvolutionRules.Check.NOT_ENOUGH -> result = EvolveResult.NotEnough
            }
        }
        if (result == EvolveResult.Success) scheduleSync()
        return result
    }

    /** Returns true only the first time this Pokémon is seen, so callers can celebrate the new entry. */
    suspend fun markSeen(id: Int): Boolean {
        if (id !in 1..DexProgress.TOTAL) return false
        var isNew = false
        ds.edit { p ->
            val seen = p[seenKey].orEmpty()
            if (id.toString() !in seen) {
                p[seenKey] = seen + id.toString()
                isNew = true
            }
        }
        if (isNew) scheduleSync()
        return isNew
    }

    /** The starter is the one Pokémon you start out with, so it is caught from day one. */
    suspend fun chooseStarter(id: Int) {
        if (id !in 1..DexProgress.TOTAL) return
        ds.edit { p ->
            p[starterChosenKey] = true
            p[caughtKey] = p[caughtKey].orEmpty() + id.toString()
            p[seenKey] = p[seenKey].orEmpty() + id.toString()
        }
        scheduleSync()
    }

    /** Anyone who skipped the intro, or installed before it existed, starts with Pikachu. Runs once. */
    suspend fun ensureDefaultStarter() {
        val id = DexProgress.DEFAULT_STARTER_ID.toString()
        ds.edit { p ->
            if (p[starterChosenKey] != true) {
                p[starterChosenKey] = true
                p[caughtKey] = p[caughtKey].orEmpty() + id
                p[seenKey] = p[seenKey].orEmpty() + id
            }
        }
        scheduleSync()
    }

    suspend fun markCaught(id: Int, tier: CatchTier? = null) {
        if (id !in 1..DexProgress.TOTAL) return
        ds.edit { p ->
            // The first category recorded for a species stays: the same Pokémon is always the same category.
            if (tier != null && p[tiersKey].toTiers()[id] == null) p[tiersKey] = p[tiersKey].orEmpty() + "$id:${tier.ordinal}"
            p[caughtKey] = p[caughtKey].orEmpty() + id.toString()
            p[seenKey] = p[seenKey].orEmpty() + id.toString()
            // Catching one you evolved away puts a fresh one back on the roster.
            p[consumedKey]?.let { c -> if (id.toString() in c) p[consumedKey] = c - id.toString() }
        }
        scheduleSync()
    }

    // ── Cloud sync ──────────────────────────────────────────────────────────
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingSync: Job? = null

    /** Batches a burst of changes (a Wild Catch session) into one write a few seconds after the last. */
    private fun scheduleSync() {
        if (FirebaseAuth.getInstance().currentUser == null) return
        pendingSync?.cancel()
        pendingSync = syncScope.launch {
            delay(SYNC_DEBOUNCE_MS)
            push()
        }
    }

    /** arrayUnion, so a stale phone can only ever add entries, never remove another phone's. */
    private suspend fun push() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val p = ds.data.first()
        val seen = p[seenKey].toIds()
        val caught = p[caughtKey].toIds()
        val evolved = p[evolvedKey].toIds()
        val consumed = p[consumedKey].toIds()
        val tiers = p[tiersKey].orEmpty().toList()
        val earned = p[evoEarnedKey] ?: 0
        val spent = (p[evoSpentKey] ?: 0).coerceAtMost(earned)
        if (seen.isEmpty() && caught.isEmpty() && earned == 0) return
        val fields = buildMap<String, Any> {
            // Unlike the sets above this one can shrink (catching an evolved-away form again), so it is written
            // whole rather than unioned. Written even when empty, to clear what an older write left behind.
            put("dexConsumed", consumed.sorted())
            if (tiers.isNotEmpty()) put("dexTiers", FieldValue.arrayUnion(*tiers.sorted().toTypedArray()))
            if (seen.isNotEmpty()) put("dexSeen", FieldValue.arrayUnion(*seen.sorted().toTypedArray()))
            if (caught.isNotEmpty()) put("dexCaught", FieldValue.arrayUnion(*caught.sorted().toTypedArray()))
            if (evolved.isNotEmpty()) put("dexEvolved", FieldValue.arrayUnion(*evolved.sorted().toTypedArray()))
            if (earned > 0) {
                put("dexEvoEarned", earned)
                put("dexEvoSpent", spent)
            }
        }
        try {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(fields, SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Local is the working copy; the next change or launch retries.
        }
    }

    /**
     * Brings the account's Pokédex onto this phone and publishes anything only this phone has.
     * With [replaceLocal], this phone's own entries are dropped first: signing in to an existing
     * account must not inherit what a guest built here.
     */
    suspend fun pullAndMerge(uid: String, replaceLocal: Boolean) {
        val snap = try {
            FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return
        }
        fun ids(field: String): Set<Int> =
            (snap.get(field) as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
                ?.filter { it in 1..DexProgress.TOTAL }?.toSet().orEmpty()

        val cloudSeen = ids("dexSeen") + ids("dexCaught")
        val cloudCaught = ids("dexCaught")
        val cloudEvolved = ids("dexEvolved")
        val cloudConsumed = ids("dexConsumed")
        val cloudTiers = (snap.get("dexTiers") as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
        val cloudEarned = (snap.get("dexEvoEarned") as? Number)?.toInt() ?: 0
        val cloudSpent = (snap.get("dexEvoSpent") as? Number)?.toInt() ?: 0
        ds.edit { p ->
            val earned = maxOf(if (replaceLocal) 0 else p[evoEarnedKey] ?: 0, cloudEarned)
            p[evoEarnedKey] = earned
            p[evoSpentKey] = maxOf(if (replaceLocal) 0 else p[evoSpentKey] ?: 0, cloudSpent).coerceAtMost(earned)
            val evolved = (if (replaceLocal) emptySet() else p[evolvedKey].toIds()) + cloudEvolved
            p[evolvedKey] = evolved.mapTo(HashSet()) { it.toString() }
            val seen = (if (replaceLocal) emptySet() else p[seenKey].toIds()) + cloudSeen
            val caught = (if (replaceLocal) emptySet() else p[caughtKey].toIds()) + cloudCaught
            p[seenKey] = seen.mapTo(HashSet()) { it.toString() }
            p[caughtKey] = caught.mapTo(HashSet()) { it.toString() }
            // Evolved-away forms can come back (re-caught), so this one is not a union: this phone's own list
            // wins unless it has none yet (a new phone, or signing in to an existing account).
            val localConsumed = if (replaceLocal) emptySet() else p[consumedKey].toIds()
            val consumed = (if (localConsumed.isNotEmpty()) localConsumed else cloudConsumed).intersect(caught)
            p[consumedKey] = consumed.mapTo(HashSet()) { it.toString() }
            val tiers = (if (replaceLocal) emptySet() else p[tiersKey].orEmpty()) + cloudTiers
            p[tiersKey] = tiers.filterTo(HashSet()) { it.toTierEntry() != null }
            // A restored account already has its own starter; don't hand it a default Pikachu on top.
            if (cloudSeen.isNotEmpty()) p[starterChosenKey] = true
        }
        scheduleSync()
    }

    /** Signing out: the Pokédex belongs to the account, so the next account on this phone starts clean. */
    suspend fun clearLocal() {
        pendingSync?.cancel()
        ds.edit { it.clear() }
    }

    private fun String.toTierEntry(): Pair<Int, CatchTier>? {
        val (id, ord) = split(":").takeIf { it.size == 2 } ?: return null
        return (id.toIntOrNull() ?: return null) to (CatchTier.entries.getOrNull(ord.toIntOrNull() ?: return null) ?: return null)
    }

    private fun Set<String>?.toTiers(): Map<Int, CatchTier> = orEmpty().mapNotNull { it.toTierEntry() }.toMap()

    private fun Set<String>?.toIds(): Set<Int> = orEmpty().mapNotNullTo(HashSet()) { it.toIntOrNull() }

    private companion object {
        const val SYNC_DEBOUNCE_MS = 3_000L
        const val MAX_SHARDS = 900_000
    }
}

class PokedexViewModel(private val repository: PokedexRepository) : ViewModel() {
    val progress: StateFlow<DexProgress> = repository.progress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DexProgress.EMPTY)

    val requestedFilter: StateFlow<DexFilter?> = repository.requestedFilter

    fun requestFilter(filter: DexFilter) {
        repository.requestedFilter.value = filter
    }

    fun consumeRequestedFilter() {
        repository.requestedFilter.value = null
    }

    fun markSeen(id: Int) {
        viewModelScope.launch { repository.markSeen(id) }
    }

    fun markCaught(id: Int, tier: CatchTier? = null) {
        viewModelScope.launch { repository.markCaught(id, tier) }
    }
}

/** National-dex number from a PokeAPI resource url (".../pokemon/25/"), or 0 when it can't be read. */
fun pokemonIdFromUrl(url: String): Int =
    url.trimEnd('/').substringAfterLast('/').toIntOrNull() ?: 0

/** Home filter: everything, only what has been seen, or only what has been caught. */
enum class DexFilter { ALL, SEEN, CAUGHT }

/** Dex number from a PokeAPI sprite url (".../pokemon/25.png"), or null for forms and empty urls. */
fun pokemonIdFromSpriteUrl(url: String): Int? =
    url.substringAfterLast('/').substringBefore('.').toIntOrNull()?.takeIf { it in 1..DexProgress.TOTAL }

/** True when [ids] is a full party and every member has been caught. */
fun isTrainersParty(ids: List<Int>, caught: Set<Int>): Boolean =
    ids.size == DexProgress.PARTY_SIZE && ids.all { it in caught }
