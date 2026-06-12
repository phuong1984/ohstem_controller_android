package com.ohstem.robot_controller.di

import android.content.Context
import androidx.room.Room
import com.ohstem.robot_controller.data.local.RobotControllerDao
import com.ohstem.robot_controller.data.local.RobotControllerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RobotControllerDatabase {
        return Room.databaseBuilder(
            context,
            RobotControllerDatabase::class.java,
            "robot_controller_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideDao(database: RobotControllerDatabase): RobotControllerDao {
        return database.robotControllerDao()
    }
}
