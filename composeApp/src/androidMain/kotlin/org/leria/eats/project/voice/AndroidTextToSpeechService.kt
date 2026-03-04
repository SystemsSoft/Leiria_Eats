package org.leria.eats.project.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidTextToSpeechService(private val context: Context) : TextToSpeechService {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.forLanguageTag("pt-PT"))
                isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                if (!isReady) {
                    // fallback para pt-BR se pt-PT não disponível
                    tts?.setLanguage(Locale.forLanguageTag("pt-BR"))
                    isReady = true
                }
                pendingText?.let { speak(it) }
                pendingText = null
            }
        }
    }

    override fun speak(text: String) {
        if (isReady) {
            tts?.stop()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "komaai_tts")
        } else {
            pendingText = text
        }
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

