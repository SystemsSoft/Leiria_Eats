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
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("pt-PT")
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("pt-BR")
        utterance.rate = 0.5f
        utterance.pitchMultiplier = 1.0f
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(0L)
    }

    override fun shutdown() {
        synthesizer.stopSpeakingAtBoundary(0L)
    }
}
