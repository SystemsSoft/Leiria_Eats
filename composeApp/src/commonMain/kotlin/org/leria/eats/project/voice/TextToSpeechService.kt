package org.leria.eats.project.voice

interface TextToSpeechService {
    fun speak(text: String)
    // Toca um áudio (PCM16) já sintetizado, sem chamada de rede — usado quando o texto já
    // foi sintetizado antecipadamente (ver SearchViewModel.fetchSearch) para tocar no
    // instante exato em que a mensagem é revelada, sem esperar a síntese de novo.
    fun playAudio(pcm: ByteArray)
    fun stop()
    fun shutdown()
}

