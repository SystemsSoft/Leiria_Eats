package org.leria.eats.project.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.leria.eats.project.domain.repository.SpeechRepository
import org.leria.eats.project.domain.voice.VoiceAudioOutput

/**
 * Implementação única (commonMain, sem `expect`/`actual`) de [TextToSpeechService]
 * usando a MESMA voz da ligação ao vivo (Gemini, voz "Aoede" — ver
 * GeminiSalesAgent.TTS_VOICE no servidor) para TODO o app — chat normal, chat de
 * voz pelo microfone, sacola, onboarding e perfil —, substituindo as vozes nativas
 * do Android/iOS que existiam antes (AndroidTextToSpeechService/
 * IosTextToSpeechService, removidas).
 *
 * Reaproveita a saída de áudio da ligação ([VoiceAudioOutput], mesmo formato de áudio: PCM16 mono 24kHz) — não
 * precisa de nenhum player novo nem de código específico por plataforma.
 */
class GeminiTextToSpeechService(
    private val speechRepository: SpeechRepository,
    private val audioPlayer: VoiceAudioOutput,
) : TextToSpeechService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var currentJob: Job? = null

    override fun speak(text: String) {
        if (text.isBlank()) return
        // Mesmo comportamento do TTS nativo anterior (QUEUE_FLUSH): uma nova fala
        // corta a anterior, nunca fica lendo duas coisas em cima uma da outra.
        currentJob?.cancel()
        audioPlayer.stop()
        currentJob = scope.launch {
            val pcm = speechRepository.synthesize(text) ?: return@launch
            audioPlayer.play(pcm)
        }
    }

    override fun playAudio(pcm: ByteArray) {
        currentJob?.cancel()
        audioPlayer.stop()
        audioPlayer.play(pcm)
    }

    override fun stop() {
        currentJob?.cancel()
        audioPlayer.stop()
    }

    override fun shutdown() {
        currentJob?.cancel()
        audioPlayer.release()
    }
}
