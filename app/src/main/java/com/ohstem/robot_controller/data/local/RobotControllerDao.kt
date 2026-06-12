package com.ohstem.robot_controller.data.local

import androidx.room.*
import com.ohstem.robot_controller.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RobotControllerDao {
    @Query("SELECT * FROM control_profiles")
    fun getAllProfiles(): Flow<List<ControlProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ControlProfile): Long

    @Query("SELECT * FROM virtual_actions WHERE profileId = :profileId")
    fun getActionsForProfile(profileId: Long): Flow<List<VirtualAction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: VirtualAction): Long

    @Query("SELECT * FROM input_bindings WHERE profileId = :profileId")
    fun getBindingsForProfile(profileId: Long): Flow<List<InputBinding>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBinding(binding: InputBinding): Long
}
