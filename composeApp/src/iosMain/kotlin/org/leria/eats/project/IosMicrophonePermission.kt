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
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue


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
        // Must run on the main thread — AVAudioSession and SFSpeechRecognizer
        // both require main-thread access for permission requests on iOS.
        dispatch_async(dispatch_get_main_queue()) {
            // Step 1: Request microphone permission
            AVAudioSession.sharedInstance().requestRecordPermission { micGranted ->
                if (!micGranted) {
                    dispatch_async(dispatch_get_main_queue()) {
                        _status.value = PermissionStatus.DENIED
                    }
                    return@requestRecordPermission
                }
                // Step 2: Request speech recognition permission
                SFSpeechRecognizer.requestAuthorization { speechStatus ->
                    // Callback may arrive on a background queue — dispatch to main
                    dispatch_async(dispatch_get_main_queue()) {
                        _status.value =
                            if (speechStatus == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
                                PermissionStatus.GRANTED
                            } else {
                                PermissionStatus.DENIED
                            }
                    }
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