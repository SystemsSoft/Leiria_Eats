package org.leria.eats.project.voice.live

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import org.leria.eats.project.data.getApplicationContextOrNull
import org.leria.eats.project.domain.voice.VoiceAudioOutput

/**
 * Dois perfis de saída, escolhidos por [startCall]/[endCall]:
 *
 * • FORA de uma ligação (respostas faladas do chat, sacola, onboarding, perfil): o áudio toca como
 *   MÍDIA (`USAGE_MEDIA`). Sai pelo alto-falante no volume de mídia — o mesmo que os botões de
 *   volume do aparelho controlam — e nenhum modo de áudio do sistema é alterado.
 *
 * • DURANTE a ligação ao vivo (microfone aberto junto): `USAGE_VOICE_COMMUNICATION`, para o
 *   cancelamento de eco do Android correlacionar a saída com a entrada do LiveAudioRecorder
 *   (também VOICE_COMMUNICATION). Trocar por USAGE_MEDIA aqui tira o áudio desse caminho e o
 *   microfone volta a captar a própria voz da IA, causando eco/loop (a IA "conversando sozinha",
 *   visto em produção). Nesse perfil [startCall] põe o áudio em modo de comunicação, força o
 *   alto-falante (se não houver fone/bluetooth) e alinha o volume da chamada ao de mídia.
 *
 * HISTÓRICO DO "VOLUME BAIXO": a versão anterior já tentava forçar viva-voz e modo de comunicação
 * via AudioManager, mas o app NÃO declarava a permissão MODIFY_AUDIO_SETTINGS — sem ela o Android
 * (AudioService) descarta setMode() e setSpeakerphoneOn() em silêncio ("Audio Settings Permission
 * Denial" no logcat). Resultado: a voz saía pelo auricular, no fluxo de chamada, e como o modo nunca
 * virava IN_COMMUNICATION os botões de volume continuavam mexendo na MÍDIA — o volume do aparelho
 * não afetava a IA. A permissão agora está no AndroidManifest.xml.
 */
