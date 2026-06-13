package com.ohstem.robot_controller.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManagerImpl @Inject constructor(
    private val context: Context
) : BleManager {
    private val TAG = "BleManager"
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter
    private val scanner = adapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    private val commandQueue = ConcurrentLinkedQueue<String>()
    @Volatile private var isWriting = false

    private var lastConnectedAddress: String? = null
    private var userInitiatedDisconnect = false
    private var isAutoReconnecting = false
    private var autoReconnectJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _scanResults = MutableStateFlow<List<BleDevice>>(emptyList())
    override val scanResults = _scanResults

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val RECONNECT_DELAY_MS = 2000L
        private const val CONNECTION_WAIT_MS = 1500L
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT Status Error: $status")
                gatt.close()
                handleDisconnection()
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.")
                lastConnectedAddress = gatt.device.address
                _connectionState.value = BleConnectionState.Connected(gatt.device.address, gatt.device.name)
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.")
                gatt.close()
                handleDisconnection()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(NordicUartUuids.SERVICE_UUID)
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(NordicUartUuids.RX_CHAR_UUID)
                    txCharacteristic = service.getCharacteristic(NordicUartUuids.TX_CHAR_UUID)
                    
                    Log.d(TAG, "UART Service discovered. RX: ${rxCharacteristic != null}, TX: ${txCharacteristic != null}")
                } else {
                    Log.e(TAG, "UART Service NOT found!")
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            isWriting = false
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Write completed successfully")
            } else {
                Log.w(TAG, "Write failed with status: $status")
            }
            processQueue()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name ?: result.scanRecord?.deviceName
            val device = BleDevice(
                name = deviceName,
                address = result.device.address,
                rssi = result.rssi
            )
            val currentList = _scanResults.value.toMutableList()
            val index = currentList.indexOfFirst { it.address == device.address }
            if (index != -1) {
                currentList[index] = device
            } else {
                currentList.add(device)
            }
            _scanResults.value = currentList.sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScan() {
        if (adapter == null || !adapter.isEnabled) return
        _scanResults.value = emptyList()
        _isScanning.value = true
        scanner?.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        _isScanning.value = false
        scanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    override fun connect(address: String) {
        cancelAutoReconnect()
        userInitiatedDisconnect = false
        lastConnectedAddress = address
        performConnect(address)
    }

    private fun performConnect(address: String) {
        stopScan()
        val device = adapter?.getRemoteDevice(address)
        if (device == null) {
            _connectionState.value = BleConnectionState.Error("Device not found")
            return
        }
        _connectionState.value = BleConnectionState.Connecting
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        cancelAutoReconnect()
        userInitiatedDisconnect = true
        lastConnectedAddress = null
        bluetoothGatt?.disconnect()
    }

    @SuppressLint("MissingPermission")
    override fun sendCommand(command: String) {
        commandQueue.offer(command)
        processQueue()
    }

    @SuppressLint("MissingPermission")
    private fun processQueue() {
        if (isWriting) return
        val command = commandQueue.poll() ?: return
        val char = rxCharacteristic ?: return
        isWriting = true
        char.value = command.toByteArray()
        bluetoothGatt?.writeCharacteristic(char)
        Log.d(TAG, "Sending command: $command (${commandQueue.size} queued)")
    }

    private fun handleDisconnection() {
        bluetoothGatt = null
        rxCharacteristic = null
        txCharacteristic = null
        if (!userInitiatedDisconnect && lastConnectedAddress != null && !isAutoReconnecting) {
            startAutoReconnect()
        } else {
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    private fun startAutoReconnect() {
        autoReconnectJob = scope.launch {
            isAutoReconnecting = true
            val address = lastConnectedAddress ?: return@launch
            for (attempt in 1..MAX_RECONNECT_ATTEMPTS) {
                _connectionState.value = BleConnectionState.Reconnecting(attempt, MAX_RECONNECT_ATTEMPTS)
                delay(RECONNECT_DELAY_MS)
                if (lastConnectedAddress == null || userInitiatedDisconnect) break
                performConnect(address)
                delay(CONNECTION_WAIT_MS)
                if (_connectionState.value is BleConnectionState.Connected) {
                    isAutoReconnecting = false
                    return@launch
                }
            }
            isAutoReconnecting = false
            if (_connectionState.value !is BleConnectionState.Connected) {
                _connectionState.value = BleConnectionState.Disconnected
            }
        }
    }

    private fun cancelAutoReconnect() {
        autoReconnectJob?.cancel()
        autoReconnectJob = null
        isAutoReconnecting = false
    }
}
