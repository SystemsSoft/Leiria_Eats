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
import org.leria.eats.project.voice.GeminiTextToSpeechService
import org.leria.eats.project.voice.TextToSpeechService
import org.leria.eats.project.voice.live.LiveAudioPlayer
import org.leria.eats.project.voice.live.LiveAudioRecorder
import org.leria.eats.project.voice.live.LiveConversationClient

expect val platformModule: Module

val sharedModule = module {
    single { LeriaApiClient() }

    single { getDataStore() }

    single { ProfileRepository(get()) }

    single { ChatRepository(get()) }

    single { StripePaymentManager() }

    // Conversa de voz em tempo real (Gemini Live API) — LiveAudioRecorder/Player
    // são "expect class" com implementação nativa por plataforma (AudioRecord/
    // AudioTrack no Android, AVAudioEngine no iOS), resolvidas automaticamente
    // pelo compilador; não precisam de platformModule separado.
    single { LiveConversationClient() }
    single { LiveAudioRecorder() }
    single { LiveAudioPlayer() }

    // Voz única do app inteiro (mesma da ligação ao vivo, "Aoede") — substitui as
    // implementações nativas antigas (AndroidTextToSpeechService/
    // IosTextToSpeechService), removidas.
    single<TextToSpeechService> { GeminiTextToSpeechService(get(), get()) }

    viewModel { SearchViewModel(get(), get(), get(), get(), get(), get(), get()) }

}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(platformModule, sharedModule)
    }