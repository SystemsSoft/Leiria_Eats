package org.leria.eats.project.voice

interface TextToSpeechService {
    fun speak(text: String)
    fun stop()
    fun shutdown()
}

