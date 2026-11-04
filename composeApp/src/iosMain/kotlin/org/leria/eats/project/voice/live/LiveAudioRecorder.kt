package org.leria.eats.project.voice.live

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFAudio.AVAudioConverter
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatInt16
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeVoiceChat
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import org.leria.eats.project.domain.voice.VoiceAudioInput

/**
 * Captura o microfone e converte para PCM16 mono 16kHz — formato exigido pela
 * Gemini Live API (ver nota no topo de gemini_live_bridge.py, no servidor). O
 * microfone do iPhone normalmente não grava nesse formato nativamente (é comum
 * Float32 a 44.1/48kHz), daí o AVAudioConverter no meio.
 *
 * NOTA: cinterop sobre AVAudioConverter/AVAudioPCMBuffer não foi validado com fala
 * real num dispositivo nesta sessão — validar antes de assumir que a conversão de
 * sample rate/formato está correta (se soar distorcido ou na velocidade errada,
 * é aqui que está o problema mais provável).
 */
@OptIn(ExperimentalForeignApi::class)
actual class LiveAudioRecorder : VoiceAudioInput {

    companion object {
        private const val SAMPLE_RATE = 16000.0
    }

    actual override fun audioChunks(): Flow<ByteArray> = callbackFlow {
        val audioSession = AVAudioSession.sharedInstance()
        memScoped {
            val errPtr = alloc<ObjCObjectVar<NSError?>>()
            audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, error = errPtr.ptr)
            audioSession.setMode(AVAudioSessionModeVoiceChat, error = errPtr.ptr)
            audioSession.setActive(true, withOptions = 0u, error = errPtr.ptr)
        }

        val engine = AVAudioEngine()
        val inputNode = engine.inputNode
        val formatoNativo = inputNode.outputFormatForBus(0u)
        val formatoAlvo = AVAudioFormat(
            commonFormat = AVAudioPCMFormatInt16,
            sampleRate = SAMPLE_RATE,
            channels = 1u,
            interleaved = true,
        )

        if (formatoAlvo == null) {
            close(IllegalStateException("Não foi possível criar o formato de áudio alvo"))
            return@callbackFlow
        }

        val converter = AVAudioConverter(fromFormat = formatoNativo, toFormat = formatoAlvo)
        if (converter == null) {
            close(IllegalStateException("Não foi possível criar o conversor de áudio"))
            return@callbackFlow
        }

        inputNode.installTapOnBus(0u, bufferSize = 4096u, format = formatoNativo) { buffer, _ ->
            if (buffer == null) return@installTapOnBus

            val proporcao = SAMPLE_RATE / formatoNativo.sampleRate
            val framesAlvo = (buffer.frameLength.toDouble() * proporcao).toUInt() + 16u
            val outBuffer = AVAudioPCMBuffer(pCMFormat = formatoAlvo, frameCapacity = framesAlvo)
            if (outBuffer == null) return@installTapOnBus

            memScoped {
                val errPtr = alloc<ObjCObjectVar<NSError?>>()
                converter.convertToBuffer(outBuffer, fromBuffer = buffer, error = errPtr.ptr)
            }

            val frameLength = outBuffer.frameLength.toInt()
            val canal = outBuffer.int16ChannelData?.get(0)
            if (frameLength > 0 && canal != null) {
                val bytes = ByteArray(frameLength * 2)
                for (i in 0 until frameLength) {
                    val sample = canal[i].toInt()
                    bytes[i * 2] = (sample and 0xFF).toByte()
                    bytes[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
                }
                trySend(bytes)
            }
        }

        engine.prepare()
        val iniciouOk = try {
            engine.startAndReturnError(null)
        } catch (e: Exception) {
            false
        }
        if (!iniciouOk) {
            inputNode.removeTapOnBus(0u)
            close(IllegalStateException("Erro ao iniciar o motor de áudio"))
            return@callbackFlow
        }

        awaitClose {
            engine.stop()
            inputNode.removeTapOnBus(0u)
        }
    }
}
