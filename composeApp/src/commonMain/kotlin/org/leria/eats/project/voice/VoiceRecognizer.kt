package org.leria.eats.project.voice


import kotlinx.coroutines.flow.StateFlow

enum class VoiceContext {
    ONBOARDING,
    AI_SEARCH,
    HOME_SEARCH
}

interface VoiceRecognizer {
    val results: StateFlow<String>
    val isListening: StateFlow<Boolean>
    val error: StateFlow<String?>
    val currentContext: StateFlow<VoiceContext?>

    fun startListening(context: VoiceContext)
    fun stopListening()
    fun clearResults()
}

