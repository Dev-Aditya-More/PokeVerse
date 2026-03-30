package com.aditya1875.pokeverse.feature.team.domain.usecase

import com.aditya1875.pokeverse.feature.team.data.local.dao.FavouritesDao

class RemoveFromFavoritesUseCase(
    private val favouritesDao: FavouritesDao
) {
    suspend operator fun invoke(name: String) {
        favouritesDao.removeFromFavoritesByName(name)
    }
}