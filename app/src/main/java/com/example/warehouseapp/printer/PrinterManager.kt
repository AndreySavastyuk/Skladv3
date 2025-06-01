package com.example.warehouseapp.printer

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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.posprinter.POSConnect
import net.posprinter.IDeviceConnection
import net.posprinter.IConnectListener
import net.posprinter.TSPLConst
import net.posprinter.TSPLPrinter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine
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
        private const val PRINTER_NAME = "Xprinter"

        // Размеры этикетки
        private const val LABEL_WIDTH_MM = 57.0
        private const val LABEL_HEIGHT_MM = 40.0
        private const val DPI = 203
    }

    private var deviceConnection: IDeviceConnection? = null
    private var tsplPrinter: TSPLPrinter? = null

    // Состояние подключения
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // Состояние печати
    private val _printingState = MutableStateFlow(PrintingState.IDLE)
    val printingState: StateFlow<PrintingState> = _printingState

    init {
        // Инициализация SDK
        try {
            POSConnect.init(context.applicationContext)
            Log.d(TAG, "POSConnect SDK initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize POSConnect SDK", e)
        }
    }

    /**
     * Подключение к принтеру по MAC-адресу
     */
    suspend fun connectToPrinter(macAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.CONNECTING
            Log.d(TAG, "Attempting to connect to printer: $macAddress")

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
     * Асинхронное подключение
     */
    private suspend fun connectAsync(macAddress: String): Boolean = suspendCoroutine { continuation ->
        // Закрываем предыдущее соединение
        deviceConnection?.close()

        // Создаем новое соединение
        deviceConnection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)

        deviceConnection?.connect(macAddress, object : IConnectListener {
            override fun onStatus(code: Int, connectInfo: String?, message: String?) {
                Log.d(TAG, "Connection status: code=$code, info=$connectInfo, msg=$message")

                when (code) {
                    POSConnect.CONNECT_SUCCESS -> {
                        tsplPrinter = TSPLPrinter(deviceConnection)
                        continuation.resume(true)
                    }
                    POSConnect.CONNECT_FAIL,
                    POSConnect.CONNECT_INTERRUPT -> {
                        continuation.resume(false)
                    }
                    else -> {
                        continuation.resume(false)
                    }
                }
            }
        }) ?: continuation.resume(false)
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
            Log.d(TAG, "Printer disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }

    /**
     * Печать этикетки приемки
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
            val labelData = if (qrParts.size == 4) {
                LabelData(
                    partNumber = qrParts[2],
                    description = qrParts[3],
                    orderNumber = qrParts[1],
                    location = product.storageLocation,
                    quantity = product.quantity,
                    qrData = product.qrCode,
                    acceptanceDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        .format(product.receivedDate)
                )
            } else {
                LabelData(
                    partNumber = product.name,
                    description = product.name,
                    orderNumber = product.orderNumber ?: "",
                    location = product.storageLocation,
                    quantity = product.quantity,
                    qrData = product.qrCode,
                    acceptanceDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        .format(product.receivedDate)
                )
            }

            // Создаем изображение этикетки
            val labelBitmap = createAcceptanceLabelBitmap(labelData)

            // Печатаем
            printBitmap(labelBitmap)

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

            // Создаем изображение этикетки
            val labelBitmap = createPickingLabelBitmap(labelData)

            // Печатаем
            printBitmap(labelBitmap)

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
     * Создание изображения этикетки приемки (формат 57x40 мм)
     */
    private fun createAcceptanceLabelBitmap(data: LabelData): Bitmap {
        val widthPx = (LABEL_WIDTH_MM / 25.4 * DPI).toInt()
        val heightPx = (LABEL_HEIGHT_MM / 25.4 * DPI).toInt()

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val margin = 8f

        // Рамка этикетки
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, widthPx - 1f, heightPx - 1f, borderPaint)

        // QR-код
        val qrSize = 200
        val qrBitmap = generateQRCode(data.qrData, qrSize)
        canvas.drawBitmap(qrBitmap, 16f, 95f, null)

        // Номер детали (крупно по центру)
        val partNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create("Arial", Typeface.BOLD)
            textSize = 42f
            textAlign = Paint.Align.CENTER
        }

        val centerX = widthPx / 2f
        canvas.drawText(data.partNumber, centerX, 47f, partNumberPaint)

        // Рамка ячейки
        val cellBoxX = 233f
        val cellBoxY = 177f
        val cellBoxWidth = 195f
        val cellBoxHeight = 119f
        canvas.drawRect(cellBoxX, cellBoxY, cellBoxX + cellBoxWidth, cellBoxY + cellBoxHeight, borderPaint)

        // Текст ячейки
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 79f
            typeface = Typeface.create("Arial", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellTextY = cellBoxY + (cellBoxHeight + cellPaint.textSize) / 2 - 6f
        canvas.drawText(data.location, cellBoxX + cellBoxWidth / 2, cellTextY, cellPaint)

        // Наименование детали
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        canvas.drawText(data.description, 243f, 82f, namePaint)

        // Номер заказа
        val orderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        canvas.drawText(data.orderNumber, 223f, 154f, orderPaint)

        // Количество
        val quantityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        data.quantity?.let { qty ->
            canvas.drawText("Кол-во: $qty шт", 21f, 87f, quantityPaint)
        }

        // Дата
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        data.acceptanceDate?.let { date ->
            canvas.drawText("Дата: $date", 110f, 86f, datePaint)
        }

        return bitmap
    }

    /**
     * Создание изображения этикетки комплектации
     */
    private fun createPickingLabelBitmap(data: LabelData): Bitmap {
        val widthPx = (LABEL_WIDTH_MM / 25.4 * DPI).toInt()
        val heightPx = (LABEL_HEIGHT_MM / 25.4 * DPI).toInt()

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val margin = 8f

        // Рамка
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, widthPx - 1f, heightPx - 1f, borderPaint)

        // Заголовок "КОМПЛЕКТАЦИЯ"
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create("Arial", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        var yPos = margin + 16f
        canvas.drawText("КОМПЛЕКТАЦИЯ", widthPx / 2f, yPos, headerPaint)

        // Линия после заголовка
        yPos += 6f
        canvas.drawLine(margin, yPos, widthPx - margin, yPos, borderPaint)
        yPos += 10f

        // Информация о товаре
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }

        canvas.drawText("Товар: ${data.partNumber}", margin, yPos, textPaint)
        yPos += 20f

        canvas.drawText("Количество: ${data.quantity} шт", margin, yPos, textPaint)
        yPos += 20f

        canvas.drawText("Ячейка: ${data.location}", margin, yPos, textPaint)

        // QR-код справа внизу
        val qrSize = 100
        val qrX = widthPx - qrSize - margin.toInt()
        val qrY = heightPx - qrSize - margin.toInt()
        val qrBitmap = generateQRCode(data.qrData, qrSize)
        canvas.drawBitmap(qrBitmap, qrX.toFloat(), qrY.toFloat(), null)

        return bitmap
    }

    /**
     * Генерация QR-кода с поддержкой UTF-8
     */
    private fun generateQRCode(data: String, size: Int): Bitmap {
        return try {
            val writer = QRCodeWriter()
            val hints = HashMap<EncodeHintType, Any>().apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }

            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate QR code", e)
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
        }
    }

    /**
     * Отправка изображения на принтер
     */
    private fun printBitmap(bitmap: Bitmap) {
        val printer = tsplPrinter ?: throw PrinterException("Принтер не инициализирован")

        try {
            Log.d(TAG, "Starting print job")

            // Настройки принтера
            printer.cls()
            printer.sizeMm(LABEL_WIDTH_MM, LABEL_HEIGHT_MM)
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
                    .format(Date())
            )

            val bitmap = createAcceptanceLabelBitmap(testData)
            printBitmap(bitmap)

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