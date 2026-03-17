package com.aditya1875.pokeverse.di

import com.aditya1875.pokeverse.data.billing.BillingManager
import com.aditya1875.pokeverse.data.billing.IBillingManager
import com.aditya1875.pokeverse.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.domain.trivia.DailyTriviaManager
import com.aditya1875.pokeverse.domain.xp.XPManager
import com.aditya1875.pokeverse.presentation.auth.AuthManager
import com.aditya1875.pokeverse.utils.SoundManager
import com.google.gson.Gson
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val billingModule = module {

    single { AuthManager(get()) }

    single { UserProfileRepository(androidContext()) }

    single { XPManager(get()) }

    single<IBillingManager> {
        BillingManager(get(), get())
    }

    single {
        DailyTriviaManager(get(), get())
    }

    single { Gson() }

    single { SoundManager(get()) }

}