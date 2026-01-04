package org.leria.eats.project.voice


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AndroidVoiceRecognizer(private val context: Context) : VoiceRecognizer {

    private val _results = MutableStateFlow("")
    override val results = _results.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    override val isListening = _isListening.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error = _error.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    override fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            _error.value = null

            // É importante rodar na Main Thread no Android
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(recognitionListener)

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Importante para ver o texto aparecendo
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
            }
        } else {
            _error.value = "Reconhecimento de voz não disponível neste dispositivo."
        }
    }

    override fun stopListening() {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            _isListening.value = false
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { _isListening.value = false }

        override fun onError(error: Int) {
            _isListening.value = false
            _error.value = "Erro no reconhecimento: $error"
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _results.value = matches[0]
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _results.value = matches[0]
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}