package com.example.warehouseapp.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nlscan.ble.NlsBleManager
import com.nlscan.ble.NlsBleDevice
import com.nlscan.ble.NlsBleDefaultEventObserver
import com.nlscan.ble.NlsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер для работы с Newland BLE сканерами
 * Поддерживает модели: HR32-BT, MT90, BS30, BS50 и другие
 */
@Singleton
class NewlandBleManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "NewlandBleManager"
    }

    // Экземпляр SDK менеджера
    private val bleManager = NlsBleManager.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Состояние подключения
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // Результат сканирования
    private val _scanResult = MutableStateFlow("")
    val scanResult: StateFlow<String> = _scanResult

    // QR код для сопряжения
    private val _pairingQrCode = MutableStateFlow<Bitmap?>(null)
    val pairingQrCode: StateFlow<Bitmap?> = _pairingQrCode

    // Текущее подключенное устройство
    private var currentDevice: NlsBleDevice? = null

    // Callback для обработки результатов сканирования
    private var onScanCallback: ((String) -> Unit)? = null

    // Флаг регистрации наблюдателя
    private var isObserverRegistered = false

    // Наблюдатель за событиями BLE
    private val bleObserver = object : NlsBleDefaultEventObserver() {
        override fun onConnectionStateChanged(device: NlsBleDevice) {
            currentDevice = device

            when (device.connectionState) {
                NlsBleManager.CONNECTION_STATE_CONNECTED -> {
                    Log.i(TAG, "Сканер подключен: ${device.address}")
                    _connectionState.value = ConnectionState.CONNECTED
                    // Начинаем прием данных
                    bleManager.startScan()
                }
                NlsBleManager.CONNECTION_STATE_DISCONNECTED -> {
                    Log.i(TAG, "Сканер отключен")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    currentDevice = null
                }
                NlsBleManager.CONNECTION_STATE_CONNECTING -> {
                    Log.i(TAG, "Подключение к сканеру...")
                    _connectionState.value = ConnectionState.CONNECTING
                }
            }
        }

        override fun onScanDataReceived(data: String) {
            // Данные уже в UTF-8, включая кириллицу
            Log.d(TAG, "Получены данные: $data")
            _scanResult.value = data
            onScanCallback?.invoke(data)
        }

        override fun onBatteryLevelRead(result: NlsResult<Int>) {
            if (result.retSucceed()) {
                Log.d(TAG, "Уровень заряда: ${result.result}%")
            }
        }
    }

    init {
        // Регистрируем наблюдателя после небольшой задержки
        mainHandler.postDelayed({
            try {
                bleManager.registerBleEventObserver(bleObserver)
                isObserverRegistered = true
                Log.d(TAG, "BLE наблюдатель зарегистрирован")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка регистрации наблюдателя: ${e.message}")
                isObserverRegistered = false
            }
        }, 100)
    }

    /**
     * Генерация QR кода для сопряжения (Fine scan to connect)
     * Исправлено: обертываем в Handler для выполнения в главном потоке
     */
    fun generatePairingQrCode() {
        mainHandler.post {
            try {
                bleManager.generateConnectCodeBitmap { bitmap: Bitmap? ->
                    if (bitmap != null) {
                        _pairingQrCode.value = bitmap
                        // Запускаем режим сопряжения
                        bleManager.startFineScanToConnect()
                        Log.i(TAG, "QR код для сопряжения создан успешно")
                    } else {
                        Log.e(TAG, "Не удалось создать QR код для сопряжения")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при генерации QR кода: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Остановка режима сопряжения
     */
    fun stopPairing() {
        mainHandler.post {
            try {
                bleManager.stopFineScanToConnect()
                _pairingQrCode.value = null
                Log.d(TAG, "Режим сопряжения остановлен")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при остановке сопряжения: ${e.message}")
            }
        }
    }

    /**
     * Ручное подключение к сканеру по MAC адресу
     */
    fun connectToScanner(macAddress: String) {
        mainHandler.post {
            _connectionState.value = ConnectionState.CONNECTING
            try {
                bleManager.connect(macAddress)
                Log.d(TAG, "Подключение к устройству: $macAddress")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка подключения: ${e.message}")
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * Отключение от сканера
     */
    fun disconnect() {
        mainHandler.post {
            try {
                bleManager.stopScan()
                bleManager.stopFineScanToConnect()
                currentDevice = null
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.d(TAG, "Отключено от сканера")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отключении: ${e.message}")
            }
        }
    }

    /**
     * Поиск доступных BLE сканеров
     */
    fun searchDevices(): List<BluetoothDevice> {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()

        // Проверка разрешений Bluetooth
        try {
            val bondedDevices = bluetoothAdapter.bondedDevices ?: return emptyList()
            return bondedDevices.filter { device ->
                try {
                    val name = device.name ?: return@filter false
                    name.contains("Newland", ignoreCase = true) ||
                            name.contains("HR32", ignoreCase = true) ||
                            name.contains("MT90", ignoreCase = true) ||
                            name.contains("BS30", ignoreCase = true) ||
                            name.contains("BS50", ignoreCase = true)
                } catch (se: SecurityException) {
                    Log.e(TAG, "Нет разрешения для доступа к имени устройства: ${se.message}")
                    false
                }
            }.toList()
        } catch (se: SecurityException) {
            Log.e(TAG, "Нет разрешения для доступа к спаренным устройствам: ${se.message}")
            return emptyList()
        }
    }

    /**
     * Активное BLE сканирование новых устройств
     */
    fun startBleDiscovery() {
        mainHandler.post {
            try {
                bleManager.startScan()
                Log.d(TAG, "Начато BLE сканирование")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при запуске сканирования: ${e.message}")
            }
        }
    }

    /**
     * Остановка BLE сканирования
     */
    fun stopBleDiscovery() {
        mainHandler.post {
            try {
                bleManager.stopScan()
                Log.d(TAG, "BLE сканирование остановлено")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при остановке сканирования: ${e.message}")
            }
        }
    }

    /**
     * Установка callback для получения результатов сканирования
     */
    fun setScanCallback(callback: (String) -> Unit) {
        onScanCallback = callback
    }

    /**
     * Запрос уровня заряда батареи
     */
    fun queryBatteryLevel() {
        mainHandler.post {
            try {
                bleManager.queryBatteryLevel()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при запросе уровня батареи: ${e.message}")
            }
        }
    }

    /**
     * Звуковой сигнал
     */
    fun beep(frequency: Int = 2700, duration: Long = 100, volume: Int = 10) {
        mainHandler.post {
            try {
                bleManager.beep(frequency, duration, volume)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при звуковом сигнале: ${e.message}")
            }
        }
    }

    /**
     * Вибрация (для BS30, BS50)
     */
    fun vibrate(duration: Long = 100) {
        mainHandler.post {
            if (duration in 50..3000) {
                try {
                    bleManager.vibrate(duration)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при вибрации: ${e.message}")
                }
            }
        }
    }

    /**
     * Звук и вибрация одновременно
     */
    fun beepAndVibrate() {
        mainHandler.post {
            try {
                bleManager.beepAndVibrate(2700, 100, 10, 100)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при звуке и вибрации: ${e.message}")
            }
        }
    }

    /**
     * Очистка ресурсов
     */
    fun release() {
        mainHandler.post {
            try {
                disconnect()
                if (isObserverRegistered) {
                    bleManager.unregisterBleEventObserver(bleObserver)
                    isObserverRegistered = false
                    Log.d(TAG, "BLE наблюдатель отписан")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при освобождении ресурсов: ${e.message}")
            }
        }
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}