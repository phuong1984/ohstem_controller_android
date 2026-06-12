package com.ohstem.robot_controller.ble

import java.util.UUID

data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int
)

sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    object Connecting : BleConnectionState()
    data class Connected(val address: String, val name: String? = null) : BleConnectionState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

object NordicUartUuids {
    val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val RX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // App to Device
    val TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // Device to App
}
