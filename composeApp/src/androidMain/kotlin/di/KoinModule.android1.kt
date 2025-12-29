package org.leria.eats.project.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.leria.eats.project.permissions.AndroidPermissionManager
import org.leria.eats.project.permissions.PermissionManager

actual val platformModule = module {
    // Single: cria uma única instância para o app todo
    single<PermissionManager> {
        AndroidPermissionManager(context = get()) // 'get()' aqui recupera o Context injetado
    }
}