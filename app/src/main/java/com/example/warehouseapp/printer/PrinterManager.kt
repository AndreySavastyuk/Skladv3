package com.example.warehouseapp.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.*
import android.util.Log
import com.example.warehouseapp.data.Product
import com.example.warehouseapp.data.TaskItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.posprinter.IConnectListener
import net.posprinter.IDeviceConnection
import net.posprinter.POSConnect
import net.posprinter.TSPLConst
import net.posprinter.TSPLPrinter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Менеджер для работы с принтером Xprinter V3BT
 * Использует POSPrinter SDK из printer-lib-3.2.0.aar
 */
@Singleton
class PrinterManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PrinterManager"
        private const val CONNECTION_TIMEOUT_MS = 10000L

        // Размеры этикетки
        private const val LABEL_WIDTH_MM = 57.0
        private const val LABEL_HEIGHT_MM = 40.0
        private const val DPI = 203
    }

    private var deviceConnection: IDeviceConnection? = null
    private var tsplPrinter: TSPLPrinter? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // Состояние подключения
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // Состояние печати
    private val _printingState = MutableStateFlow(PrintingState.IDLE)
    val printingState: StateFlow<PrintingState> = _printingState

    init {
        // Инициализация SDK
        initializeSdk()
    }

    /**
     * Инициализация SDK принтера
     */
    private fun initializeSdk() {
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

                    // Фильтруем принтеры по имени или MAC-адресу
                    deviceName.contains("Xprinter", ignoreCase = true) ||
                            deviceName.contains("Printer", ignoreCase = true) ||
                            deviceName.contains("V3BT", ignoreCase = true) ||
                            deviceAddress.startsWith("DC:0D:30", ignoreCase = true)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception accessing device", e)
                    false
                }
            }?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paired printers", e)
            emptyList()
        }
    }

    /**
     * Подключение к принтеру по MAC-адресу
     */
    suspend fun connectToPrinter(macAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.CONNECTING
            Log.d(TAG, "Attempting to connect to printer: $macAddress")

            // Закрываем предыдущее соединение
            disconnect()

            val connected = withTimeout(CONNECTION_TIMEOUT_MS) {
                connectAsync(macAddress)
            }

            if (connected) {
                _connectionState.value = ConnectionState.CONNECTED
                Log.i(TAG, "Successfully connected to printer")

                // Звуковой сигнал подтверждения
                try {
                    tsplPrinter?.sound(2, 100)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to play connection sound", e)
                }

                return@withContext true
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.w(TAG, "Failed to connect to printer")
                return@withContext false
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.e(TAG, "Connection error", e)
            return@withContext false
        }
    }

    /**
     * Асинхронное подключение с использованием корутин
     */
    private suspend fun connectAsync(macAddress: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            try {
                // Создаем новое Bluetooth соединение
                deviceConnection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)

                deviceConnection?.connect(macAddress, object : IConnectListener {
                    override fun onStatus(code: Int, connectInfo: String?, message: String?) {
                        Log.d(TAG, "Connection status: code=$code, info=$connectInfo, msg=$message")

                        when (code) {
                            POSConnect.CONNECT_SUCCESS -> {
                                Log.i(TAG, "Connection successful")
                                // Инициализируем TSPL принтер
                                deviceConnection?.let { connection ->
                                    tsplPrinter = TSPLPrinter(connection)
                                    if (continuation.isActive) {
                                        continuation.resume(true)
                                    }
                                } ?: run {
                                    Log.e(TAG, "Device connection is null after success")
                                    if (continuation.isActive) {
                                        continuation.resume(false)
                                    }
                                }
                            }
                            POSConnect.CONNECT_FAIL -> {
                                Log.e(TAG, "Connection failed: $message")
                                if (continuation.isActive) {
                                    continuation.resume(false)
                                }
                            }
                            POSConnect.CONNECT_INTERRUPT -> {
                                Log.w(TAG, "Connection interrupted")
                                if (continuation.isActive) {
                                    continuation.resume(false)
                                }
                            }
                            else -> {
                                Log.w(TAG, "Unknown connection status: $code")
                                if (continuation.isActive) {
                                    continuation.resume(false)
                                }
                            }
                        }
                    }
                }) ?: run {
                    Log.e(TAG, "Failed to create connection")
                    continuation.resume(false)
                }

                // Обработка отмены корутины
                continuation.invokeOnCancellation {
                    Log.d(TAG, "Connection cancelled")
                    disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during connection", e)
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }

    /**
     * Отключение от принтера
     */
    fun disconnect() {
        try {
            deviceConnection?.close()
            deviceConnection = null
            tsplPrinter = null
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.d(TAG, "Disconnected from printer")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }

    /**
     * Печать этикетки приемки для продукта
     */
    suspend fun printLabel(product: Product): Boolean = withContext(Dispatchers.IO) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.e(TAG, "Принтер не подключен")
            return@withContext false
        }

        try {
            _printingState.value = PrintingState.PRINTING

            // Парсим QR данные
            val qrParts = product.qrCode.split("=")
            val labelData = LabelData(
                partNumber = if (qrParts.size >= 3) qrParts[2] else product.name,
                description = if (qrParts.size >= 4) qrParts[3] else product.name,
                orderNumber = if (qrParts.size >= 2) qrParts[1] else (product.orderNumber ?: ""),
                location = product.storageLocation,
                quantity = product.quantity,
                qrData = product.qrCode,
                acceptanceDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    .format(product.receivedDate),
                labelType = "Приемка"
            )

            // Создаем и печатаем этикетку
            val format = AcceptanceLabelFormat57x40()
            val bitmap = format.createBitmap(labelData)
            printBitmap(bitmap, format)

            _printingState.value = PrintingState.SUCCESS
            Log.i(TAG, "Label printed successfully")

            return@withContext true

        } catch (e: Exception) {
            _printingState.value = PrintingState.ERROR
            Log.e(TAG, "Print error", e)
            return@withContext false
        } finally {
            // Сброс состояния через 2 секунды
            kotlinx.coroutines.delay(2000)
            _printingState.value = PrintingState.IDLE
        }
    }

    /**
     * Печать этикетки комплектации
     */
    suspend fun printShipmentLabel(item: TaskItem, quantity: Int): Boolean = withContext(Dispatchers.IO) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return@withContext false
        }

        try {
            _printingState.value = PrintingState.PRINTING

            val labelData = LabelData(
                partNumber = item.productName,
                description = item.productName,
                orderNumber = "Комплектация",
                location = item.storageLocation,
                quantity = quantity,
                qrData = "${item.productId}=$quantity",
                labelType = "Комплектация"
            )

            // Создаем и печатаем этикетку
            val format = PickingLabelFormat57x40()
            val bitmap = format.createBitmap(labelData)
            printBitmap(bitmap, format)

            _printingState.value = PrintingState.SUCCESS
            return@withContext true

        } catch (e: Exception) {
            _printingState.value = PrintingState.ERROR
            Log.e(TAG, "Print shipment label error", e)
            return@withContext false
        } finally {
            kotlinx.coroutines.delay(2000)
            _printingState.value = PrintingState.IDLE
        }
    }

    /**
     * Отправка изображения на принтер используя TSPL команды
     */
    private fun printBitmap(bitmap: Bitmap, format: LabelFormat) {
        val printer = tsplPrinter ?: throw PrinterException("Принтер не инициализирован")

        try {
            Log.d(TAG, "Starting print job")

            // Настройки принтера
            printer.cls()
            printer.sizeMm(format.widthMm, format.heightMm)
            printer.gapMm(2.0, 0.0)
            printer.speed(2.0)
            printer.density(8)
            printer.direction(TSPLConst.DIRECTION_FORWARD)
            printer.reference(0, 0)
            printer.cls()

            // Отправка изображения
            printer.bitmap(0, 0, TSPLConst.BMP_MODE_OVERWRITE, bitmap.width, bitmap)

            // Печать
            printer.print(1)
            printer.sound(1, 50)

            Log.d(TAG, "Print job sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Print bitmap error", e)
            throw PrinterException("Ошибка печати: ${e.message}")
        }
    }

    /**
     * Тестовая печать
     */
    suspend fun printTest(): Boolean = withContext(Dispatchers.IO) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return@withContext false
        }

        try {
            _printingState.value = PrintingState.PRINTING

            val testData = LabelData(
                partNumber = "TEST-001",
                description = "Тестовая этикетка",
                orderNumber = "TEST-ORDER",
                location = "A1B2",
                quantity = 999,
                qrData = "TEST=ORDER=001=Тест печати",
                acceptanceDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    .format(Date()),
                labelType = "Тест"
            )

            val format = AcceptanceLabelFormat57x40()
            val bitmap = format.createBitmap(testData)
            printBitmap(bitmap, format)

            _printingState.value = PrintingState.SUCCESS
            return@withContext true

        } catch (e: Exception) {
            _printingState.value = PrintingState.ERROR
            Log.e(TAG, "Test print error", e)
            return@withContext false
        } finally {
            kotlinx.coroutines.delay(2000)
            _printingState.value = PrintingState.IDLE
        }
    }
}

/**
 * Модель данных для этикетки
 */
data class LabelData(
    val partNumber: String,
    val description: String,
    val orderNumber: String,
    val location: String,
    val quantity: Int? = null,
    val qrData: String,
    val labelType: String = "Общая",
    val acceptanceDate: String? = null
)

/**
 * Состояния подключения принтера
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * Состояния процесса печати
 */
enum class PrintingState {
    IDLE,
    PRINTING,
    SUCCESS,
    ERROR
}

/**
 * Исключение для ошибок принтера
 */
class PrinterException(message: String) : Exception(message)