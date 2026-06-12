package com.ohstem.robot_controller.repository

import com.ohstem.robot_controller.data.local.RobotControllerDao
import com.ohstem.robot_controller.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MappingRepository @Inject constructor(
    private val dao: RobotControllerDao
) {
    fun getProfiles(): Flow<List<ControlProfile>> = dao.getAllProfiles()
    
    suspend fun createProfile(name: String, isActive: Boolean = false): Long {
        return dao.insertProfile(ControlProfile(name = name, isActive = isActive))
    }

    fun getActions(profileId: Long): Flow<List<VirtualAction>> = dao.getActionsForProfile(profileId)
    
    fun getBindings(profileId: Long): Flow<List<InputBinding>> = dao.getBindingsForProfile(profileId)

    suspend fun saveAction(action: VirtualAction) = dao.insertAction(action)
    
    suspend fun saveBinding(binding: InputBinding) = dao.insertBinding(binding)
}
