package com.pianoscales.learnmusic.ble

import kotlinx.coroutines.flow.StateFlow

enum class BleConnectionState {
    IDLE,
    CHECKING_BLUETOOTH,
    BLUETOOTH_DISABLED,
    CHECKING_PERMISSIONS,
    SCANNING,
    DEVICE_FOUND,
    CONNECTING,
    DISCOVERING_SERVICES,
    CONNECTED,
    FAILED,
    DISCONNECTED,
    TIMEOUT
}

data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int
)

interface PianoBuddyBleManager {
    val connectionState: StateFlow<BleConnectionState>
    val discoveredDevices: StateFlow<List<BleDevice>>
    
    fun startScan()
    fun stopScan()
    fun connectToDevice(device: BleDevice)
    fun disconnect()
    fun sendMidiNote(midiNote: Int, frequency: Float = 0f)
    fun sendTargetNote(midiNote: Int, frequency: Float = 0f)
}
