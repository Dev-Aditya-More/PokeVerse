package com.aditya1875.pokeverse.feature.progression.data

import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeRules
import com.aditya1875.pokeverse.feature.progression.domain.BagRules
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.progression.domain.CoinEconomy
import com.aditya1875.pokeverse.feature.progression.domain.Cosmetic
import com.aditya1875.pokeverse.feature.progression.domain.CosmeticKind
import com.aditya1875.pokeverse.feature.progression.domain.Cosmetics
import com.aditya1875.pokeverse.feature.progression.domain.LevelReward
import com.aditya1875.pokeverse.feature.progression.domain.LevelRewards
import com.aditya1875.pokeverse.feature.progression.domain.ShopItem
import com.aditya1875.pokeverse.feature.progression.domain.ShopItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.progressionStore: DataStore<Preferences> by preferencesDataStore("progression")

/** A player's Dex Coin wallet, collection and equipped look. Account-tied: guests never earn, sign-out clears it. */
data class ProgressionState(
    /** Lifetime coins earned. Only ever goes up, so merging two devices is just `max`. */
    val coinsEarned: Int = 0,
    /** Lifetime coins spent. Only ever goes up, and never passes [coinsEarned]. */
    val coinsSpent: Int = 0,
    val owned: Set<String> = emptySet(),
    val frame: String = "",
    val title: String = "",
    val backdrop: String = "",
    val widget: String = "",
    val freezesBought: Int = 0,
    val freezesUsed: Int = 0,
    /** Lifetime count bought per shop item id (stones, balls, lures). Only ever goes up, so merging is `max`. */
    val itemsBought: Map<String, Int> = emptyMap(),
    /** Lifetime count used per item id. Never passes [itemsBought] for the same id. */
    val itemsUsed: Map<String, Int> = emptyMap(),
    /** How many bag upgrades the player has bought (see [BagRules]). Only ever goes up. */
    val bagLevel: Int = 0,
    /** Highest level whose reward has been paid. */
    val claimedLevel: Int = 0,
    val completionsToday: Int = 0,
    val completionsDate: String = "",
    /** How many owned items the player had when they last opened Rewards (device-local, not synced). */
    val seenOwned: Int = 0,
    /** Challenge rewards already taken (see ChallengeRules): a claim id is paid once, whichever phone claims it. */
    val challengesClaimed: Set<String> = emptySet(),
) {
    /** Cosmetics unlocked since the player last looked, for the "new" dot. */
    val unseen: Int get() = (owned.size - seenOwned).coerceAtLeast(0)

    val balance: Int get() = (coinsEarned - coinsSpent).coerceAtLeast(0)
    val freezes: Int get() = (freezesBought - freezesUsed).coerceAtLeast(0)

    /** How many of shop item [id] the player is holding right now. */
    fun held(id: String): Int = ((itemsBought[id] ?: 0) - (itemsUsed[id] ?: 0)).coerceAtLeast(0)

    /** The most of [item] this player's bag holds. */
    fun capacityOf(item: ShopItem): Int = BagRules.capacity(item, bagLevel)

    fun owns(id: String) = id in owned
    fun equippedId(kind: CosmeticKind): String = when (kind) {
        CosmeticKind.FRAME -> frame
        CosmeticKind.TITLE -> title
        CosmeticKind.BACKDROP -> backdrop
        CosmeticKind.WIDGET -> widget
        CosmeticKind.THEME -> ""
    }
}

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object NotEnoughCoins : PurchaseResult
    data object AlreadyOwned : PurchaseResult
    data object FreezeFull : PurchaseResult
    /** The player already holds as many of this item as they may. */
    data object ItemFull : PurchaseResult
    /** The bag is already at its top level. */
    data object BagMaxed : PurchaseResult
    data object NotForSale : PurchaseResult
}

/** Coins gained by a daily check-in: the flat login coin plus any streak milestone. */
data class DailyCoins(val login: Int, val milestone: Int) {
    val total: Int get() = login + milestone
}

/** What the player won on the weekly leaderboard, as recorded by the Monday Cloud Function. */
data class WeeklyReward(val week: String, val rank: Int, val coins: Int)

class ProgressionRepository(private val context: Context) {

    private val ds = context.progressionStore
    private val firestore get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingSync: Job? = null

