package com.example.warehouseapp.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log

// Расширение для PrinterManager
fun PrinterManager.getPairedPrinters(): List<BluetoothDevice> {
    return try {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter?.bondedDevices?.filter { device ->
            try {
                val deviceName = device.name ?: ""
                val deviceAddress = device.address ?: ""

                // Фильтруем принтеры по имени или MAC-адресу
                deviceName.contains("Xprinter", ignoreCase = true) ||
                        deviceName.contains("Printer", ignoreCase = true) ||
                        deviceName.contains("V3BT", ignoreCase = true) ||
                        deviceAddress.startsWith("DC:0D:30", ignoreCase = true) // Типичный префикс Xprinter
            } catch (e: SecurityException) {
                Log.e("PrinterManager", "Security exception accessing device", e)
                false
            }
        }?.toList() ?: emptyList()
    } catch (e: Exception) {
        Log.e("PrinterManager", "Error getting paired printers", e)
        emptyList()
    }
}