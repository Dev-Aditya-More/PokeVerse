package com.aditya1875.pokeverse

import android.app.Application
import android.content.Context
import com.aditya1875.pokeverse.di.appModules
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.feature.widget.streak.StreakWidgetSync
import com.aditya1875.pokeverse.utils.Features
import com.aditya1875.pokeverse.utils.LocaleHelper
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class PokeVerseApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(base))
    }

    override fun onCreate() {
        super.onCreate()

        // Only the Coach (Gemini) needs App Check; skip it entirely while that feature is off.
        if (Features.COACH_ENABLED) AppCheckSetup.install()

        // Billing (direct Play Billing Library, or RevenueCat when that's back in play) is
        // configured inside the play-flavor BillingManager's init{} — this class is compiled
        // for every flavor, so referencing billing SDK classes here would break other flavors.
        val koin = startKoin {
            androidContext(this@PokeVerseApp)
            modules(appModules)
        }.koin

        StreakWidgetSync.start(this, koin.get<UserProfileRepository>())
    }
}
