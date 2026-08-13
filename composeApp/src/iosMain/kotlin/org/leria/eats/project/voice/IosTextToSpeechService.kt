package org.leria.eats.project.voice

import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechBoundary

class IosTextToSpeechService : TextToSpeechService {

    private val synthesizer = AVSpeechSynthesizer()
    // AVSpeechBoundaryImmediate = 0 via CEnum.byValue
    private val immediate = AVSpeechBoundary.byValue(0)

    private fun stopSynthesizer() {
        synthesizer.stopSpeakingAtBoundary(immediate)
    }

    override fun speak(text: String) {
        if (synthesizer.isSpeaking()) {
            stopSynthesizer()
        }
        val utterance = AVSpeechUtterance(string = text)

        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("pt-PT")
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("pt-BR")

        utterance.rate = 0.58f
        utterance.pitchMultiplier = 1.22f
        utterance.volume = 1.0f

        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        stopSynthesizer()
    }

    override fun shutdown() {
        stopSynthesizer()
    }
}
