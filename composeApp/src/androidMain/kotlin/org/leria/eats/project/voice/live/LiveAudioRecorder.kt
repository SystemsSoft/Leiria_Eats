package org.leria.eats.project.voice.live

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.leria.eats.project.domain.voice.VoiceAudioInput

actual class LiveAudioRecorder : VoiceAudioInput {

    companion object {
        private const val SAMPLE_RATE = 16000 // exigido pela Gemini Live API (entrada)
        // ~100ms por chunk — pequeno o bastante pra latência baixa, grande o
        // bastante pra não inundar o WebSocket com frames minúsculos.
        private const val CHUNK_MS = 100
        private const val BYTES_POR_AMOSTRA = 2 // PCM16
        private val CHUNK_SIZE_BYTES = SAMPLE_RATE * BYTES_POR_AMOSTRA * CHUNK_MS / 1000
    }

    @SuppressLint("MissingPermission") // permissão RECORD_AUDIO verificada antes de chamar isto
    actual override fun audioChunks(): Flow<ByteArray> = callbackFlow {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            close(IllegalStateException("Dispositivo não suporta gravação em $SAMPLE_RATE Hz mono PCM16"))
            return@callbackFlow
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, CHUNK_SIZE_BYTES * 2)
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            close(IllegalStateException("AudioRecord falhou ao inicializar"))
            return@callbackFlow
        }

        audioRecord.startRecording()

        // O loop de leitura roda num Job separado, verificando isActive a cada
        // volta — cancelar esse Job (no awaitClose, quando o coletor do Flow
        // desiste) é o único jeito confiável de parar: audioRecord.read() é uma
        // chamada bloqueante (não suspend), então cancelamento cooperativo comum
        // não a interrompe no meio: o loop só nota a cada iteração.
        val buffer = ByteArray(CHUNK_SIZE_BYTES)
        val job = launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val lidos = audioRecord.read(buffer, 0, buffer.size)
                    if (lidos > 0) {
                        trySend(buffer.copyOf(lidos))
                    } else if (lidos < 0) {
                        break // erro (AudioRecord.ERROR_*)
                    }
                }
            } finally {
                runCatching { audioRecord.stop() }
                runCatching { audioRecord.release() }
            }
        }

        awaitClose { job.cancel() }
    }.flowOn(Dispatchers.IO)
}
