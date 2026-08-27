package org.leria.eats.project.voice

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import kotlinx.cinterop.ObjCObjectVar

class IosVoiceRecognizer : VoiceRecognizer {
    private val _results = MutableStateFlow("")
    override val results = _results.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    override val isListening = _isListening.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error = _error.asStateFlow()

    private val _currentContext = MutableStateFlow<VoiceContext?>(null)
    override val currentContext = _currentContext.asStateFlow()

    // Escopo próprio para o timer de detecção de pausa (estilo Gemini).
    private val recognizerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var silenceJob: Job? = null

    // Use pt-BR locale — it has broader speech recognition support than pt-PT.
    // Falls back to device locale if pt-BR recognizer is unavailable.
    private val speechRecognizer: SFSpeechRecognizer = run {
        val ptBR = NSLocale(localeIdentifier = "pt-BR")
        val recognizer = SFSpeechRecognizer(locale = ptBR)
        if (recognizer != null && recognizer.isAvailable()) {
            recognizer
        } else {
            SFSpeechRecognizer(locale = NSLocale.currentLocale)
                ?: SFSpeechRecognizer()!!
        }
    }

    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null

    // A fresh AVAudioEngine is created for each recording session.
    // This is the safest way to avoid stale tap / format state that causes
    // AVAudioEngineImpl::InstallTapOnNode to throw NSInvalidArgumentException.
    private var audioEngine: AVAudioEngine = AVAudioEngine()

    // Tracks whether installTapOnBus succeeded so we never call
    // removeTapOnBus when no tap is installed (that also crashes).
    private var tapInstalled = false

    @OptIn(ExperimentalForeignApi::class)
    override fun startListening(context: VoiceContext) {
        // If already running, stop first
        if (audioEngine.isRunning()) {
            stopListening()
            return
        }

        // Set context for this listening session
        _currentContext.value = context

        // Guard: microphone permission
        if (AVAudioSession.sharedInstance().recordPermission() != AVAudioSessionRecordPermissionGranted) {
            _error.value = "Permissão de microfone não concedida."
            return
        }

        // Guard: speech recognition authorization
        if (SFSpeechRecognizer.authorizationStatus() !=
            SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized
        ) {
            _error.value = "Permissão de reconhecimento de voz não concedida."
            return
        }

        // Tear down any previous session completely
        cleanupSession()
        _error.value = null

        // Create a brand-new engine for this session — the safest way to
        // guarantee a clean input node with no lingering taps.
        audioEngine = AVAudioEngine()
        tapInstalled = false

        // Configure AVAudioSession — PlayAndRecord ensures the audio graph is
        // properly initialised even on the Simulator where Record alone can
        // leave the input node with an uninitialised (zero sample-rate) format.
        val audioSession = AVAudioSession.sharedInstance()
        val sessionOk = memScoped {
            val errPtr = alloc<ObjCObjectVar<NSError?>>()
            errPtr.value = null

            if (!audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, error = errPtr.ptr)) {
                _error.value = "Erro ao configurar sessão de áudio: ${errPtr.value?.localizedDescription}"
                return@memScoped false
            }
            if (!audioSession.setMode(AVAudioSessionModeMeasurement, error = errPtr.ptr)) {
                _error.value = "Erro ao configurar modo de áudio: ${errPtr.value?.localizedDescription}"
                return@memScoped false
            }
            if (!audioSession.setActive(true, withOptions = 0u, error = errPtr.ptr)) {
                _error.value = "Erro ao ativar sessão de áudio: ${errPtr.value?.localizedDescription}"
                return@memScoped false
            }
            true
        }
        if (!sessionOk) return

        // Create recognition request
        val request = SFSpeechAudioBufferRecognitionRequest().also {
            it.shouldReportPartialResults = true
        }
        recognitionRequest = request

        val inputNode = audioEngine.inputNode

        // Start recognition task
        recognitionTask = speechRecognizer.recognitionTaskWithRequest(request) { result, taskError ->
            if (result != null) {
                _results.value = result.bestTranscription.formattedString
                // Detecção de pausa (estilo Gemini): se não chegar nenhum resultado
                // novo por 1.8s, encerra a escuta automaticamente para que a
                // mensagem seja enviada assim que o utilizador terminar de falar.
                scheduleSilenceTimer()
            }
            if (taskError != null || result?.isFinal() == true) {
                silenceJob?.cancel()
                if (audioEngine.isRunning()) audioEngine.stop()
                if (tapInstalled) {
                    inputNode.removeTapOnBus(0u)
                    tapInstalled = false
                }
                _isListening.value = false
                recognitionRequest = null
                recognitionTask = null
            }
        }

        // Install tap using the input node's NATIVE format (nil = use native format).
        // Passing an explicit AVAudioFormat that mismatches the hardware format causes
        // AVAudioEngineImpl::InstallTapOnNode to throw NSInvalidArgumentException on
        // iOS Simulator. Using nil lets the engine pick the correct format automatically.
        inputNode.installTapOnBus(0u, bufferSize = 4096u, format = null) { buffer, _ ->
            buffer?.let { request.appendAudioPCMBuffer(it) }
        }
        tapInstalled = true

        // Start engine
        audioEngine.prepare()
        val engineOk = memScoped {
            val errPtr = alloc<ObjCObjectVar<NSError?>>()
            errPtr.value = null
            val started = audioEngine.startAndReturnError(errPtr.ptr)
            if (!started) {
                _error.value = "Erro ao iniciar motor de áudio: ${errPtr.value?.localizedDescription}"
            }
            started
        }

        if (engineOk) {
            _isListening.value = true
        } else {
            // Engine failed to start — remove the tap we just installed
            if (tapInstalled) {
                inputNode.removeTapOnBus(0u)
                tapInstalled = false
            }
            recognitionTask?.cancel()
            recognitionRequest = null
            recognitionTask = null
        }
    }

    override fun stopListening() {
        // Mantém o currentContext: quem observa isListening precisa saber em
        // qual tela o microfone estava a ouvir para poder enviar a mensagem
        // automaticamente. O contexto só é limpo no próximo startListening().
        cleanupSession()
    }

    override fun clearResults() {
        _results.value = ""
    }

    private fun scheduleSilenceTimer() {
        silenceJob?.cancel()
        silenceJob = recognizerScope.launch {
            delay(1800)
            if (_isListening.value) {
                stopListening()
            }
        }
    }

    /** Stops everything and returns to a clean idle state. */
    private fun cleanupSession() {
        silenceJob?.cancel()
        silenceJob = null
        if (audioEngine.isRunning()) {
            audioEngine.stop()
        }
        if (tapInstalled) {
            audioEngine.inputNode.removeTapOnBus(0u)
            tapInstalled = false
        }
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        recognitionRequest = null
        recognitionTask = null
        _isListening.value = false
    }
}