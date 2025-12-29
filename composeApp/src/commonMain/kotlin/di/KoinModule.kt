package org.leria.eats.project.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

// 1. Declaramos que esperamos que cada plataforma forneça seu próprio módulo
expect val platformModule: Module

// 2. Criamos a função initKoin que será chamada pelo AndroidApp e pelo iOS
fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        // Executa configurações extras (como androidContext)
        appDeclaration()
        // Carrega o ódulo da plataforma específica
        modules(platformModule)
    }