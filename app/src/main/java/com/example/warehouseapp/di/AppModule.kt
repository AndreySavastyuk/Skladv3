package com.example.warehouseapp.di

import android.content.Context
import androidx.room.Room
import com.example.warehouseapp.database.WarehouseDatabase
import com.example.warehouseapp.network.NetworkManager
import com.example.warehouseapp.printer.PrinterManager
import com.example.warehouseapp.scanner.NewlandBleManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWarehouseDatabase(
        @ApplicationContext context: Context
    ): WarehouseDatabase {
        return Room.databaseBuilder(
            context,
            WarehouseDatabase::class.java,
            "warehouse_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideProductDao(database: WarehouseDatabase) = database.productDao()

    @Provides
    @Singleton
    fun provideNetworkManager(
        @ApplicationContext context: Context
    ): NetworkManager {
        return NetworkManager(context)
    }

    @Provides
    @Singleton
    fun providePrinterManager(
        @ApplicationContext context: Context
    ): PrinterManager {
        return PrinterManager(context)
    }

    @Provides
    @Singleton
    fun provideNewlandBleManager(
        @ApplicationContext context: Context
    ): NewlandBleManager {
        return NewlandBleManager(context)
    }
}