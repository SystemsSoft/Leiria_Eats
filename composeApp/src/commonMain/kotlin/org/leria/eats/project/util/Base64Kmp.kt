package org.leria.eats.project.util

// Codificação Base64 própria, sem depender de kotlin.io.encoding.Base64 (experimental
// e sensível à versão do Kotlin) nem de nenhuma lib nova — só usado para os chunks de
// áudio PCM trocados com o bridge de voz (services/gemini_live_bridge.py no servidor).

private const val BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

fun ByteArray.toBase64(): String {
    if (isEmpty()) return ""
    val sb = StringBuilder(((size + 2) / 3) * 4)
    var i = 0
    while (i < size) {
        val b0 = this[i].toInt() and 0xFF
        val b1 = if (i + 1 < size) this[i + 1].toInt() and 0xFF else 0
        val b2 = if (i + 2 < size) this[i + 2].toInt() and 0xFF else 0
        val triple = (b0 shl 16) or (b1 shl 8) or b2
        sb.append(BASE64_CHARS[(triple shr 18) and 0x3F])
        sb.append(BASE64_CHARS[(triple shr 12) and 0x3F])
        sb.append(if (i + 1 < size) BASE64_CHARS[(triple shr 6) and 0x3F] else '=')
        sb.append(if (i + 2 < size) BASE64_CHARS[triple and 0x3F] else '=')
        i += 3
    }
    return sb.toString()
}

fun String.fromBase64(): ByteArray {
    val clean = trimEnd('=')
    val out = ArrayList<Byte>(((clean.length) * 3) / 4 + 3)
    var buffer = 0
    var bitsCollected = 0
    for (c in clean) {
        val value = BASE64_CHARS.indexOf(c)
        if (value < 0) continue
        buffer = (buffer shl 6) or value
        bitsCollected += 6
        if (bitsCollected >= 8) {
            bitsCollected -= 8
            out.add(((buffer shr bitsCollected) and 0xFF).toByte())
        }
    }
    return out.toByteArray()
}
