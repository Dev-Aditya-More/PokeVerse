package com.aditya1875.pokeverse.di

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
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

val networkModule = module {

    // OkHttp's default Dispatcher caps concurrent requests to the SAME host at 5 —
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
    // No global crossfade — call sites opt in per request, matching Coil's default.
    single { ImageLoader.Builder(androidContext()).build() }

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
