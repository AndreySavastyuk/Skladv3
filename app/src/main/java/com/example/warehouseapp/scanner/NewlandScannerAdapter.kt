package com.example.warehouseapp.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

/**
 * Адаптер для работы с BLE сканером Newland через nlsblesdk.aar
 * Поддерживает сканеры: HR32-BT, MT90 и другие модели Newland с BLE
 */
class NewlandScannerAdapter(private val context: Context) {

    companion object {
        private const val TAG = "NewlandScannerAdapter"

        // UUID для Newland сканеров
        private const val SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb"
        private const val CHAR_UUID_NOTIFY = "0000fff1-0000-1000-8000-00805f9b34fb"
        private const val CHAR_UUID_WRITE = "0000fff2-0000-1000-8000-00805f9b34fb"
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    private val _scanResult = MutableStateFlow("")
    val scanResult: StateFlow<String> = _scanResult

    private var onScanCallback: ((String) -> Unit)? = null

    // TODO: Здесь будет интеграция с nlsblesdk
    // private lateinit var nlsScanner: NlsScanner // из nlsblesdk.aar

    enum class BleConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }
    private fun checkBluetoothPermissions(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED

        val hasAdminPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADMIN
        ) == PackageManager.PERMISSION_GRANTED

        // Для Android 12+ (API 31+) требуются дополнительные разрешения
        val hasConnectPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return hasPermission && hasAdminPermission && hasConnectPermission
    }

    /**
     * Поиск доступных BLE сканеров
     */
    fun searchDevices(): List<BluetoothDevice> {
        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "Нет необходимых разрешений для Bluetooth")
            return emptyList()
        }

        return try {
            bluetoothAdapter?.bondedDevices?.filter { device ->
                try {
                    val deviceName = device.name ?: ""
                    deviceName.contains("Newland", ignoreCase = true) ||
                            deviceName.contains("HR32", ignoreCase = true) ||
                            deviceName.contains("MT90", ignoreCase = true)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Ошибка доступа к имени устройства", e)
                    false
                }
            }?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Ошибка доступа к списку спаренных устройств", e)
            emptyList()
        }
    }

    /**
     * Подключение к сканеру
     */
    suspend fun connect(device: BluetoothDevice): Boolean {
        try {
            _connectionState.value = BleConnectionState.CONNECTING

            if (!checkBluetoothPermissions()) {
                Log.e(TAG, "Нет необходимых разрешений для Bluetooth")
                _connectionState.value = BleConnectionState.ERROR
                return false
            }

            // Безопасно получаем имя устройства
            val deviceName = try {
                device.name ?: "Unknown device"
            } catch (e: SecurityException) {
                "Unknown device"
            }

            Log.i(TAG, "Подключение к $deviceName (${device.address})")

            // Временная заглушка
            _connectionState.value = BleConnectionState.CONNECTED
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка подключения", e)
            _connectionState.value = BleConnectionState.ERROR
            return false
        }
    }


    /**
     * Отключение от сканера
     */
    fun disconnect() {
        try {
            // TODO: Использовать nlsblesdk
            // nlsScanner?.disconnect()

            _connectionState.value = BleConnectionState.DISCONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отключения", e)
        }
    }

    /**
     * Установка callback для получения результатов сканирования
     */
    fun setScanCallback(callback: (String) -> Unit) {
        onScanCallback = callback

        // TODO: Установить callback в nlsblesdk
        // nlsScanner?.setOnBarcodeScannedListener { barcode ->
        //     onScanCallback?.invoke(barcode)
        //     _scanResult.value = barcode
        // }
    }

    /**
     * Включение/выключение подсветки сканера
     */
    fun toggleLight(enable: Boolean) {
        // TODO: Использовать nlsblesdk
        // nlsScanner?.enableLight(enable)
    }

    /**
     * Включение/выключение звукового сигнала
     */
    fun toggleBeep(enable: Boolean) {
        // TODO: Использовать nlsblesdk
        // nlsScanner?.enableBeep(enable)
    }

    /**
     * Настройка режима сканирования
     */
    fun setScanMode(continuous: Boolean) {
        // TODO: Использовать nlsblesdk
        // nlsScanner?.setScanMode(if (continuous) ScanMode.CONTINUOUS else ScanMode.SINGLE)
    }

    /**
     * Проверка поддержки BLE
     */
    fun isBleSupported(): Boolean {
        return bluetoothAdapter != null
    }

    /**
     * Проверка включен ли Bluetooth
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
}