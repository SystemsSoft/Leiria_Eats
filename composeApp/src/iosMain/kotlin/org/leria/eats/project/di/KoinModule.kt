package org.leria.eats.project.di

import org.koin.dsl.module
import org.leria.eats.project.permissions.IosPermissionManager
import org.leria.eats.project.permissions.PermissionManager

actual val platformModule = module {
    single<PermissionManager> { IosPermissionManager() }
}