package org.leria.eats.project.voice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechBoundary
fun typeCheck() {
    val s = AVSpeechSynthesizer()
    val b: AVSpeechBoundary = TODO()
    s.stopSpeakingAtBoundary(b)
}