    private object K {
        val EARNED = intPreferencesKey("coins_earned")
        val SPENT = intPreferencesKey("coins_spent")
        val OWNED = stringSetPreferencesKey("owned")
        val FRAME = stringPreferencesKey("eq_frame")
        val TITLE = stringPreferencesKey("eq_title")
        val BACKDROP = stringPreferencesKey("eq_backdrop")
        val WIDGET = stringPreferencesKey("eq_widget")
        val FREEZES_BOUGHT = intPreferencesKey("freezes_bought")
        val FREEZES_USED = intPreferencesKey("freezes_used")
        val CLAIMED_LEVEL = intPreferencesKey("claimed_level")
        val COMPLETIONS = intPreferencesKey("completions_today")
        val COMPLETIONS_DATE = stringPreferencesKey("completions_date")
        val SEEN_OWNED = intPreferencesKey("seen_owned")
        val WEEKLY_SEEN = stringPreferencesKey("weekly_reward_seen")
        val BAG_LEVEL = intPreferencesKey("bag_level")
        val CHALLENGES = stringSetPreferencesKey("challenges_claimed")
        const val BOUGHT_PREFIX = "ib_"
        const val USED_PREFIX = "iu_"
        fun bought(id: String) = intPreferencesKey(BOUGHT_PREFIX + id)
        fun used(id: String) = intPreferencesKey(USED_PREFIX + id)
    }

    private val _unseenWeeklyReward = MutableStateFlow<WeeklyReward?>(null)

    /** The latest weekly prize the player hasn't been shown yet, or null. */
    val unseenWeeklyReward: StateFlow<WeeklyReward?> = _unseenWeeklyReward.asStateFlow()

    suspend fun markWeeklyRewardSeen() {
        val week = _unseenWeeklyReward.value?.week ?: return
        ds.edit { it[K.WEEKLY_SEEN] = week }
        _unseenWeeklyReward.value = null
    }

    val state: Flow<ProgressionState> = ds.data.map { it.toState() }

    /** Per-item counters stored as `ib_<id>` / `iu_<id>`; ids no longer in the catalogue are dropped. */
    private fun Preferences.itemCounts(prefix: String): Map<String, Int> =
        asMap().entries.mapNotNull { (key, value) ->
            val id = key.name.removePrefix(prefix).takeIf { key.name.startsWith(prefix) }
            if (id != null && value is Int && ShopItems.find(id) != null) id to value else null
        }.toMap()

    private fun Preferences.toState() = ProgressionState(
        coinsEarned = this[K.EARNED] ?: 0,
        coinsSpent = this[K.SPENT] ?: 0,
        owned = (this[K.OWNED] ?: emptySet()).filter { Cosmetics.find(it) != null }.toSet(),
        // An id that was removed from the catalogue must not stay equipped.
        frame = known(this[K.FRAME]),
        title = known(this[K.TITLE]),
        backdrop = known(this[K.BACKDROP]),
        widget = known(this[K.WIDGET]),
        freezesBought = this[K.FREEZES_BOUGHT] ?: 0,
        freezesUsed = this[K.FREEZES_USED] ?: 0,
        itemsBought = itemCounts(K.BOUGHT_PREFIX),
        itemsUsed = itemCounts(K.USED_PREFIX),
        bagLevel = (this[K.BAG_LEVEL] ?: 0).coerceIn(0, BagRules.MAX_LEVEL),
        claimedLevel = this[K.CLAIMED_LEVEL] ?: 0,
        completionsToday = this[K.COMPLETIONS] ?: 0,
        completionsDate = this[K.COMPLETIONS_DATE] ?: "",
        seenOwned = this[K.SEEN_OWNED] ?: 0,
        challengesClaimed = this[K.CHALLENGES] ?: emptySet()
    )

    private fun known(id: String?): String = id?.takeIf { Cosmetics.find(it) != null } ?: ""

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    suspend fun current(): ProgressionState = state.first()

    // ── Earning ──────────────────────────────────────────────────────────────

