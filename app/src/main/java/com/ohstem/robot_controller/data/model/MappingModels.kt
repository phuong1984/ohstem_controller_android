package com.ohstem.robot_controller.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_actions")
data class VirtualAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val activationCommand: String,
    val deactivationCommand: String? = null,
    val type: String, // e.g., "BUTTON", "JOYSTICK", "VOICE", "GESTURE"
    val profileId: Long
)

@Entity(tableName = "input_bindings")
data class InputBinding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceType: String, // e.g., "GAMEPAD_BUTTON", "JOYSTICK_AXIS", "GESTURE"
    val sourceCode: String, // e.g., "TRIANGLE", "LX", "SWIPE_LEFT"
    val virtualActionId: Long,
    val profileId: Long
)

@Entity(tableName = "voice_synonyms")
data class VoiceSynonym(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inputBindingId: Long,
    val synonym: String
)

@Entity(tableName = "control_profiles")
data class ControlProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
