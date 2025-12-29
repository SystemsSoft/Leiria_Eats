package org.leria.eats.project.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidPermissionManager(private val context: Context) : PermissionManager {

    private val _status = MutableStateFlow(PermissionStatus.IDLE)
    override val status = _status.asStateFlow()
    var launcher: (() -> Unit)? = null

    init {
        checkCurrentStatus()
    }

    private fun checkCurrentStatus() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        _status.value = if (hasPermission) PermissionStatus.GRANTED else PermissionStatus.IDLE
    }

    override fun askForPermission() {
        if (_status.value == PermissionStatus.GRANTED) return

        launcher?.invoke()
    }

    fun onPermissionResult(isGranted: Boolean) {
        _status.value = if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    override fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}