    /** Pays the coins [event] is worth (respecting the daily completion cap); returns how many. */
    suspend fun earnForEvent(event: XPEvent): Int {
        var paid = 0
        ds.edit { p ->
            val today = today()
            val done = if (p[K.COMPLETIONS_DATE] == today) p[K.COMPLETIONS] ?: 0 else 0
            val award = CoinEconomy.forEvent(event, done)
            if (award.coins <= 0) return@edit
            p[K.EARNED] = (p[K.EARNED] ?: 0) + award.coins
            if (award.countsAsCompletion) {
                p[K.COMPLETIONS] = done + 1
                p[K.COMPLETIONS_DATE] = today
            }
            paid = award.coins
        }
        if (paid > 0) scheduleSync()
        return paid
    }

    /** The coin for showing up, plus a milestone bonus if [streak] just hit one. */
    suspend fun earnForDailyLogin(streak: Int): DailyCoins {
        val coins = DailyCoins(CoinEconomy.DAILY_LOGIN, CoinEconomy.streakMilestone(streak))
        ds.edit { p -> p[K.EARNED] = (p[K.EARNED] ?: 0) + coins.total }
        scheduleSync()
        return coins
    }

    /**
     * Pays every level reward up to [level] that hasn't been paid yet, and unlocks its cosmetics.
     * Idempotent, so it also catches up players who levelled before this system existed.
     */
    suspend fun claimLevelRewards(level: Int): List<LevelReward> {
        var granted: List<LevelReward> = emptyList()
        var changed = false
        ds.edit { p ->
            // Cosmetics are re-checked on every call, not just when a level is newly claimed: an item added
            // to an already-passed level (or renamed) would otherwise stay locked for veterans forever.
            val owned = p[K.OWNED] ?: emptySet()
            val missing = Cosmetics.earnedByLevel(level).filter { it !in owned }
            if (missing.isNotEmpty()) {
                p[K.OWNED] = owned + missing
                changed = true
            }

            val claimed = p[K.CLAIMED_LEVEL] ?: 0
            if (level <= claimed) return@edit
            granted = LevelRewards.between(claimed, level)
            changed = true
            p[K.EARNED] = (p[K.EARNED] ?: 0) + granted.sumOf { it.coins }
            fun addItem(id: String, n: Int) {
                if (n > 0) p[K.bought(id)] = (p[K.bought(id)] ?: 0) + n
            }
            for (r in granted) {
                addItem(ShopItems.ID_GREAT_BALL, r.great)
                addItem(ShopItems.ID_ULTRA_BALL, r.ultra)
                addItem(ShopItems.ID_LURE, r.lures)
                r.stone?.let { addItem(it, 1) }
                // A Streak Freeze only fits while the player holds fewer than the most they may.
                if (r.freezes > 0 && (p[K.FREEZES_BOUGHT] ?: 0) - (p[K.FREEZES_USED] ?: 0) < CoinEconomy.FREEZE_MAX) {
                    p[K.FREEZES_BOUGHT] = (p[K.FREEZES_BOUGHT] ?: 0) + 1
                }
            }
            val newIds = granted.flatMap { it.cosmeticIds }
            if (newIds.isNotEmpty()) p[K.OWNED] = (p[K.OWNED] ?: emptySet()) + newIds
            p[K.CLAIMED_LEVEL] = level
        }
        if (changed) scheduleSync()
        return granted
    }

    // ── Streak protection ────────────────────────────────────────────────────

    suspend fun useFreezes(count: Int) {
        if (count <= 0) return
        ds.edit { p -> p[K.FREEZES_USED] = (p[K.FREEZES_USED] ?: 0) + count }
        scheduleSync()
    }

    suspend fun buyFreeze(): PurchaseResult {
        var result: PurchaseResult = PurchaseResult.Success
        ds.edit { p ->
            val held = (p[K.FREEZES_BOUGHT] ?: 0) - (p[K.FREEZES_USED] ?: 0)
            val balance = (p[K.EARNED] ?: 0) - (p[K.SPENT] ?: 0)
            result = when {
                held >= CoinEconomy.FREEZE_MAX -> PurchaseResult.FreezeFull
                balance < CoinEconomy.FREEZE_PRICE -> PurchaseResult.NotEnoughCoins
                else -> {
                    p[K.SPENT] = (p[K.SPENT] ?: 0) + CoinEconomy.FREEZE_PRICE
                    p[K.FREEZES_BOUGHT] = (p[K.FREEZES_BOUGHT] ?: 0) + 1
                    PurchaseResult.Success
                }
            }
        }
        if (result == PurchaseResult.Success) scheduleSync()
        return result
    }

