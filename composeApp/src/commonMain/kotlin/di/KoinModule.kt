package org.leria.eats.project.di


import org.koin.core.module.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.leria.eats.project.data.LeriaApiClient
import org.leria.eats.project.presentation.SearchViewModel

expect val platformModule: Module

val sharedModule = module {
    single { LeriaApiClient() }

    viewModel { SearchViewModel(get()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(platformModule, sharedModule)
    }