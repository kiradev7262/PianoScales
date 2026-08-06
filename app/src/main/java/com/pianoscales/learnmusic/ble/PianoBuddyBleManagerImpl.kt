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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PianoBuddyBLE"
private const val SCAN_TIMEOUT_MS = 10000L
private const val REQUIRED_CONFIRMATION_FRAMES = 3

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
    private val bleMutex = Mutex()
    @Volatile private var lastSentFeedbackMidi: Int? = null
    @Volatile private var lastSentTargetMidi: Int? = null

    // Candidate Confirmation State (Debounce)
    private var pendingMidi: Int? = null
    private var pendingCount = 0

    init {
        scope.launch {
            noteEventDispatcher.noteEvents.collect { event ->
                if (_connectionState.value == BleConnectionState.CONNECTED) {
                    sendMidiNote(event.midiNote, event.frequency)
                }
            }
        }
        scope.launch {
            noteEventDispatcher.activeNotes.collect { notes ->
                if (_connectionState.value == BleConnectionState.CONNECTED) {
                    sendActiveNotes(notes)
                }
            }
        }
        // Ensure latest active notes are sent upon connection
        scope.launch {
            _connectionState.collect { state ->
                if (state == BleConnectionState.CONNECTED) {
                    sendActiveNotes(noteEventDispatcher.activeNotes.value)
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
            val rssi = result.rssi

            val bleDevice = BleDevice(deviceName, device.address, rssi)
            
            if (!_discoveredDevices.value.any { it.address == bleDevice.address }) {
                Log.d(TAG, "Device found: $deviceName (${device.address}) RSSI: $rssi")
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
        Log.d(TAG, "Connecting to ${device.name ?: "Unknown"} (${device.address})")
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
        lastSentFeedbackMidi = null
        lastSentTargetMidi = null
        pendingMidi = null
        pendingCount = 0
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    override fun sendMidiNote(midiNote: Int, frequency: Float) {
        // Bypass for Virtual Piano / App Audio
        if (frequency <= 0f) {
            handleVirtualMidiNote(midiNote)
            return
        }

        // Rule 4: Silence
        if (midiNote == -1) {
            if (pendingMidi != null || lastSentFeedbackMidi != null) {
                Log.d("SongCoach_BLE", "Silence detected - Resetting candidate state. lastSentFeedbackMidi was $lastSentFeedbackMidi")
            }
            pendingMidi = null
            pendingCount = 0
            lastSentFeedbackMidi = null
            return
        }

        // Case 1: Same Confirmed Note
        if (midiNote == lastSentFeedbackMidi) {
            if (pendingMidi != null) {
                Log.d("SongCoach_BLE", "sendMidiNote($midiNote) - Discarding candidate $pendingMidi because already confirmed")
                pendingMidi = null
                pendingCount = 0
            }
            return
        }

        // Case 2 & 3: Candidate logic
        if (midiNote == pendingMidi) {
            pendingCount++
        } else {
            pendingMidi = midiNote
            pendingCount = 1
        }

        // Promotion Rule
        if (pendingCount >= REQUIRED_CONFIRMATION_FRAMES) {
            val reason = if (lastSentFeedbackMidi == null) "First Detection" else "Note Changed"
            pendingMidi = null
            pendingCount = 0
            
            scope.launch {
                performBleWriteWithRetry(midiNote, frequency, "External Piano ($reason)", isTarget = false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun sendTargetNote(midiNote: Int, frequency: Float) {
        scope.launch {
            performBleWriteWithRetry(midiNote, frequency, "Guided Practice", isTarget = true)
        }
    }

    private var blinkJob: Job? = null

    override fun sendTargetNoteWithBlink(midiNote: Int, frequency: Float) {
        Log.d("SongCoach_BLE", "Blink Requested for $midiNote")
        blinkJob?.cancel()
        blinkJob = scope.launch {
            Log.d("SongCoach_BLE", "Blink Started for $midiNote")
            performBleWriteWithRetry(-1, 0f, "Blink (OFF)", isTarget = true)
            delay(150) // Increased delay for stability
            performBleWriteWithRetry(midiNote, frequency, "Blink (ON)", isTarget = true)
            Log.d("SongCoach_BLE", "Blink Completed for $midiNote")
        }
    }

    private suspend fun performBleWriteWithRetry(midiNote: Int, frequency: Float, reason: String, isTarget: Boolean): Boolean {
        return bleMutex.withLock {
            val lastSent = if (isTarget) lastSentTargetMidi else lastSentFeedbackMidi
            if (midiNote == lastSent && midiNote != -1) {
                Log.d("SongCoach_BLE", "Any skipped BLE transmission: $midiNote. Reason: Already set (isTarget=$isTarget).")
                return@withLock true
            }

            var success = false
            repeat(3) { attempt ->
                if (performBleWriteDirect(midiNote, frequency, "$reason (Attempt ${attempt + 1})", isTarget = isTarget)) {
                    if (isTarget) {
                        lastSentTargetMidi = if (midiNote == -1) -1 else midiNote
                    } else {
                        lastSentFeedbackMidi = if (midiNote == -1) -1 else midiNote
                    }
                    success = true
                    return@withLock true
                }
                delay(50)
            }
            
            Log.w("SongCoach_BLE", "Failed to send $midiNote after 3 attempts")
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleVirtualMidiNote(midiNote: Int) {
        if (midiNote == -1) {
            lastSentFeedbackMidi = null
            // For virtual silence, we send the polyphonic clear command
            performBleWriteDirect(-1, 0f, "Virtual Piano / App Audio (Silence)", isTarget = false)
            return
        }

        if (midiNote == lastSentFeedbackMidi) return
        
        lastSentFeedbackMidi = midiNote
        performBleWriteDirect(midiNote, 0f, "Virtual Piano / App Audio", isTarget = false)
    }

    @SuppressLint("MissingPermission")
    private fun performBleWriteDirect(midiNote: Int, frequency: Float, reason: String, isTarget: Boolean): Boolean {
        val gatt = bluetoothGatt
        if (gatt == null) {
            Log.w("SongCoach_BLE", "performBleWriteDirect($midiNote) - Failed: GATT null")
            return false
        }
        val service = gatt.getService(SERVICE_UUID)
        if (service == null) {
            Log.w("SongCoach_BLE", "performBleWriteDirect($midiNote) - Failed: Service null")
            return false
        }
        val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
        if (characteristic == null) {
            Log.w("SongCoach_BLE", "performBleWriteDirect($midiNote) - Failed: Characteristic null")
            return false
        }
        
        val payload = if (midiNote == -1) {
            if (isTarget) byteArrayOf() else byteArrayOf(0xFE.toByte())
        } else {
            if (isTarget) byteArrayOf(midiNote.toByte())
            else byteArrayOf(0xFE.toByte(), midiNote.toByte())
        }
        characteristic.value = payload
        val success = gatt.writeCharacteristic(characteristic)
        
        if (success) {
            if (midiNote == -1) {
                Log.d("SongCoach_BLE", "BLE OFF sent ($reason, isTarget=$isTarget)")
            } else {
                Log.d("SongCoach_BLE", "BLE ON sent: $midiNote ($reason, isTarget=$isTarget)")
            }
        } else {
            Log.w("SongCoach_BLE", "BLE Write Failed: $midiNote ($reason, isTarget=$isTarget)")
        }
        
        return success
    }

    @SuppressLint("MissingPermission")
    private fun sendActiveNotes(notes: Set<Int>) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID) ?: return

        // New Packet Format: [COMMAND_BYTE, note1, note2, ...]
        // 0xFE (254) is our command byte for Polyphonic Active Notes
        val payload = ByteArray(notes.size + 1)
        payload[0] = 0xFE.toByte()
        notes.forEachIndexed { index, note ->
            payload[index + 1] = note.toByte()
        }
        
        characteristic.value = payload
        val success = gatt.writeCharacteristic(characteristic)

        // Instrumentation Logging
        if (BuildConfig.DEBUG) {
            val noteNames = notes.map { midiNote ->
                val noteName = com.pianoscales.learnmusic.theory.Note.entries[(midiNote % 12 + 12) % 12].displayName
                val octave = (midiNote / 12) - 1
                "$noteName$octave"
            }
            Log.d(TAG, """
                ========== PianoBuddy TX (Polyphonic) ==========
                Reason      : Buddy Active Notes
                Mode        : Virtual Piano
                Status      : ${if (success) "Success" else "Failed"}

                Notes       : ${noteNames.joinToString(" ")}
                MIDI        : ${notes.toList()}
                Packet Size : ${payload.size}

                BLE Payload : [${payload.joinToString { it.toString() }}]
                ================================================
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