    // ── Items (stones, balls, lures) ─────────────────────────────────────────

    /**
     * Buys one pack of [item]. A shard pouch only spends the coins (the caller grants the shards), every
     * other item lands in the inventory. Refuses when the player already holds the most they may.
     */
    suspend fun buyItem(item: ShopItem): PurchaseResult {
        var result: PurchaseResult = PurchaseResult.Success
        ds.edit { p ->
            val balance = (p[K.EARNED] ?: 0) - (p[K.SPENT] ?: 0)
            val held = (p[K.bought(item.id)] ?: 0) - (p[K.used(item.id)] ?: 0)
            val capacity = BagRules.capacity(item, p[K.BAG_LEVEL] ?: 0)
            result = when {
                item.isInventory && held + item.quantity > capacity -> PurchaseResult.ItemFull
                balance < item.price -> PurchaseResult.NotEnoughCoins
                else -> {
                    p[K.SPENT] = (p[K.SPENT] ?: 0) + item.price
                    if (item.isInventory) p[K.bought(item.id)] = (p[K.bought(item.id)] ?: 0) + item.quantity
                    PurchaseResult.Success
                }
            }
        }
        if (result == PurchaseResult.Success) scheduleSync()
        return result
    }

    /**
     * Pays a challenge's balls, once. [claimId] is remembered, so a second tap, a second phone or a retry after a
     * crash cannot pay it again. Rewards are not held to the bag limit (a prize must never be lost to a full bag);
     * the limit only gates buying. Returns true only when this call paid.
     */
    suspend fun claimChallenge(
        claimId: String,
        great: Int,
        ultra: Int,
        today: String = today(),
        /** Dex Coins paid by a long-term goal. */
        coins: Int = 0,
        /** A cosmetic id paid by a long-term goal (a title). */
        cosmetic: String? = null,
    ): Boolean {
        var paid = false
        ds.edit { p ->
            val claimed = p[K.CHALLENGES] ?: emptySet()
            if (claimId in claimed) return@edit
            if (great > 0) p[K.bought(ShopItems.ID_GREAT_BALL)] = (p[K.bought(ShopItems.ID_GREAT_BALL)] ?: 0) + great
            if (ultra > 0) p[K.bought(ShopItems.ID_ULTRA_BALL)] = (p[K.bought(ShopItems.ID_ULTRA_BALL)] ?: 0) + ultra
            if (coins > 0) p[K.EARNED] = (p[K.EARNED] ?: 0) + coins
            if (cosmetic != null && Cosmetics.find(cosmetic) != null) p[K.OWNED] = (p[K.OWNED] ?: emptySet()) + cosmetic
            p[K.CHALLENGES] = ChallengeRules.prune(claimed + claimId, today)
            paid = true
        }
        if (paid) scheduleSync()
        return paid
    }

    /** Uses up one [id]. False (and nothing changes) when the player has none. */
    suspend fun useItem(id: String): Boolean {
        var used = false
        ds.edit { p ->
            val held = (p[K.bought(id)] ?: 0) - (p[K.used(id)] ?: 0)
            if (held > 0) {
                p[K.used(id)] = (p[K.used(id)] ?: 0) + 1
                used = true
            }
        }
        if (used) scheduleSync()
        return used
    }

    /** Buys the next bag upgrade, which raises every item's carrying limit. */
    suspend fun upgradeBag(): PurchaseResult {
        var result: PurchaseResult = PurchaseResult.Success
        ds.edit { p ->
            val level = (p[K.BAG_LEVEL] ?: 0).coerceIn(0, BagRules.MAX_LEVEL)
            val price = BagRules.upgradePrice(level)
            val balance = (p[K.EARNED] ?: 0) - (p[K.SPENT] ?: 0)
            result = when {
                price == null -> PurchaseResult.BagMaxed
                balance < price -> PurchaseResult.NotEnoughCoins
                else -> {
                    p[K.SPENT] = (p[K.SPENT] ?: 0) + price
                    p[K.BAG_LEVEL] = level + 1
                    PurchaseResult.Success
                }
            }
        }
        if (result == PurchaseResult.Success) scheduleSync()
        return result
    }

