package com.pianoscales.learnmusic.ui.pianobuddy

import androidx.lifecycle.ViewModel
import com.pianoscales.learnmusic.ble.BleConnectionState
import com.pianoscales.learnmusic.ble.PianoBuddyBleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PianoBuddyViewModel @Inject constructor(
    private val bleManager: PianoBuddyBleManager
) : ViewModel() {
    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState

    fun connect() {
        bleManager.connect()
    }

    fun disconnect() {
        bleManager.disconnect()
    }
}
