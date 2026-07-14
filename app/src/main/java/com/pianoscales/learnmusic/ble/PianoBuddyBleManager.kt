package com.pianoscales.learnmusic.ble

import kotlinx.coroutines.flow.StateFlow

enum class BleConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

interface PianoBuddyBleManager {
    val connectionState: StateFlow<BleConnectionState>
    fun connect()
    fun disconnect()
    fun sendMidiNote(midiNote: Int)
}
