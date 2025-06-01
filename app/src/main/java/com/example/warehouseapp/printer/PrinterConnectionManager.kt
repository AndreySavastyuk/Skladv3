package com.example.warehouseapp.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import net.posprinter.IConnectListener
import net.posprinter.IDeviceConnection
import net.posprinter.POSConnect
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Исправленный менеджер подключения к принтеру Xprinter
 * на основе официальной документации POSConnect
 */
class PrinterConnectionManager(private val context: Context) {
    companion object {
        private const val TAG = "PrinterConnectionManager"
        private const val XPRINTER_NAME_PREFIX = "Xprinter"
        private const val XPRINTER_MAC_PREFIX = "DC:0D:30" // Типичный префикс MAC для Xprinter
    }

    private var deviceConnection: IDeviceConnection? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // Состояние подключения
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    init {
        // Инициализация SDK
        try {
            POSConnect.init(context.applicationContext)
            Log.d(TAG, "POSConnect SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize POSConnect SDK", e)
        }
    }

    /**
     * Получение списка спаренных принтеров
     */
    fun getPairedPrinters(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.filter { device ->
                try {
                    val deviceName = device.name ?: ""
                    val deviceAddress = device.address ?: ""

                    // Фильтруем по имени или MAC-адресу
                    deviceName.contains(XPRINTER_NAME_PREFIX, ignoreCase = true) ||
                            deviceAddress.startsWith(XPRINTER_MAC_PREFIX, ignoreCase = true) ||
                            deviceName.contains("V3BT", ignoreCase = true) ||
                            deviceName.contains("Printer", ignoreCase = true)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception accessing device info", e)
                    false
                }
            }?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception accessing paired devices", e)
            emptyList()
        }
    }

    /**
     * Асинхронное подключение к принтеру
     */
    suspend fun connectToPrinter(macAddress: String): Result<IDeviceConnection> =
        suspendCancellableCoroutine { continuation ->
            try {
                _connectionState.value = ConnectionState.CONNECTING

                // Закрываем предыдущее соединение
                disconnect()

                // Создаем новое Bluetooth соединение
                deviceConnection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)

                Log.d(TAG, "Attempting to connect to printer: $macAddress")

                // Подключаемся используя MAC-адрес
                deviceConnection?.connect(macAddress, object : IConnectListener {
                    override fun onStatus(code: Int, connectInfo: String?, message: String?) {
                        Log.d(TAG, "Connection status - Code: $code, Info: $connectInfo, Message: $message")

                        when (code) {
                            POSConnect.CONNECT_SUCCESS -> {
                                Log.i(TAG, "Successfully connected to printer")
                                _connectionState.value = ConnectionState.CONNECTED

                                deviceConnection?.let { connection ->
                                    if (continuation.isActive) {
                                        continuation.resume(Result.success(connection))
                                    }
                                } ?: run {
                                    _connectionState.value = ConnectionState.DISCONNECTED
                                    if (continuation.isActive) {
                                        continuation.resume(
                                            Result.failure(PrinterException("Connection object is null"))
                                        )
                                    }
                                }
                            }

                            POSConnect.CONNECT_FAIL -> {
                                Log.e(TAG, "Failed to connect: $message")
                                _connectionState.value = ConnectionState.DISCONNECTED
                                if (continuation.isActive) {
                                    continuation.resume(
                                        Result.failure(PrinterException("Connection failed: $message"))
                                    )
                                }
                            }

                            POSConnect.CONNECT_INTERRUPT -> {
                                Log.w(TAG, "Connection interrupted")
                                _connectionState.value = ConnectionState.DISCONNECTED
                                if (continuation.isActive) {
                                    continuation.resume(
                                        Result.failure(PrinterException("Connection interrupted"))
                                    )
                                }
                            }

                            else -> {
                                Log.w(TAG, "Unknown connection status: $code")
                                if (continuation.isActive) {
                                    continuation.resume(
                                        Result.failure(PrinterException("Unknown connection status: $code"))
                                    )
                                }
                            }
                        }
                    }
                }) ?: run {
                    Log.e(TAG, "Failed to create connection - deviceConnection is null")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    continuation.resume(
                        Result.failure(PrinterException("Failed to create device connection"))
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception during connection", e)
                _connectionState.value = ConnectionState.DISCONNECTED
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }

            // Обработка отмены
            continuation.invokeOnCancellation {
                Log.d(TAG, "Connection cancelled")
                disconnect()
            }
        }

    /**
     * Отключение от принтера
     */
    fun disconnect() {
        try {
            deviceConnection?.close()
            deviceConnection = null
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.d(TAG, "Disconnected from printer")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }

    /**
     * Получение текущего соединения
     */
    fun getConnection(): IDeviceConnection? = deviceConnection

    /**
     * Проверка подключения
     */
    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    /**
     * Освобождение ресурсов
     */
    fun release() {
        disconnect()
        try {
            POSConnect.exit()
            Log.d(TAG, "POSConnect SDK released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing POSConnect SDK", e)
        }
    }
}