package com.aditya1875.pokeverse

import android.app.Application
import android.content.Context
import com.aditya1875.pokeverse.di.appModules
import com.aditya1875.pokeverse.utils.LocaleHelper
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class PokeVerseApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(base))
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@PokeVerseApp)
            modules(appModules)
        }
    }
}
