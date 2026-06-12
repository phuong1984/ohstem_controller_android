package com.ohstem.robot_controller.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ohstem.robot_controller.data.model.*

@Database(
    entities = [
        VirtualAction::class,
        InputBinding::class,
        VoiceSynonym::class,
        ControlProfile::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RobotControllerDatabase : RoomDatabase() {
    abstract fun robotControllerDao(): RobotControllerDao
}
