package com.ohstem.robot_controller.di

import android.content.Context
import com.ohstem.robot_controller.voice.VoiceManager
import com.ohstem.robot_controller.voice.VoskVoiceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {

    @Provides
    @Singleton
    fun provideVoiceManager(@ApplicationContext context: Context): VoiceManager {
        return VoskVoiceManager(context)
    }
}
