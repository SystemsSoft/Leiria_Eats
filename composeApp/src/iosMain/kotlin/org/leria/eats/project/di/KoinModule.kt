package org.leria.eats.project.di

import org.koin.dsl.module
import org.leria.eats.project.permissions.IosPermissionManager
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.voice.IosVoiceRecognizer
import org.leria.eats.project.voice.VoiceRecognizer

actual val platformModule = module {
    single<PermissionManager> { IosPermissionManager() }

    single<VoiceRecognizer> { IosVoiceRecognizer() }
}