package com.example.pianoscales.util

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun rememberPermissionHandler(
    permission: String,
    permissionName: String,
    snackbarHostState: SnackbarHostState,
    onPermissionGranted: () -> Unit = {}
): (onGranted: (() -> Unit)?) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
            pendingAction?.invoke()
        } else {
            val activity = context as? Activity
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
            } ?: true

            scope.launch {
                if (!showRationale) {
                    val result = snackbarHostState.showSnackbar(
                        message = "$permissionName permission has been permanently denied. Please enable it in Settings.",
                        actionLabel = "Settings",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                } else {
                    snackbarHostState.showSnackbar("$permissionName permission is required.")
                }
            }
        }
        pendingAction = null
    }

    return { onGranted ->
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, permission) -> {
                onPermissionGranted()
                onGranted?.invoke()
            }
            else -> {
                pendingAction = onGranted
                permissionLauncher.launch(permission)
            }
        }
    }
}
