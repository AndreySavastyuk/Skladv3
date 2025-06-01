package com.example.warehouseapp

import android.app.Application
import com.nlscan.ble.NlsBleManager
import com.nlscan.ble.NlsReportHelper

class WarehouseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // ВАЖНО: Инициализируем Newland BLE SDK ДО любого использования
        NlsBleManager.getInstance().init(this)

        // Включаем сохранение логов для отладки
        NlsReportHelper.getInstance().init(this)
        NlsReportHelper.getInstance().setSaveLogEnable(true)
    }
}