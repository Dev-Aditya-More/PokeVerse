package com.aditya1875.pokeverse.feature.team.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.team.data.local.dao.FavouritesDao
import com.aditya1875.pokeverse.feature.team.data.local.entity.FavouriteEntity
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonResult
import com.aditya1875.pokeverse.feature.team.domain.usecase.AddToFavoritesUseCase
import com.aditya1875.pokeverse.feature.team.domain.usecase.RemoveFromFavoritesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavouritesViewModel(
    private val addToFavoritesUseCase: AddToFavoritesUseCase,
    private val removeFromFavoritesUseCase: RemoveFromFavoritesUseCase,
    private val favouritesDao: FavouritesDao
) : ViewModel() {

    val favourites: StateFlow<List<FavouriteEntity>> =
        favouritesDao.getAllFavorites()
            .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    fun addToFavorites(pokemonResult: PokemonResult) {
        viewModelScope.launch {
            addToFavoritesUseCase(pokemonResult.name)
        }
    }

    fun removeFromFavorites(favorite: FavouriteEntity) {
        viewModelScope.launch {
            removeFromFavoritesUseCase(favorite.name)
        }
    }

    fun removeFromFavoritesByName(name: String) {
        viewModelScope.launch {
            removeFromFavoritesUseCase(name)
        }
    }

    fun isInFavorites(name: String): Flow<Boolean> {
        return favouritesDao.isInFavorites(name)
    }
}