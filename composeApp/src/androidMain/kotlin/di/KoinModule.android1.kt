package org.leria.eats.project.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.leria.eats.project.permissions.AndroidPermissionManager
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.service.AndroidLocationService
import org.leria.eats.project.service.LocationService
import org.leria.eats.project.voice.AndroidVoiceRecognizer
import org.leria.eats.project.voice.AndroidTextToSpeechService
import org.leria.eats.project.voice.TextToSpeechService
import org.leria.eats.project.voice.VoiceRecognizer

actual val platformModule = module {

    single<PermissionManager> {
        AndroidPermissionManager(context = get())
    }

    single<LocationService> {
        AndroidLocationService(context = androidContext())
    }

    single<VoiceRecognizer> { AndroidVoiceRecognizer(context = get()) }

    single<TextToSpeechService> { AndroidTextToSpeechService(context = androidContext()) }
}