    /** Gives back an item [useItem] took when the thing it was spent on then failed. */
    suspend fun refundItem(id: String) {
        ds.edit { p ->
            val usedNow = p[K.used(id)] ?: 0
            if (usedNow > 0) p[K.used(id)] = usedNow - 1
        }
        scheduleSync()
    }

    // ── Shop ─────────────────────────────────────────────────────────────────

    suspend fun purchase(item: Cosmetic): PurchaseResult {
        val price = item.price ?: return PurchaseResult.NotForSale
        var result: PurchaseResult = PurchaseResult.Success
        ds.edit { p ->
            val owned = p[K.OWNED] ?: emptySet()
            val balance = (p[K.EARNED] ?: 0) - (p[K.SPENT] ?: 0)
            result = when {
                item.id in owned -> PurchaseResult.AlreadyOwned
                balance < price -> PurchaseResult.NotEnoughCoins
                else -> {
                    p[K.SPENT] = (p[K.SPENT] ?: 0) + price
                    p[K.OWNED] = owned + item.id
                    PurchaseResult.Success
                }
            }
        }
        if (result == PurchaseResult.Success) scheduleSync()
        return result
    }

    /** Equips an owned cosmetic, or clears the slot when [id] is empty. Themes aren't "equipped" here. */
    suspend fun equip(kind: CosmeticKind, id: String) {
        val key = when (kind) {
            CosmeticKind.FRAME -> K.FRAME
            CosmeticKind.TITLE -> K.TITLE
            CosmeticKind.BACKDROP -> K.BACKDROP
            CosmeticKind.WIDGET -> K.WIDGET
            CosmeticKind.THEME -> return
        }
        ds.edit { p ->
            if (id.isEmpty()) p.remove(key)
            else if (id in (p[K.OWNED] ?: emptySet()) && Cosmetics.find(id)?.kind == kind) p[key] = id
        }
        scheduleSync()
    }

    suspend fun markOwnedSeen() {
        ds.edit { p -> p[K.SEEN_OWNED] = (p[K.OWNED] ?: emptySet()).size }
    }

    // ── Cloud ────────────────────────────────────────────────────────────────

    /** Debounced like XP: a burst of changes becomes one write of the latest state. */
    private fun scheduleSync() {
        pendingSync?.cancel()
        pendingSync = syncScope.launch {
            delay(SYNC_DEBOUNCE_MS)
            pushToFirestore()
        }
    }