actual class LiveAudioPlayer actual constructor() : VoiceAudioOutput {

    companion object {
        private const val SAMPLE_RATE = 24000 // confirmado empiricamente: "audio/pcm;rate=24000"

        // Sinal de "pare" da thread de escrita. LinkedBlockingQueue NÃO aceita null (offer(null)
        // lança NullPointerException), então o sinal é este array vazio, comparado por identidade.
        private val PARE = ByteArray(0)
    }

    /** AudioTrack + fila + thread de escrita, criados e descartados JUNTOS — assim trocar de perfil
     * (mídia ↔ chamada) nunca deixa uma thread velha consumindo a fila da faixa nova. */
    private class Reproducao(val track: AudioTrack, val fila: LinkedBlockingQueue<ByteArray>)

    /** O que [startCall] mexeu no sistema, para [endCall] devolver do jeito que estava. Os campos
     * são preenchidos à medida que cada ajuste é feito, então uma falha no meio ainda dá pra
     * desfazer só o que já tinha sido aplicado. */
    private class AjustesDeChamada(val modoAnterior: Int, val vivaVozAnterior: Boolean) {
        var usouDispositivoDeComunicacao = false // Android 12+: setCommunicationDevice(alto-falante)
        var forcouVivaVoz = false                // Android ≤ 11: isSpeakerphoneOn
        var volumeChamadaAnterior: Int? = null   // só preenchidos se a gente SUBIU o volume da chamada
        var volumeChamadaDefinido: Int? = null
    }

    // Este objeto é um singleton de app inteiro (Koin `single`) — por isso a faixa e a thread só
    // nascem no primeiro play(), no mesmo espírito do garantirEngineIniciado() do iOS.
    private var reproducao: Reproducao? = null
    private var emChamada = false
    private var ajustesDeChamada: AjustesDeChamada? = null

    private fun garantirIniciado(): Reproducao {
        reproducao?.let { return it }

        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val usage = if (emChamada) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuffer, SAMPLE_RATE)) // ~0.5-1s de folga
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()

        // Thread dedicada: AudioTrack.write(...) em modo STREAM bloqueia até ter espaço no buffer
        // nativo — não dá pra chamar isso a cada chunk vindo do WebSocket sem travar a coroutine
        // que recebe as mensagens.
        val fila = LinkedBlockingQueue<ByteArray>()
        thread(start = true, isDaemon = true, name = "LiveAudioPlayer") {
            while (true) {
                val chunk = fila.take()
                if (chunk === PARE) break
                runCatching { track.write(chunk, 0, chunk.size) }
            }
        }
        return Reproducao(track, fila).also { reproducao = it }
    }

    private fun encerrarReproducao() {
        val r = reproducao ?: return
        reproducao = null
        r.fila.clear()
        r.fila.offer(PARE)
        runCatching { r.track.stop() }
        runCatching { r.track.release() }
    }

    @Synchronized
    actual override fun play(pcm: ByteArray) {
        val r = garantirIniciado()
        r.fila.offer(VoiceBoost.apply(pcm))
    }

    @Synchronized
    actual override fun stop() {
        val r = reproducao ?: return
        r.fila.clear()
        runCatching { r.track.pause() }
        runCatching { r.track.flush() }
        runCatching { r.track.play() }
    }

    @Synchronized
    actual override fun release() {
        endCall()
        encerrarReproducao()
    }

    @Synchronized
    actual override fun startCall() {
        if (emChamada) return
        emChamada = true
        // A faixa atual (de mídia) é descartada: a próxima play() cria a de chamada, já com o
        // modo de áudio e o roteamento certos.
        encerrarReproducao()
        ajustesDeChamada = runCatching { aplicarAjustesDeChamada() }.getOrNull()
    }

    @Synchronized
    actual override fun endCall() {
        if (!emChamada) return
        emChamada = false
        encerrarReproducao() // a próxima play() (resposta falada do chat) volta a tocar como mídia
        ajustesDeChamada?.let { ajustes -> runCatching { restaurarAjustes(ajustes) } }
        ajustesDeChamada = null
    }

    // ── sistema de áudio ──────────────────────────────────────────────────────────────────────

    private fun audioManager(): AudioManager? =
        getApplicationContextOrNull()?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    @Suppress("DEPRECATION")
    private fun aplicarAjustesDeChamada(): AjustesDeChamada? {
        val audioManager = audioManager() ?: return null
        // Ligação telefônica de verdade em andamento (ou tocando): não mexe em nada do áudio.
        if (audioManager.mode != AudioManager.MODE_NORMAL) return null

        val ajustes = AjustesDeChamada(
            modoAnterior = audioManager.mode,
            vivaVozAnterior = audioManager.isSpeakerphoneOn,
        )
        try {
            // Requer MODIFY_AUDIO_SETTINGS (AndroidManifest.xml). Com o modo em comunicação, os
            // botões de volume passam a controlar o fluxo de chamada — o mesmo em que a voz da IA toca.
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            // Com fone (fio/bluetooth/USB) conectado a saída fica com ele — só força o alto-falante
            // quando o som iria para o auricular, que é onde a voz ficava baixinha.
            if (!temSaidaExternaConectada(audioManager)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val altoFalante = audioManager.availableCommunicationDevices
                        .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    ajustes.usouDispositivoDeComunicacao =
                        altoFalante != null && audioManager.setCommunicationDevice(altoFalante)
                }
                if (!ajustes.usouDispositivoDeComunicacao) {
                    audioManager.isSpeakerphoneOn = true
                    ajustes.forcouVivaVoz = true
                }
            }

            alinharVolumeDaChamada(audioManager, ajustes)
        } catch (e: Exception) {
            runCatching { restaurarAjustes(ajustes) }
            return null
        }
        return ajustes
    }

    /** Sobe o volume do fluxo de chamada para acompanhar o de mídia (nunca abaixa). */
    private fun alinharVolumeDaChamada(audioManager: AudioManager, ajustes: AjustesDeChamada) {
        if (audioManager.isVolumeFixed) return
        val atual = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        val alvo = volumeAlvoDaChamada(
            volumeMidia = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
            maxMidia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            volumeChamada = atual,
            maxChamada = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
        )
        if (alvo <= atual) return
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, alvo, 0)
        ajustes.volumeChamadaAnterior = atual
        ajustes.volumeChamadaDefinido = alvo
    }

    private fun temSaidaExternaConectada(audioManager: AudioManager): Boolean {
        val externos = mutableListOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) externos += AudioDeviceInfo.TYPE_USB_HEADSET
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) externos += AudioDeviceInfo.TYPE_HEARING_AID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) externos += AudioDeviceInfo.TYPE_BLE_HEADSET
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in externos }
    }

    @Suppress("DEPRECATION")
    private fun restaurarAjustes(ajustes: AjustesDeChamada) {
        val audioManager = audioManager() ?: return
        // Só devolve o volume se ninguém mexeu nele durante a ligação (se o usuário ajustou pelos
        // botões, o ajuste dele vale mais do que o nosso valor antigo).
        val anterior = ajustes.volumeChamadaAnterior
        val definido = ajustes.volumeChamadaDefinido
        if (anterior != null && definido != null &&
            audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) == definido
        ) {
            runCatching { audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, anterior, 0) }
        }
        if (ajustes.usouDispositivoDeComunicacao && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { audioManager.clearCommunicationDevice() }
        }
        if (ajustes.forcouVivaVoz) {
            runCatching { audioManager.isSpeakerphoneOn = ajustes.vivaVozAnterior }
        }
        runCatching { audioManager.mode = ajustes.modoAnterior }
    }
}
