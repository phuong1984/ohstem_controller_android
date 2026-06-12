package com.ohstem.robot_controller.ble

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BleManager {
    val scanResults: Flow<List<BleDevice>>
    val isScanning: StateFlow<Boolean>
    val connectionState: StateFlow<BleConnectionState>
    
    fun startScan()
    fun stopScan()
    fun connect(address: String)
    fun disconnect()
    fun sendCommand(command: String)
}