    private suspend fun pushToFirestore() {
        val uid = auth.currentUser?.uid ?: return
        // Merge what the account already holds before writing: this map replaces the cloud's, so a phone that
        // never pulled (new install, offline start) would otherwise wipe coins, cosmetics and items bought
        // elsewhere. If the cloud can't be read, don't write; the next change or launch tries again.
        val cloud = try {
            com.aditya1875.pokeverse.utils.UserDocCache.read(uid).get("progression") as? Map<*, *>
        } catch (_: Exception) {
            return
        }
        if (cloud != null) mergeCloud(cloud)
        val s = current()
        try {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "progression" to mapOf(
                        "earned" to s.coinsEarned,
                        "spent" to s.coinsSpent,
                        "owned" to s.owned.sorted(),
                        "frame" to s.frame,
                        "title" to s.title,
                        "backdrop" to s.backdrop,
                        "widget" to s.widget,
                        "freezesBought" to s.freezesBought,
                        "freezesUsed" to s.freezesUsed,
                        "itemsBought" to s.itemsBought,
                        "itemsUsed" to s.itemsUsed,
                        "bagLevel" to s.bagLevel,
                        "claimedLevel" to s.claimedLevel,
                        "challengesClaimed" to s.challengesClaimed.sorted()
                    )
                ),
                SetOptions.merge()
            ).await()
            // The only parts other players see: the equipped frame, title and backdrop.
            firestore.collection("leaderboard").document(uid)
                .set(mapOf("frame" to s.frame, "title" to s.title, "backdrop" to s.backdrop), SetOptions.merge()).await()
        } catch (_: Exception) {
            // Local state is the source of truth; the next change or launch retries.
        }
    }

    /**
     * Merges the cloud copy into the local one. Counters take the larger value and owned items are
     * unioned, so a stale device can never roll anything back or double-pay coins. Safe to call repeatedly.
     */
    suspend fun pullAndMerge(uid: String) {
        val snapshot = try {
            com.aditya1875.pokeverse.utils.UserDocCache.read(uid)
        } catch (_: Exception) {
            return
        }
        // The Monday payout lands in `progression.earned` (merged below) and is described here.
        (snapshot.get("weeklyReward") as? Map<*, *>)?.let { wr ->
            val week = wr["week"] as? String
            val coins = (wr["coins"] as? Number)?.toInt() ?: 0
            val rank = (wr["rank"] as? Number)?.toInt() ?: 0
            if (week != null && coins > 0 && rank > 0 && week != ds.data.first()[K.WEEKLY_SEEN]) {
                _unseenWeeklyReward.value = WeeklyReward(week, rank, coins)
            }
        }

        val cloud = snapshot.get("progression") as? Map<*, *>
        if (cloud == null) {
            // Nothing in the cloud yet: publish whatever this device has.
            scheduleSync()
            return
        }
        mergeCloud(cloud)
        scheduleSync()
    }

    /** Folds the cloud wallet into this device: counters keep the larger value, owned sets are unions. */
    private suspend fun mergeCloud(cloud: Map<*, *>) {
        fun int(key: String) = (cloud[key] as? Number)?.toInt() ?: 0
        fun str(key: String) = cloud[key] as? String ?: ""
        fun cloudCounts(key: String): Map<String, Int> =
            (cloud[key] as? Map<*, *>)?.entries?.mapNotNull { (k, v) ->
                (k as? String)?.let { id -> (v as? Number)?.toInt()?.let { id to it } }
            }?.toMap().orEmpty()
        val cloudOwned = (cloud["owned"] as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()

        ds.edit { p ->
            val earned = maxOf(p[K.EARNED] ?: 0, int("earned"))
            p[K.EARNED] = earned
            p[K.SPENT] = maxOf(p[K.SPENT] ?: 0, int("spent")).coerceAtMost(earned)
            p[K.OWNED] = (p[K.OWNED] ?: emptySet()) + cloudOwned.filter { Cosmetics.find(it) != null }
            p[K.FREEZES_BOUGHT] = maxOf(p[K.FREEZES_BOUGHT] ?: 0, int("freezesBought"))
            p[K.FREEZES_USED] = maxOf(p[K.FREEZES_USED] ?: 0, int("freezesUsed"))
            // Per-item counters: larger value wins, and "used" can never pass "bought".
            val cloudBought = cloudCounts("itemsBought")
            val cloudUsed = cloudCounts("itemsUsed")
            for (item in ShopItems.all.filter { it.isInventory }) {
                val bought = maxOf(p[K.bought(item.id)] ?: 0, cloudBought[item.id] ?: 0)
                if (bought > 0) p[K.bought(item.id)] = bought
                val used = maxOf(p[K.used(item.id)] ?: 0, cloudUsed[item.id] ?: 0).coerceAtMost(bought)
                if (used > 0) p[K.used(item.id)] = used
            }
            p[K.BAG_LEVEL] = maxOf(p[K.BAG_LEVEL] ?: 0, int("bagLevel")).coerceIn(0, BagRules.MAX_LEVEL)
            p[K.CLAIMED_LEVEL] = maxOf(p[K.CLAIMED_LEVEL] ?: 0, int("claimedLevel"))
            val cloudClaims = (cloud["challengesClaimed"] as? List<*>)?.filterIsInstance<String>().orEmpty()
            p[K.CHALLENGES] = ChallengeRules.prune((p[K.CHALLENGES] ?: emptySet()) + cloudClaims, today())
            // What's equipped is a preference, not a counter: keep this device's choice, fill gaps from the cloud.
            fun fill(key: Preferences.Key<String>, cloudValue: String) {
                if (p[key].isNullOrEmpty() && cloudValue.isNotEmpty()) p[key] = cloudValue
            }
            fill(K.FRAME, str("frame"))
            fill(K.TITLE, str("title"))
            fill(K.BACKDROP, str("backdrop"))
            fill(K.WIDGET, str("widget"))
        }
    }

    suspend fun clearLocal() {
        pendingSync?.cancel()
        ds.edit { it.clear() }
    }

    private companion object {
        const val SYNC_DEBOUNCE_MS = 3_000L
    }
}
