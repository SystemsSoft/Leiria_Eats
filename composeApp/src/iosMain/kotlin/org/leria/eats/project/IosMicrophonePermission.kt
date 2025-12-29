package org.leria.eats.project.permissions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

class IosPermissionManager : PermissionManager {
    private val _status = MutableStateFlow(PermissionStatus.IDLE)
    override val status = _status.asStateFlow()

    init {
        checkStatus()
    }

    private fun checkStatus() {
        val currentStatus = AVAudioSession.sharedInstance().recordPermission()
        _status.value = when (currentStatus) {
            AVAudioSessionRecordPermissionGranted -> PermissionStatus.GRANTED
            AVAudioSessionRecordPermissionDenied -> PermissionStatus.DENIED
            else -> PermissionStatus.IDLE
        }
    }

    override fun askForPermission() {
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            _status.value = if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
        }
    }

    override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
        }
    }
}