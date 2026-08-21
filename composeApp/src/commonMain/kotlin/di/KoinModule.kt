package org.leria.eats.project.di


import org.koin.core.module.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.leria.eats.project.data.ChatRepository
import org.leria.eats.project.data.LeriaApiClient
import org.leria.eats.project.data.ProfileRepository
import org.leria.eats.project.data.getDataStore
import org.leria.eats.project.payment.StripePaymentManager
import org.leria.eats.project.presentation.viewmodel.SearchViewModel

expect val platformModule: Module

val sharedModule = module {
    single { LeriaApiClient() }

    single { getDataStore() }

    single { ProfileRepository(get()) }
    
    single { ChatRepository(get()) }
    
    single { StripePaymentManager() }

    viewModel { SearchViewModel(get(), get(), get(), get()) }

}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(platformModule, sharedModule)
    }