package com.ohstem.robot_controller.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ohstem.robot_controller.ble.BleConnectionState
import com.ohstem.robot_controller.ble.BleDevice
import com.ohstem.robot_controller.ui.theme.ConnectedGreen
import com.ohstem.robot_controller.ui.theme.DisconnectedRed
import com.ohstem.robot_controller.ui.theme.WarningOrange
import com.ohstem.robot_controller.viewmodel.BleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: BleViewModel = hiltViewModel()) {
    val scanResults by viewModel.scanResults.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    // Permission Handling
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("OhStem Robot Controller") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ConnectionStatus(connectionState)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (connectionState is BleConnectionState.Connected) {
                    Button(
                        onClick = { viewModel.disconnect() },
                        colors = ButtonDefaults.buttonColors(containerColor = DisconnectedRed)
                    ) {
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        onClick = { launcher.launch(permissionsToRequest) },
                        enabled = !isScanning
                    ) {
                        Text("Start Scan")
                    }
                }
                
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }

                if (connectionState !is BleConnectionState.Connected) {
                    Button(
                        onClick = { viewModel.stopScan() },
                        enabled = isScanning
                    ) {
                        Text("Stop Scan")
                    }
                }
            }

            val connectedAddress = (connectionState as? BleConnectionState.Connected)?.address
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(scanResults) { device ->
                    DeviceItem(
                        device = device,
                        isConnected = device.address == connectedAddress,
                        onClick = { viewModel.connect(device.address) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionStatus(state: BleConnectionState) {
    val text = when (state) {
        is BleConnectionState.Disconnected -> "Disconnected"
        is BleConnectionState.Connecting -> "Connecting..."
        is BleConnectionState.Connected -> "Connected"
        is BleConnectionState.Reconnecting -> "Reconnecting... (${state.attempt}/${state.maxAttempts})"
        is BleConnectionState.Error -> "Error: ${state.message}"
    }
    
    val backgroundColor = when (state) {
        is BleConnectionState.Connected -> com.ohstem.robot_controller.ui.theme.ConnectedGreen
        is BleConnectionState.Reconnecting -> WarningOrange
        is BleConnectionState.Error -> DisconnectedRed
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = when (state) {
        is BleConnectionState.Connected, is BleConnectionState.Error, is BleConnectionState.Reconnecting -> androidx.compose.ui.graphics.Color.White
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun DeviceItem(device: BleDevice, isConnected: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isConnected) ConnectedGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isConnected) 2.dp else 0.dp
    ) {
        ListItem(
            headlineContent = { Text(device.name ?: "Unknown Device") },
            supportingContent = { Text(device.address) },
            trailingContent = {
                if (isConnected) {
                    Text("Connected", color = ConnectedGreen)
                } else {
                    Text("${device.rssi} dBm")
                }
            },
            leadingContent = if (isConnected) {
                {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ConnectedGreen)
                    )
                }
            } else null
        )
    }
}
