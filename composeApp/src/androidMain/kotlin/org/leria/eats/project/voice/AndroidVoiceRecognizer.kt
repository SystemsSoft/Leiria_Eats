package org.leria.eats.project.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AndroidVoiceRecognizer(private val context: Context) : VoiceRecognizer {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // --- CONTROLE DE ESTADO ---
    // Essa variável é a chave: Ela diz se o usuário clicou em PAUSE ou não.
    private var userWantsToListen = false

    // Aqui guardamos tudo o que você já falou antes da pausa
    private var accumulatedText = ""

    // Timer para detectar pausa e enviar automaticamente (estilo Gemini)
    private var autoPauseTimer: Runnable? = null
    private var lastTextReceived = ""

    // --- FLUXOS (Observables) ---
    private val _results = MutableStateFlow("")
    override val results: StateFlow<String> = _results.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val _shouldAutoSend = MutableStateFlow(false)
    override val shouldAutoSend: StateFlow<Boolean> = _shouldAutoSend.asStateFlow()

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createListener())
        } else {
            _error.value = "Reconhecimento de voz não disponível."
        }
    }

    private fun createListener() = object : RecognitionListener {

        // 1. ENQUANTO VOCÊ FALA (Tempo Real)
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val currentPhrase = matches[0]
                lastTextReceived = currentPhrase
                // Mostra na tela: O que já estava salvo + O que você está falando agora
                _results.value = "$accumulatedText $currentPhrase".trim()

                // Cancela o timer anterior e inicia um novo (a pessoa ainda está falando)
                cancelAutoPauseTimer()
                scheduleAutoPauseTimer()
            }
        }

        // 2. QUANDO O ANDROID ACHA QUE VOCÊ TERMINOU
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val finalPhrase = matches[0]
                // Salva na memória permanente para não perder quando reiniciar
                accumulatedText = "$accumulatedText $finalPhrase".trim()
                _results.value = accumulatedText
                lastTextReceived = finalPhrase
            }

            // A LÓGICA DO "SÓ PARA NO PAUSE":
            // Se o usuário NÃO clicou em pause (userWantsToListen == true),
            // a gente reinicia o microfone imediatamente!
            if (userWantsToListen) {
                // Agenda o auto-envio antes de reiniciar
                scheduleAutoPauseTimer()
                restartListening()
            } else {
                _isListening.value = false
            }
        }

        override fun onError(errorCode: Int) {
            // Se o usuário quer ouvir, ignoramos erros de "tempo esgotado" e reiniciamos
            if (userWantsToListen) {
                if (errorCode == SpeechRecognizer.ERROR_NO_MATCH ||
                    errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    restartListening()
                    return
                }
            }

            // Se for erro real (sem internet, etc)
            if (errorCode != SpeechRecognizer.ERROR_NO_MATCH && errorCode != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                val message = when (errorCode) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sem permissão"
                    SpeechRecognizer.ERROR_NETWORK -> "Erro de conexão"
                    else -> "Erro $errorCode"
                }
                _error.value = message
                _isListening.value = false
                userWantsToListen = false
            }
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() { _isListening.value = true }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            // Não marcamos como false aqui, esperamos o onResults decidir
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // Função para reiniciar o microfone sem travar a UI
    private fun restartListening() {
        mainHandler.postDelayed({
            if (userWantsToListen) {
                startListeningIntent()
            }
        }, 50)
    }

    private fun startListeningIntent() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

                // Tempo de silêncio reduzido para 1.8s para detecção rápida de pausa (estilo Gemini)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800)
            }
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _error.value = "Erro: ${e.message}"
            userWantsToListen = false
        }
    }


    override fun startListening() {
        mainHandler.post {
            userWantsToListen = true
            accumulatedText = ""
            _results.value = ""
            _shouldAutoSend.value = false // Reset auto-send flag
            lastTextReceived = ""
            cancelAutoPauseTimer()
            startListeningIntent()
        }
    }

    override fun stopListening() {
        mainHandler.post {
            userWantsToListen = false // Desativa a flag (Agora pode parar de verdade)
            cancelAutoPauseTimer()
            speechRecognizer?.stopListening()
            _isListening.value = false
        }
    }

    // ── Funções para gerenciar o timer de auto-pausa (estilo Gemini) ──
    private fun scheduleAutoPauseTimer() {
        // Cancela qualquer timer anterior
        cancelAutoPauseTimer()

        // Cria novo timer: se passar 1.8s sem novos resultados, envia automaticamente
        autoPauseTimer = Runnable {
            if (userWantsToListen && _results.value.isNotBlank()) {
                // Detectou pausa significativa - sinaliza para enviar automaticamente
                _shouldAutoSend.value = true
                userWantsToListen = false
                speechRecognizer?.stopListening()
                _isListening.value = false
            }
        }

        // Agenda para 1.8 segundos (mesmo tempo do silêncio do recognizer)
        mainHandler.postDelayed(autoPauseTimer!!, 1800)
    }

    private fun cancelAutoPauseTimer() {
        autoPauseTimer?.let {
            mainHandler.removeCallbacks(it)
            autoPauseTimer = null
        }
    }
}

