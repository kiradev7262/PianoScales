package com.pianoscales.learnmusic.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.pianoscales.learnmusic.BuildConfig
import com.pianoscales.learnmusic.audio.NoteEventDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PianoBuddyBLE"
private const val SCAN_TIMEOUT_MS = 10000L

@Singleton
class PianoBuddyBleManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteEventDispatcher: NoteEventDispatcher
) : PianoBuddyBleManager {

    private val _connectionState = MutableStateFlow(BleConnectionState.IDLE)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    
    // Nordic UART Service UUIDs (Common for ESP32 BLE Serial)
    private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val CHARACTERISTIC_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e") // RX for ESP32

    private val scope = CoroutineScope(Dispatchers.IO)
    private var scanJob: Job? = null
    private var lastSentMidi: Int? = null

    init {
        scope.launch {
            noteEventDispatcher.noteEvents.collect { event ->
                if (_connectionState.value == BleConnectionState.CONNECTED) {
                    sendMidiNote(event.midiNote, event.frequency)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScan() {
        Log.d(TAG, "Checking Bluetooth...")
        _connectionState.value = BleConnectionState.CHECKING_BLUETOOTH
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter not found")
            _connectionState.value = BleConnectionState.FAILED
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth is disabled")
            _connectionState.value = BleConnectionState.BLUETOOTH_DISABLED
            return
        }

        Log.d(TAG, "Scan started")
        _connectionState.value = BleConnectionState.SCANNING
        _discoveredDevices.value = emptyList()

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "BLE Scanner not available")
            _connectionState.value = BleConnectionState.FAILED
            return
        }

        scanJob?.cancel()
        scanJob = scope.launch {
            delay(SCAN_TIMEOUT_MS)
            if (_connectionState.value == BleConnectionState.SCANNING) {
                stopScan()
                if (_discoveredDevices.value.isEmpty()) {
                    Log.w(TAG, "Scan timeout - no devices found")
                    _connectionState.value = BleConnectionState.TIMEOUT
                }
            }
        }

        scanner.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        Log.d(TAG, "Stopping scan")
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        scanJob?.cancel()
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name
            val deviceAddress = if (BuildConfig.DEBUG) device.address else ""
            val rssi = result.rssi

            val bleDevice = BleDevice(deviceName, device.address, rssi)
            
            if (!_discoveredDevices.value.any { it.address == bleDevice.address }) {
                Log.d(TAG, "Device found: $deviceName ($deviceAddress) RSSI: $rssi")
                val newList = _discoveredDevices.value + bleDevice
                _discoveredDevices.value = newList

                // Auto-connect if only one compatible device found so far and it's a Piano Buddy
                if (newList.size == 1 && isCompatible(bleDevice)) {
                    Log.d(TAG, "Single compatible device found, auto-connecting...")
                    stopScan()
                    connectToDevice(bleDevice)
                } else if (newList.size > 1) {
                    _connectionState.value = BleConnectionState.DEVICE_FOUND
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            _connectionState.value = BleConnectionState.FAILED
        }
    }

    private fun isCompatible(device: BleDevice): Boolean {
        return device.name == "Piano Buddy" || device.name == "ESP32_Piano"
    }

    @SuppressLint("MissingPermission")
    override fun connectToDevice(device: BleDevice) {
        Log.d(TAG, "Connecting to ${device.name ?: "Unknown"} (${if (BuildConfig.DEBUG) device.address else "..."})")
        _connectionState.value = BleConnectionState.CONNECTING
        
        val remoteDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        if (remoteDevice == null) {
            Log.e(TAG, "Remote device not found")
            _connectionState.value = BleConnectionState.FAILED
            return
        }

        bluetoothGatt = remoteDevice.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        Log.d(TAG, "Disconnecting...")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        lastSentMidi = null
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    override fun sendMidiNote(midiNote: Int, frequency: Float) {
        // Rule 4: Silence
        if (midiNote == -1) {
            lastSentMidi = null
            return
        }

        // Rule 2: Same Note Continues
        if (midiNote == lastSentMidi) return

        val reason = if (lastSentMidi == null) "First Detection" else "Note Changed"
        lastSentMidi = midiNote

        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID) ?: return
        
        val payload = byteArrayOf(midiNote.toByte())
        characteristic.value = payload
        gatt.writeCharacteristic(characteristic)

        // Instrumentation Logging
        if (BuildConfig.DEBUG) {
            val note = com.pianoscales.learnmusic.theory.Note.entries[(midiNote % 12 + 12) % 12]
            val octave = (midiNote / 12) - 1
            Log.d(TAG, """
                ========== PianoBuddy TX ==========
                Reason      : $reason
                Mode        : External Piano

                Frequency   : ${if (frequency > 0) String.format(Locale.US, "%.2f Hz", frequency) else "N/A"}
                Detected    : ${note.displayName}$octave
                Octave      : $octave
                MIDI        : $midiNote

                BLE Payload : [${payload.joinToString { it.toString() }}]
                ===================================
            """.trimIndent())
        }
    }

    @SuppressLint("MissingPermission")
    override fun sendGuidedMidiNotes(currentMidi: Int, nextMidi: Int, currentFrequency: Float) {
        // Apply same state-based logic for Guided Practice
        if (currentMidi == lastSentMidi) return

        val reason = if (lastSentMidi == null) "First Detection" else "Note Changed"
        lastSentMidi = currentMidi

        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID) ?: return

        val payload = byteArrayOf(currentMidi.toByte(), nextMidi.toByte())
        characteristic.value = payload
        gatt.writeCharacteristic(characteristic)

        // Instrumentation Logging
        if (BuildConfig.DEBUG) {
            val curNote = com.pianoscales.learnmusic.theory.Note.entries[(currentMidi % 12 + 12) % 12]
            val curOctave = (currentMidi / 12) - 1
            val nextNote = com.pianoscales.learnmusic.theory.Note.entries[(nextMidi % 12 + 12) % 12]
            val nextOctave = (nextMidi / 12) - 1
            
            Log.d(TAG, """
                ========== PianoBuddy TX ==========
                Reason      : $reason
                Mode        : Guided Practice

                Current Note : ${curNote.displayName}$curOctave
                Current MIDI : $currentMidi
                ${if (currentFrequency > 0) "Frequency    : ${String.format(Locale.US, "%.2f Hz", currentFrequency)}" else ""}

                Next Note    : ${nextNote.displayName}$nextOctave
                Next MIDI    : $nextMidi

                BLE Payload  : [${payload.joinToString { it.toString() }}]
                ===================================
            """.trimIndent())
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected. Discovering services...")
                _connectionState.value = BleConnectionState.DISCOVERING_SERVICES
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected")
                _connectionState.value = BleConnectionState.DISCONNECTED
                bluetoothGatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                
                if (service != null && characteristic != null) {
                    Log.d(TAG, "Services and characteristic discovered. Ready.")
                    _connectionState.value = BleConnectionState.CONNECTED
                } else {
                    Log.e(TAG, "Required service/characteristic not found")
                    disconnect()
                    _connectionState.value = BleConnectionState.FAILED
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
                disconnect()
                _connectionState.value = BleConnectionState.FAILED
            }
        }
    }
}
