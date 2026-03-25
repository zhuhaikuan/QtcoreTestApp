package com.lenovo.quantum.test.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.lenovo.quantum.sdk.logging.logE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

//Include here any new permission
private val androidPermissions= mapOf(
    Permissions.AUDIO to Manifest.permission.RECORD_AUDIO
)

actual open class MicrophonePermissionHandler (
    private val context: Context,
    private val activity: Activity?,
    private val onPermissionResult: (Boolean) -> Unit
) {
    private val _microphonePermissionStatus = MutableStateFlow(checkInitialPermission())
    actual open val microphonePermissionStatus: StateFlow<MicrophonePermissionStatus> = _microphonePermissionStatus

    private fun checkInitialPermission(): MicrophonePermissionStatus {
        return if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            MicrophonePermissionStatus.GRANTED
        } else {
            MicrophonePermissionStatus.NOTDETERMINED
        }
    }

    actual open fun checkPermission(): MicrophonePermissionStatus {
        val currentAndroidStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        val newStatus = if (currentAndroidStatus == PackageManager.PERMISSION_GRANTED) {
            MicrophonePermissionStatus.GRANTED
        } else {
            if (activity?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == true) {
                MicrophonePermissionStatus.DENIED
            } else {
                MicrophonePermissionStatus.NOTDETERMINED
            }
        }
        _microphonePermissionStatus.value = newStatus
        return newStatus
    }

    actual open fun requestPermission() {
        if (_microphonePermissionStatus.value != MicrophonePermissionStatus.GRANTED) {
            _microphonePermissionStatus.value = MicrophonePermissionStatus.NOTDETERMINED
        }
    }

    fun onPermissionRequested(granted: Boolean) {
        val newStatus = if (granted) MicrophonePermissionStatus.GRANTED else MicrophonePermissionStatus.DENIED
        _microphonePermissionStatus.value = newStatus
        onPermissionResult(granted)
    }

}

@SuppressLint("ContextCastToActivity")
@Composable
actual fun rememberMicrophonePermissionHandler(permission: Permissions): MicrophonePermissionHandler {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity

    val permissionHandlerState = remember {
        MutableStateFlow(
            if (androidPermissions[permission]
                    ?.let { ContextCompat.checkSelfPermission(context, it) }
                == PackageManager.PERMISSION_GRANTED
            ) MicrophonePermissionStatus.GRANTED
            else MicrophonePermissionStatus.NOTDETERMINED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionHandlerState.value = if (isGranted) MicrophonePermissionStatus.GRANTED else MicrophonePermissionStatus.DENIED
    }

    val handler = remember(context, activity) {
        object : MicrophonePermissionHandler(context, activity, onPermissionResult = { isGranted ->
            permissionHandlerState.value = if (isGranted) MicrophonePermissionStatus.GRANTED else MicrophonePermissionStatus.DENIED
        }) {
            override fun requestPermission() {
                if (activity == null) {
                    logE { "Activity is null, cannot request permission." }
                    permissionHandlerState.value = MicrophonePermissionStatus.DENIED
                    return
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    permissionHandlerState.value = MicrophonePermissionStatus.GRANTED
                }
            }

            override val microphonePermissionStatus: StateFlow<MicrophonePermissionStatus>
                get() = permissionHandlerState

            override fun checkPermission(): MicrophonePermissionStatus {
                val currentAndroidStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                val newStatus = if (currentAndroidStatus == PackageManager.PERMISSION_GRANTED) {
                    MicrophonePermissionStatus.GRANTED
                } else {
                    if (activity?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == true) {
                        MicrophonePermissionStatus.DENIED
                    } else {
                        MicrophonePermissionStatus.NOTDETERMINED
                    }
                }
                permissionHandlerState.value = newStatus
                return newStatus
            }
        }
    }
    return handler
}