package com.aditya1875.pokeverse.di

import coil.disk.DiskCache
import coil.memory.MemoryCache
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.PokemonDetailsApi
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.PokemonListApi
import com.aditya1875.pokeverse.feature.berry.data.source.remote.BerryApiService
import com.aditya1875.pokeverse.feature.item.data.source.remote.model.ItemApiService
import com.aditya1875.pokeverse.utils.ConnectivityObserver
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Pokémon data barely changes, so when a request fails for lack of a connection, answer from the disk
 * cache (however old) instead of failing: every Pokémon, move and evolution the player has already
 * opened keeps working on a plane. If nothing is cached the original error is rethrown unchanged, so
 * the screen still reports the real reason (offline, timeout...).
 */
private class OfflineCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (request.method != "GET") throw e
            val stale = request.newBuilder()
                .cacheControl(CacheControl.Builder().onlyIfCached().maxStale(Int.MAX_VALUE, TimeUnit.SECONDS).build())
                .build()
            val cached = try { chain.proceed(stale) } catch (_: IOException) { null }
            // Only-if-cached with nothing cached answers 504; that is a miss, not data.
            if (cached != null && cached.code != 504) cached else { cached?.close(); throw e }
        }
    }
}

val networkModule = module {

    // OkHttp's default Dispatcher caps concurrent requests to the SAME host at 5 -
    // question generators fetch a whole batch of Pokémon concurrently (up to ~40
    // for a 10-question game), so the default cap silently serialized most of that
    // batch back down to 5-at-a-time. Raising it plus a disk cache (PokeAPI sends
    // long-lived Cache-Control headers since Pokémon data barely changes) are what
    // actually make the concurrent batch fetches fast in practice.
    single {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 32
        }
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .cache(Cache(File(androidContext().cacheDir, "http_cache"), 20L * 1024 * 1024))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            // Nothing should hang past this, whatever the phase that stalls.
            .callTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(OfflineCacheInterceptor())
            .build()
    }

    single(named("pokeapi")) {
        Retrofit.Builder()
            .baseUrl("https://pokeapi.co/api/v2/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<PokemonListApi> {
        get<Retrofit>(named("pokeapi")).create(PokemonListApi::class.java)
    }

    single<PokemonDetailsApi> {
        get<Retrofit>(named("pokeapi")).create(PokemonDetailsApi::class.java)
    }

    single { get<Retrofit>(named("pokeapi")).create(ItemApiService::class.java) }
    single { get<Retrofit>(named("pokeapi")).create(BerryApiService::class.java) }

    // The ONE image pipeline: PokeVerseApp hands this to Coil as its singleton, so plain
    // AsyncImage calls and the games' sprite prefetches share a memory cache (before, the
    // prefetches warmed this loader while screens drew from Coil's separate default one).
    // No global crossfade, call sites opt in per request, matching Coil's default.
    //
    // Tuned for sprites, which all come from one host (GitHub): the shared OkHttp client's connection
    // pool and 32 parallel requests per host (Coil's own default client allows 5, so a screen full of
    // sprites queued behind a handful of slots), a 200 MB disk cache so a sprite is downloaded once ever,
    // and respectCacheHeaders(false) because GitHub marks sprites as fresh for only 5 minutes, which
    // made Coil re-check the network for the same unchanging image over and over.
    single {
        val ctx = androidContext()
        val imageClient = get<OkHttpClient>().newBuilder()
            .cache(null) // Coil's disk cache does this job
            .apply { interceptors().clear() }
            .build()
        ImageLoader.Builder(ctx)
            .okHttpClient(imageClient)
            .memoryCache { MemoryCache.Builder(ctx).maxSizePercent(0.25).build() }
            .diskCache { DiskCache.Builder().directory(File(ctx.cacheDir, "sprite_cache")).maxSizeBytes(200L * 1024 * 1024).build() }
            .respectCacheHeaders(false)
            .build()
    }

    // Animated sprites (Showdown GIFs). newBuilder() shares the memory/disk cache and HTTP
    // client above; ImageDecoder only exists on API 28+, so older devices use GifDecoder.
    single(named("gif")) {
        get<ImageLoader>().newBuilder()
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }

    single { ConnectivityObserver(androidContext()) }
}
