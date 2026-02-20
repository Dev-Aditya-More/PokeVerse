package com.aditya1875.pokeverse.di

import com.aditya1875.pokeverse.data.billing.BillingManager
import com.aditya1875.pokeverse.data.billing.IBillingManager
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val billingModule = module {
    single<IBillingManager> {
        BillingManager(
            context = get(),
            coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        ).also { it.startConnection() }
    }

    viewModel {
        BillingViewModel(billingManager = get())
    }
}