package com.aditya1875.pokeverse.data.repository

import com.aditya1875.pokeverse.data.remote.model.itemModels.ItemDetail
import com.aditya1875.pokeverse.data.remote.model.itemModels.ItemListResponse
import com.aditya1875.pokeverse.data.remote.model.itemModels.ItemUiModel
import com.aditya1875.pokeverse.data.remote.model.itemModels.toUiModel
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ── Retrofit API interface ────────────────────────────────────────────────────
interface ItemApiService {
    @GET("item")
    suspend fun getItems(
        @Query("offset") offset: Int = 0,
        @Query("limit")  limit: Int  = 40
    ): ItemListResponse

    @GET("item/{nameOrId}")
    suspend fun getItemDetail(@Path("nameOrId") nameOrId: String): ItemDetail
}

// ── Repository ────────────────────────────────────────────────────────────────
class ItemRepository(private val api: ItemApiService) {

    private val PAGE_SIZE = 40

    // In-memory cache — lives for the session
    private val detailCache = HashMap<String, ItemUiModel>(200)
    private var cachedList: List<ItemUiModel> = emptyList()
    private var totalCount: Int = 0
    private var loadedOffset: Int = 0

    sealed class ItemListResult {
        data class Success(
            val items: List<ItemUiModel>,
            val canLoadMore: Boolean
        ) : ItemListResult()
        data class Error(val message: String) : ItemListResult()
    }

    sealed class ItemDetailResult {
        data class Success(val item: ItemUiModel) : ItemDetailResult()
        data class Error(val message: String) : ItemDetailResult()
    }

    // ── Load first page (or return cache) ─────────────────────────────────────
    suspend fun getItems(forceRefresh: Boolean = false): ItemListResult {
        if (!forceRefresh && cachedList.isNotEmpty()) {
            return ItemListResult.Success(
                items = cachedList,
                canLoadMore = cachedList.size < totalCount
            )
        }
        return fetchPage(offset = 0, reset = true)
    }

    // ── Load next page ────────────────────────────────────────────────────────
    suspend fun loadMore(): ItemListResult {
        if (cachedList.size >= totalCount && totalCount > 0) {
            return ItemListResult.Success(cachedList, canLoadMore = false)
        }
        return fetchPage(offset = loadedOffset, reset = false)
    }

    private suspend fun fetchPage(offset: Int, reset: Boolean): ItemListResult {
        return try {
            val response = api.getItems(offset = offset, limit = PAGE_SIZE)
            totalCount = response.count

            // Fetch details for this page in parallel-ish batches of 8
            val newItems = mutableListOf<ItemUiModel>()
            response.results.chunked(8).forEach { chunk ->
                chunk.map { namedRes ->
                    val cached = detailCache[namedRes.name]
                    if (cached != null) {
                        newItems.add(cached)
                    } else {
                        try {
                            val detail = api.getItemDetail(namedRes.name)
                            val ui = detail.toUiModel()
                            detailCache[namedRes.name] = ui
                            newItems.add(ui)
                        } catch (e: Exception) {
                            // Skip items that fail (some have missing data)
                        }
                    }
                }
            }

            if (reset) {
                cachedList = newItems
                loadedOffset = PAGE_SIZE
            } else {
                cachedList = cachedList + newItems
                loadedOffset += PAGE_SIZE
            }

            ItemListResult.Success(
                items = cachedList,
                canLoadMore = cachedList.size < totalCount
            )
        } catch (e: Exception) {
            ItemListResult.Error(e.message ?: "Failed to load items")
        }
    }

    // ── Get single item detail ────────────────────────────────────────────────
    suspend fun getItemDetail(nameOrId: String): ItemDetailResult {
        detailCache[nameOrId]?.let { return ItemDetailResult.Success(it) }
        return try {
            val detail = api.getItemDetail(nameOrId)
            val ui = detail.toUiModel()
            detailCache[nameOrId] = ui
            ItemDetailResult.Success(ui)
        } catch (e: Exception) {
            ItemDetailResult.Error(e.message ?: "Failed to load item")
        }
    }

    // ── Search within loaded items (client-side) ──────────────────────────────
    fun searchItems(query: String): List<ItemUiModel> {
        if (query.isBlank()) return cachedList
        val q = query.lowercase()
        return cachedList.filter {
            it.displayName.lowercase().contains(q) ||
                    it.categoryDisplay.lowercase().contains(q) ||
                    it.effect.lowercase().contains(q)
        }
    }
}