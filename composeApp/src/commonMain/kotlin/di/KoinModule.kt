package org.leria.eats.project.di


import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import org.leria.eats.project.data.getDataStore
import org.leria.eats.project.data.local.UserPreferencesDataSource
import org.leria.eats.project.data.remote.LeriaApiClient
import org.leria.eats.project.data.repository.AiChatRepositoryImpl
import org.leria.eats.project.data.repository.ChatHistoryRepositoryImpl
import org.leria.eats.project.data.repository.OrderRepositoryImpl
import org.leria.eats.project.data.repository.ProfileRepositoryImpl
import org.leria.eats.project.data.repository.RestaurantRepositoryImpl
import org.leria.eats.project.data.repository.SpeechRepositoryImpl
import org.leria.eats.project.data.voice.LiveConversationClient
import org.leria.eats.project.domain.repository.AiChatRepository
import org.leria.eats.project.domain.repository.ChatHistoryRepository
import org.leria.eats.project.domain.repository.LiveConversationRepository
import org.leria.eats.project.domain.repository.OrderRepository
import org.leria.eats.project.domain.repository.ProfileRepository
import org.leria.eats.project.domain.repository.RestaurantRepository
import org.leria.eats.project.domain.repository.SpeechRepository
import org.leria.eats.project.domain.usecase.PlaceOrderUseCase
import org.leria.eats.project.domain.usecase.RateOrderItemUseCase
import org.leria.eats.project.domain.usecase.SaveUserProfileUseCase
import org.leria.eats.project.domain.usecase.SyncSavedPaymentMethodsUseCase
import org.leria.eats.project.domain.voice.VoiceAudioInput
import org.leria.eats.project.domain.voice.VoiceAudioOutput
import org.leria.eats.project.payment.StripePaymentManager
import org.leria.eats.project.presentation.viewmodel.SearchViewModel
import org.leria.eats.project.voice.GeminiTextToSpeechService
import org.leria.eats.project.voice.TextToSpeechService
import org.leria.eats.project.voice.live.LiveAudioPlayer
import org.leria.eats.project.voice.live.LiveAudioRecorder

expect val platformModule: Module

/** Fontes de dados (Ktor, DataStore, WebSocket) e as implementações dos repositórios do domínio. */
val dataModule = module {
    single { LeriaApiClient() }
    single { getDataStore() }
    singleOf(::UserPreferencesDataSource)

    singleOf(::RestaurantRepositoryImpl) bind RestaurantRepository::class
    singleOf(::AiChatRepositoryImpl) bind AiChatRepository::class
    singleOf(::ChatHistoryRepositoryImpl) bind ChatHistoryRepository::class
    singleOf(::OrderRepositoryImpl) bind OrderRepository::class
    singleOf(::ProfileRepositoryImpl) bind ProfileRepository::class
    singleOf(::SpeechRepositoryImpl) bind SpeechRepository::class

    // Conversa de voz em tempo real (Gemini Live API via bridge no servidor).
    single<LiveConversationRepository> { LiveConversationClient() }
}

val domainModule = module {
    factoryOf(::PlaceOrderUseCase)
    factoryOf(::RateOrderItemUseCase)
    factoryOf(::SaveUserProfileUseCase)
    factoryOf(::SyncSavedPaymentMethodsUseCase)
}

val presentationModule = module {
    single { StripePaymentManager() }

    // LiveAudioRecorder/Player são "expect class" com implementação nativa por plataforma
    // (AudioRecord/AudioTrack no Android, AVAudioEngine no iOS), resolvidas pelo compilador.
    // Um único player para a ligação e para o TTS (mesma saída de áudio).
    single<VoiceAudioInput> { LiveAudioRecorder() }
    single<VoiceAudioOutput> { LiveAudioPlayer() }

    // Voz única do app inteiro (mesma da ligação ao vivo, "Aoede") — substitui as
    // implementações nativas antigas (AndroidTextToSpeechService/
    // IosTextToSpeechService), removidas.
    single<TextToSpeechService> { GeminiTextToSpeechService(get(), get()) }

    viewModelOf(::SearchViewModel)
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(platformModule, dataModule, domainModule, presentationModule)
    }
