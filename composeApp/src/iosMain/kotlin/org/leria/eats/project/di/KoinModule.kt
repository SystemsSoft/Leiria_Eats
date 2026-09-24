package org.leria.eats.project.di

import org.koin.dsl.module
import org.leria.eats.project.permissions.IosPermissionManager
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.service.IosLocationService
import org.leria.eats.project.service.LocationService
import org.leria.eats.project.voice.IosVoiceRecognizer
import org.leria.eats.project.voice.VoiceRecognizer

actual val platformModule = module {
    single<PermissionManager> { IosPermissionManager() }
    single<LocationService> { IosLocationService() }
    single<VoiceRecognizer> { IosVoiceRecognizer() }

    // TextToSpeechService: ver sharedModule (GeminiTextToSpeechService) — mesma
    // voz da ligação ao vivo em todas as plataformas, sem implementação nativa.
}
