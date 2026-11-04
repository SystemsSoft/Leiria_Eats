package org.leria.eats.project.voice.live

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceLoudnessTest {

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun pcmDe(vararg amostras: Int): ByteArray {
        val out = ByteArray(amostras.size * 2)
        amostras.forEachIndexed { i, s ->
            out[i * 2] = (s and 0xFF).toByte()
            out[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun amostrasDe(pcm: ByteArray): IntArray =
        IntArray(pcm.size / 2) { i -> (pcm[i * 2 + 1].toInt() shl 8) or (pcm[i * 2].toInt() and 0xFF) }

    private fun senoide(amplitude: Double, amostras: Int = 24000, hz: Double = 200.0): IntArray =
        IntArray(amostras) { n -> (amplitude * 32767 * sin(2 * PI * hz * n / 24000.0)).toInt() }

    private fun rmsDb(x: IntArray): Double = 20 * log10(sqrt(x.sumOf { it.toDouble() * it } / x.size) / 32768.0)

    // ── VoiceBoost ─────────────────────────────────────────────────────────────

    @Test
    fun `bate com os valores de referencia do prototipo medido em Python`() {
        // Entrada/saída geradas pelo protótipo (ganho +3 dB, joelho −3 dBFS, teto −1 dBFS) usado para
        // escolher esses parâmetros com áudio real da Gemini — garante que a versão Kotlin é a mesma curva.
        val entrada = intArrayOf(0, 1, -1, 1000, -1000, 8000, -8000, 16000, -16000, 20000, -20000,
            24000, -24000, 30000, -30000, 32767, -32768)
        val esperado = intArrayOf(0, 1, -1, 1413, -1413, 11300, -11300, 22601, -22601, 27321, -27321,
            28874, -28874, 29184, -29184, 29199, -29199)

        val saida = amostrasDe(VoiceBoost.apply(pcmDe(*entrada)))

        esperado.indices.forEach { i ->
            assertTrue(abs(saida[i] - esperado[i]) <= 1, "amostra ${entrada[i]}: esperado ${esperado[i]}, veio ${saida[i]}")
        }
    }

    @Test
    fun `fala baixa ganha exatamente 3 dB`() {
        val entrada = senoide(amplitude = 0.05) // −26 dBFS: bem abaixo do joelho, só o ganho linear
        val saida = amostrasDe(VoiceBoost.apply(pcmDe(*entrada)))

        val ganho = rmsDb(saida) - rmsDb(entrada)
        assertTrue(abs(ganho - VoiceBoost.GAIN_DB) < 0.05, "ganho medido $ganho dB")
    }

    @Test
    fun `sinal no maximo nunca estoura e fica abaixo do teto de menos 1 dBFS`() {
        val saida = amostrasDe(VoiceBoost.apply(pcmDe(*senoide(amplitude = 1.0))))

        val teto = (32768 * 0.8913).toInt() + 1 // −1 dBFS
        assertTrue(saida.all { it in -32768..32767 })
        assertTrue(saida.all { abs(it) <= teto }, "pico ${saida.maxOf { abs(it) }} passou do teto $teto")
    }

    @Test
    fun `curva e simetrica e nunca diminui quando a entrada aumenta`() {
        var anterior = -1
        for (s in 0..32767 step 97) {
            val positivo = amostrasDe(VoiceBoost.apply(pcmDe(s)))[0]
            val negativo = amostrasDe(VoiceBoost.apply(pcmDe(-s)))[0]
            assertEquals(positivo, -negativo, "assimetria em $s")
            assertTrue(positivo >= anterior, "saída diminuiu em $s ($positivo < $anterior)")
            anterior = positivo
        }
    }

    @Test
    fun `silencio continua silencio e o tamanho e preservado`() {
        val silencio = ByteArray(480)
        assertTrue(VoiceBoost.apply(silencio).all { it == 0.toByte() })
        assertEquals(0, VoiceBoost.apply(ByteArray(0)).size)
        assertEquals(481, VoiceBoost.apply(ByteArray(481)).size) // byte solto no fim não some
    }

    @Test
    fun `nao altera o array de entrada`() {
        val entrada = pcmDe(1000, -1000, 30000)
        val copia = entrada.copyOf()
        VoiceBoost.apply(entrada)
        assertTrue(entrada.contentEquals(copia))
    }

    // ── volumeAlvoDaChamada ────────────────────────────────────────────────────

    @Test
    fun `chamada sobe para acompanhar o volume de midia`() {
        // mídia em 12/15 (80%) e chamada em 2/5 → 80% de 5 = 4
        assertEquals(4, volumeAlvoDaChamada(volumeMidia = 12, maxMidia = 15, volumeChamada = 2, maxChamada = 5))
        // mídia no máximo → chamada no máximo
        assertEquals(5, volumeAlvoDaChamada(volumeMidia = 15, maxMidia = 15, volumeChamada = 3, maxChamada = 5))
    }

    @Test
    fun `chamada nunca e abaixada`() {
        // mídia baixa (2/15) mas chamada já em 4/5: continua 4
        assertEquals(4, volumeAlvoDaChamada(volumeMidia = 2, maxMidia = 15, volumeChamada = 4, maxChamada = 5))
        // mídia zerada: não mexe
        assertEquals(3, volumeAlvoDaChamada(volumeMidia = 0, maxMidia = 15, volumeChamada = 3, maxChamada = 5))
    }

    @Test
    fun `entradas invalidas nao quebram nem mudam o volume`() {
        assertEquals(3, volumeAlvoDaChamada(volumeMidia = 10, maxMidia = 0, volumeChamada = 3, maxChamada = 5))
        assertEquals(3, volumeAlvoDaChamada(volumeMidia = 10, maxMidia = 15, volumeChamada = 3, maxChamada = 0))
    }
}
