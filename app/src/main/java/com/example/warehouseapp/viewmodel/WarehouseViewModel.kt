package com.example.warehouseapp.viewmodel

import android.bluetooth.BluetoothDevice
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.warehouseapp.data.*
import com.example.warehouseapp.network.NetworkManager
import com.example.warehouseapp.database.WarehouseDatabase
import com.example.warehouseapp.printer.ConnectionState
import com.example.warehouseapp.printer.PrintingState
import com.example.warehouseapp.printer.PrinterManager
import com.example.warehouseapp.scanner.NewlandBleManager
import java.util.*

class WarehouseViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask

    private val _scanResult = MutableStateFlow<String>("")
    val scanResult: StateFlow<String> = _scanResult

    // Состояния принтера
    private val _printerConnectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val printerConnectionState: StateFlow<ConnectionState> = _printerConnectionState

    private val _printingState = MutableStateFlow(PrintingState.IDLE)
    val printingState: StateFlow<PrintingState> = _printingState

    // Состояния сканера
    private val _scannerConnectionState = MutableStateFlow(NewlandBleManager.ConnectionState.DISCONNECTED)
    val scannerConnectionState: StateFlow<NewlandBleManager.ConnectionState> = _scannerConnectionState

    private val _scannerPairingQrCode = MutableStateFlow<Bitmap?>(null)
    val scannerPairingQrCode: StateFlow<Bitmap?> = _scannerPairingQrCode

    // Mock данные для заданий (временно)
    private val _taskItems = MutableStateFlow<Map<String, List<TaskItem>>>(emptyMap())

    private lateinit var database: WarehouseDatabase
    private lateinit var networkManager: NetworkManager
    private lateinit var printerManager: PrinterManager
    private lateinit var scannerManager: NewlandBleManager

    fun initialize(
        database: WarehouseDatabase,
        networkManager: NetworkManager,
        printerManager: PrinterManager,
        scannerManager: NewlandBleManager
    ) {
        this.database = database
        this.networkManager = networkManager
        this.printerManager = printerManager
        this.scannerManager = scannerManager

        // Подписываемся на состояния принтера
        viewModelScope.launch {
            printerManager.connectionState.collect { state ->
                _printerConnectionState.value = state
            }
        }

        viewModelScope.launch {
            printerManager.printingState.collect { state ->
                _printingState.value = state
            }
        }

        // Подписываемся на состояния сканера
        viewModelScope.launch {
            scannerManager.connectionState.collect { state ->
                _scannerConnectionState.value = state
            }
        }

        viewModelScope.launch {
            scannerManager.pairingQrCode.collect { qrCode ->
                _scannerPairingQrCode.value = qrCode
            }
        }

        viewModelScope.launch {
            scannerManager.scanResult.collect { result ->
                if (result.isNotEmpty()) {
                    _scanResult.value = result
                }
            }
        }

        // Устанавливаем callback для сканера
        scannerManager.setScanCallback { result ->
            _scanResult.value = result
        }

        loadData()
        loadMockTasks()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Загружаем продукты из базы данных
            database.productDao().getAllProducts().collect { productList ->
                _products.value = productList
            }
        }
    }

    private fun loadMockTasks() {
        // Временные данные для тестирования
        val mockTasks = listOf(
            Task(
                id = "task1",
                name = "Заказ #001 - Сборка корпусов",
                createdDate = Date(),
                isCompleted = false,
                isPaused = false,
                isSynced = false
            ),
            Task(
                id = "task2",
                name = "Заказ #002 - Комплектация деталей",
                createdDate = Date(),
                isCompleted = false,
                isPaused = true,
                isSynced = false
            )
        )
        _tasks.value = mockTasks

        // Mock элементы заданий
        val mockItems = mapOf(
            "task1" to listOf(
                TaskItem(
                    id = "item1",
                    taskId = "task1",
                    productId = "ДЕТ-456",
                    productName = "Корпус алюминиевый",
                    storageLocation = "A1B2",
                    requiredQuantity = 5,
                    scannedQuantity = 0,
                    isCompleted = false
                ),
                TaskItem(
                    id = "item2",
                    taskId = "task1",
                    productId = "ДЕТ-789",
                    productName = "Крышка пластиковая",
                    storageLocation = "B2C3",
                    requiredQuantity = 5,
                    scannedQuantity = 2,
                    isCompleted = false
                )
            ),
            "task2" to listOf(
                TaskItem(
                    id = "item3",
                    taskId = "task2",
                    productId = "СБ-123",
                    productName = "Блок питания",
                    storageLocation = "C3D4",
                    requiredQuantity = 2,
                    scannedQuantity = 0,
                    isCompleted = false
                )
            )
        )
        _taskItems.value = mockItems
    }

    fun addProduct(product: Product) {
        viewModelScope.launch {
            database.productDao().insertProduct(product)
            // Отправляем на сервер в фоне
            try {
                networkManager.sendProductToServer(product)
            } catch (e: Exception) {
                // Обработка ошибок сети
            }
        }
    }

    fun updateScanResult(result: String) {
        _scanResult.value = result
    }

    fun parseQRCode(qrCode: String): ParsedQRData {
        return when {
            qrCode.startsWith("PART:") -> ParsedQRData(
                partNumber = qrCode.removePrefix("PART:"),
                type = QRCodeType.PART
            )
            qrCode.startsWith("ASSEMBLY:") -> ParsedQRData(
                partNumber = qrCode.removePrefix("ASSEMBLY:"),
                type = QRCodeType.ASSEMBLY
            )
            qrCode.contains("=") -> {
                // Формат: номер_маршрутки=номер_заказа=номер_детали=название_детали
                val parts = qrCode.split("=")
                if (parts.size == 4) {
                    ParsedQRData(
                        routeCard = parts[0],
                        orderNumber = parts[1],
                        partNumber = parts[2],
                        partName = parts[3],
                        type = QRCodeType.FIXED_FORMAT
                    )
                } else {
                    ParsedQRData(type = QRCodeType.UNKNOWN)
                }
            }
            else -> ParsedQRData(type = QRCodeType.UNKNOWN)
        }
    }

    fun selectTask(task: Task) {
        _currentTask.value = task
    }

    fun completeTaskItem(itemId: String, quantity: Int) {
        viewModelScope.launch {
            _currentTask.value?.let { task ->
                val items = _taskItems.value[task.id] ?: return@let

                val updatedItems = items.map { item ->
                    if (item.id == itemId) {
                        val newScannedQuantity = (item.scannedQuantity + quantity).coerceAtMost(item.requiredQuantity)
                        item.copy(
                            scannedQuantity = newScannedQuantity,
                            isCompleted = newScannedQuantity >= item.requiredQuantity
                        )
                    } else {
                        item
                    }
                }

                _taskItems.value = _taskItems.value + (task.id to updatedItems)

                // Проверяем, все ли items выполнены
                val isTaskCompleted = updatedItems.all { it.isCompleted }

                if (isTaskCompleted) {
                    val updatedTask = task.copy(isCompleted = true)
                    _currentTask.value = updatedTask
                    _tasks.value = _tasks.value.map {
                        if (it.id == task.id) updatedTask else it
                    }
                    // Отправляем на сервер
                    try {
                        networkManager.updateTask(updatedTask)
                    } catch (e: Exception) {
                        // Обработка ошибок
                    }
                }

                // Сохраняем информацию о выдаче
                val taskItem = items.find { it.id == itemId }
                taskItem?.let {
                    val shipment = Shipment(
                        id = UUID.randomUUID().toString(),
                        taskId = task.id,
                        productId = it.productId,
                        productName = it.productName,
                        quantity = quantity,
                        storageLocation = it.storageLocation
                    )
                    // TODO: Сохранить в базу данных
                }
            }
        }
    }

    fun getTaskItems(taskId: String): List<TaskItem> {
        return _taskItems.value[taskId] ?: emptyList()
    }

    fun pauseTask(taskId: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(isPaused = true)
            } else {
                task
            }
        }
    }

    fun resumeTask(taskId: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(isPaused = false)
            } else {
                task
            }
        }
    }

    // Методы принтера
    suspend fun printReceptionLabel(product: Product): Boolean {
        return printerManager.printLabel(product)
    }

    suspend fun printShipmentLabel(item: TaskItem, quantity: Int): Boolean {
        return printerManager.printShipmentLabel(item, quantity)
    }

    suspend fun connectPrinter(macAddress: String): Boolean {
        return printerManager.connectToPrinter(macAddress)
    }

    suspend fun testPrint(): Boolean {
        return printerManager.printTest()
    }

    // Методы сканера
    fun startScannerPairing() {
        scannerManager.generatePairingQrCode()
    }

    fun stopScannerPairing() {
        scannerManager.stopPairing()
    }

    suspend fun connectToScanner(device: BluetoothDevice) {
        scannerManager.connectToScanner(device.address)
    }

    fun disconnectScanner() {
        scannerManager.disconnect()
    }

    fun scannerBeep() {
        scannerManager.beep(frequency = 2700, duration = 100, volume = 10)
    }

    fun scannerBeepError() {
        // Двойной сигнал для ошибки
        viewModelScope.launch {
            scannerManager.beep(frequency = 1000, duration = 100, volume = 15)
            kotlinx.coroutines.delay(150)
            scannerManager.beep(frequency = 1000, duration = 100, volume = 15)
        }
    }

    fun scannerVibrate() {
        scannerManager.vibrate(100)
    }

    override fun onCleared() {
        super.onCleared()
        scannerManager.release()
    }
}