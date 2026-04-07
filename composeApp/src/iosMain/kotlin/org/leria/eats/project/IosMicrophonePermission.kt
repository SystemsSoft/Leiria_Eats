package org.leria.eats.project.permissions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.Foundation.NSURL
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString


class IosPermissionManager : PermissionManager {
    private val _status = MutableStateFlow(PermissionStatus.IDLE)
    override val status = _status.asStateFlow()

    init {
        refreshStatus()
    }

    /**
     * Checks both microphone AND speech recognition authorization.
     * Only GRANTED when both are authorized.
     */
    override fun refreshStatus() {
        val micStatus = AVAudioSession.sharedInstance().recordPermission()
        val speechStatus = SFSpeechRecognizer.authorizationStatus()

        _status.value = when {
            micStatus == AVAudioSessionRecordPermissionGranted &&
            speechStatus == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized -> PermissionStatus.GRANTED

            micStatus == AVAudioSessionRecordPermissionDenied ||
            speechStatus == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusDenied ||
            speechStatus == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusRestricted -> PermissionStatus.DENIED

            else -> PermissionStatus.IDLE
        }
    }

    override fun askForPermission() {
        // Step 1: Request microphone permission
        AVAudioSession.sharedInstance().requestRecordPermission { micGranted ->
            if (!micGranted) {
                _status.value = PermissionStatus.DENIED
                return@requestRecordPermission
            }
            // Step 2: Request speech recognition permission
            SFSpeechRecognizer.requestAuthorization { speechStatus ->
                _status.value = if (speechStatus == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
                    PermissionStatus.GRANTED
                } else {
                    PermissionStatus.DENIED
                }
            }
        }
    }

    override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
        }
    }
}