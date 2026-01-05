package org.leria.eats.project.di

import org.koin.dsl.module
import org.leria.eats.project.permissions.AndroidPermissionManager
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.voice.AndroidVoiceRecognizer
import org.leria.eats.project.voice.VoiceRecognizer

actual val platformModule = module {
    single<PermissionManager> {
        AndroidPermissionManager(context = get())
    }

    single<VoiceRecognizer> { AndroidVoiceRecognizer(context = get()) }
}