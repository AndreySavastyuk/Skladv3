package com.example.warehouseapp.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import android.util.Log
import androidx.core.content.ContextCompat

// Расширение для PrinterManager
fun PrinterManager.getPairedPrinters(context: Context): List<BluetoothDevice> {
    return try {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Проверка разрешений для Android 12+
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // До Android 12 это разрешение не требуется явно
        }

        if (!hasPermission) {
            Log.e("PrinterManager", "Не хватает разрешения BLUETOOTH_CONNECT")
            return emptyList()
        }

        bluetoothAdapter?.bondedDevices?.filter { device ->
            try {
                val deviceName = device.name ?: ""
                val deviceAddress = device.address ?: ""

                // Фильтруем принтеры по имени или MAC-адресу
                deviceName.contains("Xprinter", ignoreCase = true) ||
                        deviceName.contains("Printer", ignoreCase = true) ||
                        deviceName.contains("V3BT", ignoreCase = true) ||
                        deviceAddress.startsWith("10:23:81:5B:DA:29", ignoreCase = true) // Типичный префикс Xprinter
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