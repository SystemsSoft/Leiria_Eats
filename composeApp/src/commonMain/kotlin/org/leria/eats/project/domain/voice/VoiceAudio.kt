package org.leria.eats.project.domain.voice

import kotlinx.coroutines.flow.Flow

/**
 * Saída de áudio PCM16 mono 24kHz (voz da IA). Implementada por plataforma em
 * voice/live/LiveAudioPlayer (AudioTrack no Android, AVAudioEngine no iOS).
 */
interface VoiceAudioOutput {
    fun play(pcm: ByteArray)
    /** Limpa o áudio enfileirado ainda não tocado (barge-in). */
    fun stop()
    fun release()
    /** Entra no perfil de ligação ao vivo (caminho de chamada, eco cancelado). */
    fun startCall()
    /** Sai do perfil de ligação. Seguro chamar sem [startCall] antes e mais de uma vez. */
    fun endCall()
}

/**
 * Captura do microfone em PCM16 mono 16kHz. Flow frio: grava enquanto houver coleta.
 * Implementada por plataforma em voice/live/LiveAudioRecorder.
 */
interface VoiceAudioInput {
    fun audioChunks(): Flow<ByteArray>
}
