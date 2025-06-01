package com.example.warehouseapp

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.nlscan.ble.NlsBleManager
import com.nlscan.ble.NlsReportHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WarehouseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // ВАЖНО: Убедимся, что инициализация происходит в главном потоке
        Handler(Looper.getMainLooper()).post {
            try {
                // Инициализируем Newland BLE SDK
                NlsBleManager.getInstance().init(this)

                // Включаем сохранение логов для отладки
                NlsReportHelper.getInstance().init(this)
                NlsReportHelper.getInstance().setSaveLogEnable(true)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}