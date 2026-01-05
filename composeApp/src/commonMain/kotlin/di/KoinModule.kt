package org.leria.eats.project.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.leria.eats.project.data.LeriaApiClient

expect val platformModule: Module

val sharedModule = module {
    single { LeriaApiClient() }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(platformModule, sharedModule)
    }