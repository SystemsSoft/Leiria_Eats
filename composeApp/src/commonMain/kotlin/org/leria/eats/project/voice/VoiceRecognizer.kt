package org.leria.eats.project.voice


import kotlinx.coroutines.flow.StateFlow

interface VoiceRecognizer {
    val results: StateFlow<String>
    val isListening: StateFlow<Boolean>
    val error: StateFlow<String?>
    val shouldAutoSend: StateFlow<Boolean>

    fun startListening()
    fun stopListening()
}

