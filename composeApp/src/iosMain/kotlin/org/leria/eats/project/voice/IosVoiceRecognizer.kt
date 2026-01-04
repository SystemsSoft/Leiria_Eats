package org.leria.eats.project.voice

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognitionTask
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

class IosVoiceRecognizer : VoiceRecognizer {
    private val _results = MutableStateFlow("")
    override val results = _results.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    override val isListening = _isListening.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error = _error.asStateFlow()

    private val speechRecognizer = SFSpeechRecognizer(locale = NSLocale.currentLocale)
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private val audioEngine = AVAudioEngine()

    @OptIn(ExperimentalForeignApi::class)
    override fun startListening() {
        if (audioEngine.isRunning()) {
            stopListening()
            return
        }

        _error.value = null
        val audioSession = AVAudioSession.sharedInstance()
        try {
            audioSession.setCategory(AVAudioSessionCategoryRecord, error = null)
            audioSession.setMode(AVAudioSessionModeMeasurement, error = null)
            audioSession.setActive(true, withOptions = 0u, error = null)
        } catch (e: Exception) {
            _error.value = "Erro na sessão de áudio"
            return
        }

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest().apply {
            shouldReportPartialResults = true
        }

        val inputNode = audioEngine.inputNode
        val request = recognitionRequest ?: return

        recognitionTask = speechRecognizer?.recognitionTaskWithRequest(request) { result, error ->
            if (result != null) {
                _results.value = result.bestTranscription.formattedString
            }

            if (error != null || (result?.isFinal() == true)) {
                audioEngine.stop()
                inputNode.removeTapOnBus(0u)
                _isListening.value = false
            }
        }

        val recordingFormat = inputNode.outputFormatForBus(0u)
        inputNode.installTapOnBus(0u, bufferSize = 1024u, format = recordingFormat) { buffer, _ ->
            buffer?.let { request.appendAudioPCMBuffer(it) }
        }

        audioEngine.prepare()
        try {
            audioEngine.startAndReturnError(null)
            _isListening.value = true
        } catch (e: Exception) {
            _error.value = "Não foi possível iniciar o motor de áudio."
        }
    }

    override fun stopListening() {
        audioEngine.stop()
        recognitionRequest?.endAudio()
        _isListening.value = false
    }
}