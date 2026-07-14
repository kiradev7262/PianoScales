package com.pianoscales.learnmusic.ui.pianobuddy

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.pianoscales.learnmusic.ble.BleConnectionState
import com.pianoscales.learnmusic.ble.BleDevice
import com.pianoscales.learnmusic.ble.PianoBuddyBleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PianoBuddyViewModel @Inject constructor(
    private val bleManager: PianoBuddyBleManager
) : ViewModel() {
    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val discoveredDevices: StateFlow<List<BleDevice>> = bleManager.discoveredDevices

    fun startScan() {
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectToDevice(device: BleDevice) {
        bleManager.connectToDevice(device)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun openNRFConnect(context: Context) {
        val packageName = "no.nordicsemi.android.mcp"
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        } else {
            try {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                context.startActivity(marketIntent)
            } catch (e: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                context.startActivity(webIntent)
            }
        }
    }
}
