package com.ohstem.robot_controller.di

import android.content.Context
import com.ohstem.robot_controller.gesture.GestureManager
import com.ohstem.robot_controller.gesture.MediaPipeGestureManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GestureModule {

    @Provides
    @Singleton
    fun provideGestureManager(@ApplicationContext context: Context): GestureManager {
        return MediaPipeGestureManager(context)
    }
}
