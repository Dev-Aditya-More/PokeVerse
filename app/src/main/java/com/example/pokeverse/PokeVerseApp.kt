package com.example.pokeverse

import android.app.Application
import com.example.pokeverse.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class PokeVerseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PokeVerseApp)
            modules(appModule)
        }
    }
}
