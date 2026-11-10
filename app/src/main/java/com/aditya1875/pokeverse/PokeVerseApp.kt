package com.aditya1875.pokeverse

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.aditya1875.pokeverse.di.appModules
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.feature.widget.streak.StreakWidgetSync
import com.aditya1875.pokeverse.utils.LocaleHelper
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin

class PokeVerseApp : Application(), ImageLoaderFactory {

    // Coil asks for its singleton lazily (first image load, after Koin is up), so every
    // AsyncImage in the app uses the same loader the games prefetch sprites into.
    override fun newImageLoader(): ImageLoader = GlobalContext.get().get()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(base))
    }

    override fun onCreate() {
        super.onCreate()

        // Every build attests itself (Play Integrity in release, debug provider in debug), so
        // Firestore, Storage, Functions and Gemini can tell the real app from scripts and
        // modified APKs. Attestation only *reports* until enforcement is turned on per service
        // in the Firebase console (do that once App Check metrics show ~100% verified traffic).
        AppCheckSetup.install()

        // Billing (direct Play Billing Library, or RevenueCat when that's back in play) is
        // configured inside the play-flavor BillingManager's init{}, this class is compiled
        // for every flavor, so referencing billing SDK classes here would break other flavors.
        val koin = startKoin {
            androidContext(this@PokeVerseApp)
            modules(appModules)
        }.koin

        StreakWidgetSync.start(this, koin.get<UserProfileRepository>())
    }
}
