package com.ohstem.robot_controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohstem.robot_controller.ble.BleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BleViewModel @Inject constructor(
    private val bleManager: BleManager
) : ViewModel() {

    val scanResults = bleManager.scanResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isScanning = bleManager.isScanning
    val connectionState = bleManager.connectionState

    fun startScan() = bleManager.startScan()
    fun stopScan() = bleManager.stopScan()
    fun connect(address: String) = bleManager.connect(address)
    fun disconnect() = bleManager.disconnect()
}
