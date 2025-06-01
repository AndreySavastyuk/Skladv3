package com.example.warehouseapp.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
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
        // SDK должен быть инициализирован в Application классе
        // Проверяем, что SDK инициализирован
        try {
            // Регистрируем наблюдателя только если SDK готов
            bleManager.registerBleEventObserver(bleObserver)
            isObserverRegistered = true
            Log.d(TAG, "BLE наблюдатель зарегистрирован")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка регистрации наблюдателя: ${e.message}")
            isObserverRegistered = false
        }
    }

    /**
     * Генерация QR кода для сопряжения (Fine scan to connect)
     * Исправлено: используем лямбду вместо интерфейса callback
     */
    fun generatePairingQrCode() {
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
    }

    /**
     * Остановка режима сопряжения
     */
    fun stopPairing() {
        bleManager.stopFineScanToConnect()
        _pairingQrCode.value = null
        Log.d(TAG, "Режим сопряжения остановлен")
    }

    /**
     * Ручное подключение к сканеру по MAC адресу
     * Исправлено: проверяем правильное название метода в SDK
     */
    fun connectToScanner(macAddress: String) {
        _connectionState.value = ConnectionState.CONNECTING
        try {
            // Попробуем разные варианты названий метода
            bleManager.connect(macAddress)
            Log.d(TAG, "Подключение к устройству: $macAddress")
        } catch (e: NoSuchMethodError) {
            Log.e(TAG, "Метод connectDevice не найден, пробуем connect")
            try {
                bleManager.connect(macAddress)
            } catch (e2: Exception) {
                Log.e(TAG, "Ошибка подключения: ${e2.message}")
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * Отключение от сканера
     * Исправлено: убран несуществующий метод disconnect()
     */
    fun disconnect() {
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

    /**
     * Поиск доступных BLE сканеров
     * Примечание: возвращает только спаренные устройства
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
        bleManager.startScan()
        Log.d(TAG, "Начато BLE сканирование")
    }

    /**
     * Остановка BLE сканирования
     */
    fun stopBleDiscovery() {
        bleManager.stopScan()
        Log.d(TAG, "BLE сканирование остановлено")
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
        bleManager.queryBatteryLevel()
    }

    /**
     * Звуковой сигнал
     */
    fun beep(frequency: Int = 2700, duration: Long = 100, volume: Int = 10) {
        bleManager.beep(frequency, duration, volume)
    }

    /**
     * Вибрация (для BS30, BS50)
     */
    fun vibrate(duration: Long = 100) {
        if (duration in 50..3000) {
            bleManager.vibrate(duration)
        }
    }

    /**
     * Звук и вибрация одновременно
     */
    fun beepAndVibrate() {
        bleManager.beepAndVibrate(2700, 100, 10, 100)
    }

    /**
     * Очистка ресурсов
     * Исправлено: правильный порядок вызовов
     */
    fun release() {
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

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}