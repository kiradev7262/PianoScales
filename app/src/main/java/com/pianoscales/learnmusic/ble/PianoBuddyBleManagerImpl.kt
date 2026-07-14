package com.pianoscales.learnmusic.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.pianoscales.learnmusic.audio.NoteEventDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PianoBuddyBleManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteEventDispatcher: NoteEventDispatcher
) : PianoBuddyBleManager {

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    
    // Nordic UART Service UUIDs (Common for ESP32 BLE Serial)
    private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val CHARACTERISTIC_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e") // RX for ESP32

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            noteEventDispatcher.noteEvents.collect { midiNote ->
                if (_connectionState.value == BleConnectionState.CONNECTED) {
                    sendMidiNote(midiNote)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        
        _connectionState.value = BleConnectionState.CONNECTING
        
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            _connectionState.value = BleConnectionState.DISCONNECTED
            return
        }
        
        scanner.startScan(object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val deviceName = result.device.name
                if (deviceName == "Piano Buddy" || deviceName == "ESP32_Piano") {
                    scanner.stopScan(this)
                    bluetoothGatt = result.device.connectGatt(context, false, gattCallback)
                }
            }
            
            override fun onScanFailed(errorCode: Int) {
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    override fun sendMidiNote(midiNote: Int) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID) ?: return
        
        characteristic.value = byteArrayOf(midiNote.toByte())
        gatt.writeCharacteristic(characteristic)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = BleConnectionState.DISCONNECTED
                bluetoothGatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.CONNECTED
            } else {
                disconnect()
            }
        }
    }
}
