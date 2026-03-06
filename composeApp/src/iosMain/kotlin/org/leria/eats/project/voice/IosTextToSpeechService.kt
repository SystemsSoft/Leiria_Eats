package org.leria.eats.project.voice

import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechSynthesisVoice

class IosTextToSpeechService : TextToSpeechService {

    private val synthesizer = AVSpeechSynthesizer()

    override fun speak(text: String) {
        if (synthesizer.isSpeaking()) {
            synthesizer.stopSpeakingAtBoundary(0L) // 0 = AVSpeechBoundaryImmediate
        }
        val utterance = AVSpeechUtterance(string = text)

        // Try to get a Portuguese (Portugal) female voice first
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("pt-PT")
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("pt-BR")

        // Configure for animated and spontaneous voice
        utterance.rate = 0.55f // Slightly faster for spontaneous feel (range: 0.0-1.0, default: 0.5)
        utterance.pitchMultiplier = 1.15f // Higher pitch for more animated feminine voice (default: 1.0)
        utterance.volume = 1.0f // Full volume for clear delivery

        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(0L)
    }

    override fun shutdown() {
        synthesizer.stopSpeakingAtBoundary(0L)
    }
}
