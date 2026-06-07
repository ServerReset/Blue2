package com.blue2.app.wear.di

import android.content.Context
import androidx.room.Room
import com.blue2.app.data.local.database.VehicleDao
import com.blue2.app.data.local.database.VehicleStatusDao
import com.blue2.app.data.local.database.WearDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WearDatabase =
        Room.databaseBuilder(context, WearDatabase::class.java, "blue2_wear.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideVehicleDao(db: WearDatabase): VehicleDao = db.vehicleDao()

    @Provides
    fun provideVehicleStatusDao(db: WearDatabase): VehicleStatusDao = db.vehicleStatusDao()
}
