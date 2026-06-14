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
    private var cache: Map<Pair<String, String>, Pair<String?, String?>>? = null

    private suspend fun loadCache() {
        val profiles = repository.getProfiles().first()
        val activeProfile = profiles.find { it.isActive } ?: return
        val bindings = repository.getBindings(activeProfile.id).first()
        val actions = repository.getActions(activeProfile.id).first()
        val map = mutableMapOf<Pair<String, String>, Pair<String?, String?>>()
        for (b in bindings) {
            val a = actions.find { it.id == b.virtualActionId }
            if (a != null) {
                map[Pair(b.sourceType, b.sourceCode)] = Pair(a.activationCommand, a.deactivationCommand)
            }
        }
        cache = map
    }

    suspend fun handleInput(sourceType: String, sourceCode: String, isActivation: Boolean = true): String {
        if (cache == null) loadCache()
        val entry = cache?.get(Pair(sourceType, sourceCode))
        val command = if (entry != null) {
            if (isActivation) entry.first else entry.second
        } else {
            cache = null
            loadCache()
            val retry = cache?.get(Pair(sourceType, sourceCode))
            if (retry != null) {
                if (isActivation) retry.first else retry.second
            } else null
        }
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
