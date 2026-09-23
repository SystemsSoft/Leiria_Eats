package org.leria.eats.project.util

import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Gerador de ULID (Universally Unique Lexicographically Sortable Identifier).
 * Formato: 10 caracteres de timestamp (ms) + 16 caracteres aleatórios, em Base32 Crockford.
 * Spec: https://github.com/ulid/spec
 */
object Ulid {
    private const val ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val ENCODING_LENGTH = 32
    private const val TIME_LENGTH = 10
    private const val RANDOM_LENGTH = 16

    @OptIn(ExperimentalTime::class)
    fun nextUlid(): String {
        // kotlinx.datetime.Clock (import antigo) parou de resolver: o Compose UI
        // força kotlinx-datetime para 0.7.1 nos alvos iOS deste projeto (0.5.0 é
        // só o que o build.gradle.kts pede, mas o Gradle resolve pra cima), e essa
        // versão moveu Clock para kotlin.time.Clock — bug pré-existente e alheio à
        // conversa de voz, encontrado ao validar a Fase 2/3 no simulador iOS.
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return encodeTime(timestamp) + encodeRandomness()
    }

    private fun encodeTime(time: Long): String {
        val chars = CharArray(TIME_LENGTH)
        var remaining = time
        for (i in TIME_LENGTH - 1 downTo 0) {
            val mod = (remaining % ENCODING_LENGTH).toInt()
            chars[i] = ENCODING[mod]
            remaining /= ENCODING_LENGTH
        }
        return chars.concatToString()
    }

    private fun encodeRandomness(): String {
        val chars = CharArray(RANDOM_LENGTH)
        for (i in 0 until RANDOM_LENGTH) {
            chars[i] = ENCODING[Random.nextInt(ENCODING_LENGTH)]
        }
        return chars.concatToString()
    }
}
