package com.aditya1875.pokeverse.feature.team.domain.usecase

import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.team.data.local.dao.FavouritesDao
import com.aditya1875.pokeverse.feature.team.data.local.entity.FavouriteEntity

class AddToFavoritesUseCase(
    private val repo: PokemonDetailRepo,
    private val favouritesDao: FavouritesDao
) {
    suspend operator fun invoke(name: String) {
        val pokemon = repo.getPokemonByName(name)

        val entity = FavouriteEntity(
            name = pokemon.name,
            imageUrl = pokemon.sprites.front_default ?: ""
        )

        favouritesDao.addToFavorites(entity)
    }
}