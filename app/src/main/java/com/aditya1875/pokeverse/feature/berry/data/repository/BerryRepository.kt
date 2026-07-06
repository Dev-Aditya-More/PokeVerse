package com.aditya1875.pokeverse.feature.berry.data.repository

import com.aditya1875.pokeverse.feature.berry.data.source.remote.BerryApiService
import com.aditya1875.pokeverse.feature.berry.data.source.remote.model.BerryUiModel
import com.aditya1875.pokeverse.feature.berry.data.source.remote.model.toUiModel

class BerryRepository(private val api: BerryApiService) {

    private var cachedBerries: List<BerryUiModel> = emptyList()

    suspend fun getBerries(forceRefresh: Boolean = false): Result<List<BerryUiModel>> {
        if (!forceRefresh && cachedBerries.isNotEmpty()) return Result.success(cachedBerries)
        return try {
            val response = api.getBerries(offset = 0, limit = 64)
            val berries = response.results.mapNotNull { res ->
                try { api.getBerryDetail(res.name).toUiModel() } catch (_: Exception) { null }
            }
            cachedBerries = berries
            Result.success(berries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBerryByName(name: String): BerryUiModel? = cachedBerries.firstOrNull { it.name == name }

    fun searchBerries(query: String): List<BerryUiModel> {
        if (query.isBlank()) return cachedBerries
        val q = query.lowercase()
        return cachedBerries.filter {
            it.displayName.lowercase().contains(q) ||
                it.naturalGiftType.lowercase().contains(q) ||
                it.firmness.lowercase().contains(q) ||
                it.dominantFlavor.lowercase().contains(q)
        }
    }
}
