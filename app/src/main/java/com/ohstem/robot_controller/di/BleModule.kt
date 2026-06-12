package com.ohstem.robot_controller.di

import android.content.Context
import com.ohstem.robot_controller.ble.BleManager
import com.ohstem.robot_controller.ble.BleManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BleModule {

    @Provides
    @Singleton
    fun provideBleManager(@ApplicationContext context: Context): BleManager {
        return BleManagerImpl(context)
    }
}
