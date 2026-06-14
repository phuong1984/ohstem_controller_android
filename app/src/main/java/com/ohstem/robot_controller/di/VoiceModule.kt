package com.ohstem.robot_controller.di

import android.content.Context
import com.ohstem.robot_controller.voice.HybridVoiceManager
import com.ohstem.robot_controller.voice.OnlineVoiceManager
import com.ohstem.robot_controller.voice.SherpaOnnxVoiceManager
import com.ohstem.robot_controller.voice.VoiceManager
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
    fun provideSherpaOnnxVoiceManager(
        @ApplicationContext context: Context,
    ): SherpaOnnxVoiceManager {
        return SherpaOnnxVoiceManager(context)
    }

    @Provides
    @Singleton
    fun provideOnlineVoiceManager(
        @ApplicationContext context: Context,
    ): OnlineVoiceManager {
        return OnlineVoiceManager(context)
    }

    @Provides
    @Singleton
    fun provideVoiceManager(
        sherpaOnnxManager: SherpaOnnxVoiceManager,
        onlineManager: OnlineVoiceManager,
    ): VoiceManager {
        return HybridVoiceManager(sherpaOnnxManager, onlineManager)
    }
}
