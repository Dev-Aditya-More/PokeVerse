package com.aditya1875.pokeverse.feature.progression.presentation

import com.aditya1875.pokeverse.feature.progression.domain.BagRules
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import com.aditya1875.pokeverse.feature.progression.domain.ShopItem
import com.aditya1875.pokeverse.feature.progression.data.ProgressionRepository
import com.aditya1875.pokeverse.feature.progression.data.ProgressionState
import com.aditya1875.pokeverse.feature.progression.data.PurchaseResult
import com.aditya1875.pokeverse.feature.progression.domain.Cosmetic
import com.aditya1875.pokeverse.feature.progression.domain.CosmeticKind
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-shot messages for the snackbar. The screen turns these into text in the user's language. */
sealed interface RewardsMessage {
    data class Bought(val item: Cosmetic) : RewardsMessage
    data object FreezeBought : RewardsMessage
    data object FreezeFull : RewardsMessage
    data object NotEnoughCoins : RewardsMessage
    data class ItemBought(val item: ShopItem) : RewardsMessage
    /** The bag can't take more of [item]: [held] of [capacity]. [canUpgrade] is whether a bigger bag is on sale. */
    data class ItemFull(val item: ShopItem, val held: Int, val capacity: Int, val canUpgrade: Boolean) : RewardsMessage
    data class BagUpgraded(val level: Int) : RewardsMessage
    data object BagMaxed : RewardsMessage
}

class RewardsViewModel(
    private val progression: ProgressionRepository,
    private val pokedex: PokedexRepository,
    profileRepository: UserProfileRepository,
) : ViewModel() {

    val state: StateFlow<ProgressionState> =
        progression.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressionState())

    val profile: StateFlow<UserProfile> =
        profileRepository.profileFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile.GUEST)

    private val _messages = MutableSharedFlow<RewardsMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<RewardsMessage> = _messages.asSharedFlow()

    /** Clears the "new" dot on the Profile entry. */
    fun markSeen() {
        viewModelScope.launch { progression.markOwnedSeen() }
    }

    fun buy(item: Cosmetic) {
        viewModelScope.launch {
            when (progression.purchase(item)) {
                PurchaseResult.Success -> {
                    // Equip right away: buying a look and having to hunt for the equip button is a dud moment.
                    // Themes are applied from the theme screen, so they're left alone.
                    if (item.kind != CosmeticKind.THEME) progression.equip(item.kind, item.id)
                    _messages.emit(RewardsMessage.Bought(item))
                }
                PurchaseResult.NotEnoughCoins -> _messages.emit(RewardsMessage.NotEnoughCoins)
                else -> Unit
            }
        }
    }

    /** Buys a stone, ball, lure or shard pouch. A pouch pays out in Evo Shards straight away. */
    fun buyItem(item: ShopItem) {
        viewModelScope.launch {
            when (progression.buyItem(item)) {
                PurchaseResult.Success -> {
                    if (item.shards > 0) pokedex.grantPurchasedShards(item.shards)
                    _messages.emit(RewardsMessage.ItemBought(item))
                }
                PurchaseResult.ItemFull -> {
                    val s = progression.current()
                    _messages.emit(RewardsMessage.ItemFull(item, s.held(item.id), s.capacityOf(item), s.bagLevel < BagRules.MAX_LEVEL))
                }
                PurchaseResult.NotEnoughCoins -> _messages.emit(RewardsMessage.NotEnoughCoins)
                else -> Unit
            }
        }
    }

    /** Buys the next bag upgrade. */
    fun upgradeBag() {
        viewModelScope.launch {
            _messages.emit(
                when (progression.upgradeBag()) {
                    PurchaseResult.Success -> RewardsMessage.BagUpgraded(progression.current().bagLevel)
                    PurchaseResult.BagMaxed -> RewardsMessage.BagMaxed
                    else -> RewardsMessage.NotEnoughCoins
                }
            )
        }
    }

    fun buyFreeze() {
        viewModelScope.launch {
            _messages.emit(
                when (progression.buyFreeze()) {
                    PurchaseResult.Success -> RewardsMessage.FreezeBought
                    PurchaseResult.FreezeFull -> RewardsMessage.FreezeFull
                    else -> RewardsMessage.NotEnoughCoins
                }
            )
        }
    }

    fun equip(item: Cosmetic) {
        viewModelScope.launch { progression.equip(item.kind, item.id) }
    }

    fun unequip(kind: CosmeticKind) {
        viewModelScope.launch { progression.equip(kind, "") }
    }
}
