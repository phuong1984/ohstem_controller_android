package com.ohstem.robot_controller.ble

import com.ohstem.robot_controller.data.model.InputBinding
import com.ohstem.robot_controller.data.model.VirtualAction
import com.ohstem.robot_controller.repository.MappingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MappingEngine @Inject constructor(
    private val repository: MappingRepository,
    private val bleManager: BleManager
) {
    suspend fun handleInput(sourceType: String, sourceCode: String, isActivation: Boolean = true): String {
        val profiles = repository.getProfiles().first()
        val activeProfile = profiles.find { it.isActive } ?: return ""
        val bindings = repository.getBindings(activeProfile.id).first()
        val actions = repository.getActions(activeProfile.id).first()
        val binding = bindings.find { it.sourceType == sourceType && it.sourceCode == sourceCode }
        val action = binding?.let { actions.find { a -> a.id == it.virtualActionId } }
        val command = if (isActivation) action?.activationCommand else action?.deactivationCommand
        command?.let {
            bleManager.sendCommand(it)
            return it
        } ?: return ""
    }

    suspend fun handleJoystick(axis: String, value: Float): String {
        val intValue = (value * 100).toInt().coerceIn(-100, 100)
        val command = "$axis=$intValue"
        bleManager.sendCommand(command)
        return command
    }
}
