package org.leria.eats.project.voice.live

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.tanh

/**
 * Reforço de volume da voz da IA (PCM16 mono little-endian, o formato de [LiveAudioPlayer]).
 *
 * Medido em 24/09/2026 com áudio real da Gemini (ligação ao vivo e síntese de fala do chat): a voz
 * já sai com pico de ≈ −1,6 dBFS e ≈ −18 LUFS (≈ −16,5 na síntese do chat). Ou seja, o sinal NÃO é
 * fraco e quase não há margem para ganho — o "volume baixo" vinha do caminho de saída do Android
 * (ver LiveAudioPlayer.android.kt). Aqui só se sobe o nível até o de mídia comum (≈ −14 LUFS):
 * +3 dB com um limitador suave que segura os picos em −1 dBFS, sem estourar (clipar).
 *
 * Nas amostras de teste, +3 dB levou a ligação de −17,8 para −14,8 LUFS com só 0,6–1% das amostras
 * caindo no limitador (relação sinal/distorção ≥ 33 dB — inaudível na fala). Acima de +4 dB a
 * distorção cresce rápido, então não vale subir mais do que isso por software; mais volume que
 * isso é papel do volume do aparelho.
 *
 * É uma curva sem memória (cada amostra depende só dela mesma), então funciona chunk a chunk no
 * streaming, sem estado entre chamadas.
 */
object VoiceBoost {
    /** Ganho aplicado antes do limitador. */
    const val GAIN_DB = 3.0

    // Abaixo do joelho a saída é linear (só o ganho). Entre o joelho e o teto a curva é uma tanh
    // com a mesma inclinação do trecho linear (sem "quina"), chegando suavemente ao teto.
    private const val KNEE_DB = -3.0
    private const val CEILING_DB = -1.0

    private val gain = 10.0.pow(GAIN_DB / 20.0)
    private val knee = 10.0.pow(KNEE_DB / 20.0)
    private val ceiling = 10.0.pow(CEILING_DB / 20.0)

    /** Devolve uma cópia de [pcm] com o reforço aplicado; [pcm] não é alterado. */
    fun apply(pcm: ByteArray): ByteArray {
        val out = ByteArray(pcm.size)
        var i = 0
        while (i + 1 < pcm.size) {
            val lo = pcm[i].toInt() and 0xFF
            val hi = pcm[i + 1].toInt() // com sinal de propósito: (hi shl 8) or lo já é o int16
            val amostra = (hi shl 8) or lo
            // round() = meio-para-par: simétrico entre positivo e negativo (roundToInt não é)
            val v = round(limitar(amostra / 32768.0) * 32768.0).toInt().coerceIn(-32768, 32767)
            out[i] = (v and 0xFF).toByte()
            out[i + 1] = ((v shr 8) and 0xFF).toByte()
            i += 2
        }
        if (i < pcm.size) out[i] = pcm[i] // byte solto no fim (tamanho ímpar): não é uma amostra
        return out
    }

    private fun limitar(x: Double): Double {
        val v = x * gain
        val a = abs(v)
        if (a <= knee) return v
        val suave = knee + (ceiling - knee) * tanh((a - knee) / (ceiling - knee))
        return if (v < 0) -suave else suave
    }
}

/**
 * Índice de volume do fluxo de CHAMADA (STREAM_VOICE_CALL) para a ligação ao vivo: a mesma
 * proporção do volume de MÍDIA que o usuário está usando, e nunca abaixo do que a chamada já tem
 * (não mexemos no volume de quem deixou a chamada mais alta).
 *
 * Por quê: o Android guarda o volume de chamada separado do de mídia, e ele fica no padrão do
 * sistema (baixo) porque ninguém costuma mexer nele. Sem alinhar, o usuário com o volume de mídia
 * alto ainda ouvia a IA baixinho.
 */
fun volumeAlvoDaChamada(volumeMidia: Int, maxMidia: Int, volumeChamada: Int, maxChamada: Int): Int {
    if (maxMidia <= 0 || maxChamada <= 0) return volumeChamada
    val proporcional = ceil(volumeMidia.toDouble() / maxMidia * maxChamada).toInt().coerceIn(0, maxChamada)
    return maxOf(volumeChamada, proporcional)
}
