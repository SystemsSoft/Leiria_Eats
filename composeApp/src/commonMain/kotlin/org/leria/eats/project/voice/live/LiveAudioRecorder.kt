package org.leria.eats.project.voice.live

import kotlinx.coroutines.flow.Flow
import org.leria.eats.project.domain.voice.VoiceAudioInput

/**
 * Captura áudio bruto do microfone em PCM16 mono 16kHz — formato exigido pela
 * Gemini Live API (ver nota no topo de gemini_live_bridge.py, no servidor).
 * Diferente do VoiceRecognizer existente (reconhecimento de fala on-device, só
 * expõe texto final), aqui o áudio bruto é o que interessa, para streaming em
 * tempo real.
 *
 * `audioChunks()` é um Flow "frio": a gravação só começa quando alguém coleta o
 * Flow, e para sozinha (liberando o microfone) quando a coleta é cancelada — não
 * há start()/stop() separados, é só cancelar o Job da coleta.
 *
 * Quem chama PRECISA garantir a permissão de microfone concedida antes (mesmo
 * PermissionManager/RECORD_AUDIO já usado pelo VoiceRecognizer) — esta classe não
 * pede permissão sozinha.
 */
expect class LiveAudioRecorder() : VoiceAudioInput {
    override fun audioChunks(): Flow<ByteArray>
}
