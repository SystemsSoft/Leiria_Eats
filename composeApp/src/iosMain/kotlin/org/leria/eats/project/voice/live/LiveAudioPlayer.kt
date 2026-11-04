package org.leria.eats.project.voice.live

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.ObjCObjectVar
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatInt16
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionPortOverrideSpeaker
import platform.Foundation.NSError
import org.leria.eats.project.domain.voice.VoiceAudioOutput

/**
 * Toca os chunks PCM16 mono 24kHz que a Gemini Live devolve (mime_type
 * "audio/pcm;rate=24000", confirmado empiricamente — ver gemini_live_bridge.py).
 *
 * NOTA: esta é a parte de maior risco de toda a Fase 2 — manipulação de ponteiros
 * (cinterop) sobre AVAudioPCMBuffer não foi validada num dispositivo real nesta
 * sessão (sem como reproduzir áudio de verdade aqui). Validar com fala real antes
 * de assumir que funciona; se o áudio sair distorcido/cortado, o ponto mais
 * provável de ajuste é a cópia de bytes -> int16ChannelData abaixo.
 */
@OptIn(ExperimentalForeignApi::class)
actual class LiveAudioPlayer actual constructor() : VoiceAudioOutput {

    companion object {
        private const val SAMPLE_RATE = 24000.0
    }

    private val engine = AVAudioEngine()
    private val playerNode = AVAudioPlayerNode()
    private val formato = AVAudioFormat(
        commonFormat = AVAudioPCMFormatInt16,
        sampleRate = SAMPLE_RATE,
        channels = 1u,
        interleaved = true,
    )
    private var iniciado = false

    private fun garantirEngineIniciado() {
        if (iniciado) return
        engine.attachNode(playerNode)
        engine.connect(playerNode, to = engine.mainMixerNode, format = formato)
        engine.prepare()
        try {
            engine.startAndReturnError(null)
            playerNode.play()
            iniciado = true
            forcarSaidaPeloAltoFalante()
        } catch (e: Exception) {
            // Falha ao iniciar o motor de áudio — próximo play() tenta de novo.
        }
    }

    // Quando a sessão de áudio está no modo de chamada de voz (ver LiveAudioRecorder,
    // ativado durante a ligação ao vivo), o iOS roteia a saída pelo auricular (fone de
    // ouvido, feito pra ficar perto do ouvido) em vez do alto-falante — daí a voz da IA
    // soar bem mais baixa que o volume de mídia do aparelho. Forçar o alto-falante aqui
    // corrige isso; é um no-op inofensivo quando a sessão não está em PlayAndRecord.
    private fun forcarSaidaPeloAltoFalante() {
        memScoped {
            val errPtr = alloc<ObjCObjectVar<NSError?>>()
            AVAudioSession.sharedInstance()
                .overrideOutputAudioPort(AVAudioSessionPortOverrideSpeaker, error = errPtr.ptr)
        }
    }

    actual override fun play(pcm: ByteArray) {
        garantirEngineIniciado()
        if (pcm.isEmpty()) return

        // Reforço de volume (+3 dB com limitador suave) — o mesmo do Android; ver VoiceBoost.
        val reforcado = VoiceBoost.apply(pcm)

        val frameCount = reforcado.size / 2 // PCM16 = 2 bytes por amostra
        if (frameCount <= 0) return

        val buffer = AVAudioPCMBuffer(pCMFormat = formato, frameCapacity = frameCount.toUInt())
        buffer.frameLength = frameCount.toUInt()

        val canal = buffer.int16ChannelData?.get(0) ?: return
        for (i in 0 until frameCount) {
            val lo = reforcado[i * 2].toInt() and 0xFF
            val hi = reforcado[i * 2 + 1].toInt() and 0xFF
            canal[i] = ((hi shl 8) or lo).toShort()
        }

        playerNode.scheduleBuffer(buffer, completionHandler = null)
    }

    actual override fun stop() {
        playerNode.stop()
        iniciado = false
    }

    actual override fun release() {
        playerNode.stop()
        engine.stop()
        iniciado = false
    }

    // No iOS o roteamento da ligação (categoria PlayAndRecord + modo VoiceChat) é configurado pelo
    // LiveAudioRecorder ao abrir o microfone, e a saída forçada para o alto-falante acontece em
    // garantirEngineIniciado() — aqui não há nada a preparar nem a devolver. Só o reforço digital
    // (VoiceBoost) vale nas duas plataformas; o ajuste de volume/roteamento de chamada do Android
    // (ver LiveAudioPlayer.android.kt) NÃO foi portado por falta de um aparelho iOS para validar.
    actual override fun startCall() {}

    actual override fun endCall() {}
}
