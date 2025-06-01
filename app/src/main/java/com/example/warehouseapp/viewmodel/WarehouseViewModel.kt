package com.example.warehouseapp.viewmodel

import android.bluetooth.BluetoothDevice
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouseapp.data.*
import com.example.warehouseapp.printer.ConnectionState
import com.example.warehouseapp.printer.PrintingState
import com.example.warehouseapp.printer.PrinterManager
import com.example.warehouseapp.repository.WarehouseRepository
import com.example.warehouseapp.repository.SyncState
import com.example.warehouseapp.repository.FetchState
import com.example.warehouseapp.scanner.NewlandBleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WarehouseViewModel @Inject constructor(
    private val repository: WarehouseRepository,
    private val printerManager: PrinterManager,
    private val scannerManager: NewlandBleManager
) : ViewModel() {

    // UI состояния
    private val _uiState = MutableStateFlow(WarehouseUiState())
    val uiState: StateFlow<WarehouseUiState> = _uiState.asStateFlow()

    // Продукты из базы данных
    val products = repository.getAllProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Задания
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    // Текущее задание
    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask.asStateFlow()

    // Результат сканирования
    private val _scanResult = MutableStateFlow("")
    val scanResult: StateFlow<String> = _scanResult.asStateFlow()

    // Состояния принтера
    val printerConnectionState = printerManager.connectionState
    val printingState = printerManager.printingState

    // Состояния сканера
    val scannerConnectionState = scannerManager.connectionState
    val scannerPairingQrCode = scannerManager.pairingQrCode

    // Состояние синхронизации
    private val _syncState = MutableStateFlow<SyncState?>(null)
    val syncState: StateFlow<SyncState?> = _syncState.asStateFlow()

    // Mock данные для элементов заданий
    private val _taskItems = MutableStateFlow<Map<String, List<TaskItem>>>(emptyMap())

    init {
        // Подписываемся на результаты сканирования
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

        // Загружаем задания при инициализации
        loadTasks()
        loadMockTaskItems()
    }

    // Загрузка заданий с сервера
    fun loadTasks() {
        viewModelScope.launch {
            repository.fetchTasks().collect { state ->
                when (state) {
                    is FetchState.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is FetchState.Success -> {
                        _tasks.value = state.data
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is FetchState.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = state.message
                            )
                        }
                    }
                }
            }
        }
    }

    // Синхронизация данных с сервером
    fun syncData() {
        viewModelScope.launch {
            repository.syncProducts().collect { state ->
                _syncState.value = state
            }
        }
    }

    // Добавление продукта
    fun addProduct(product: Product) {
        viewModelScope.launch {
            repository.insertProduct(product)
            _uiState.update {
                it.copy(lastAddedProduct = product)
            }
        }
    }

    // Обновление результата сканирования
    fun updateScanResult(result: String) {
        _scanResult.value = result
    }

    // Парсинг QR кода
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

    // Выбор задания
    fun selectTask(task: Task) {
        _currentTask.value = task
    }

    // Завершение элемента задания
    fun completeTaskItem(itemId: String, quantity: Int) {
        viewModelScope.launch {
            _currentTask.value?.let { task ->
                val items = _taskItems.value[task.id] ?: return@let

                val updatedItems = items.map { item ->
                    if (item.id == itemId) {
                        val newScannedQuantity = (item.scannedQuantity + quantity)
                            .coerceAtMost(item.requiredQuantity)
                        item.copy(
                            scannedQuantity = newScannedQuantity,
                            isCompleted = newScannedQuantity >= item.requiredQuantity
                        )
                    } else {
                        item
                    }
                }

                _taskItems.value = _taskItems.value + (task.id to updatedItems)

                // Проверяем, все ли элементы выполнены
                val isTaskCompleted = updatedItems.all { it.isCompleted }

                if (isTaskCompleted) {
                    val updatedTask = task.copy(isCompleted = true)
                    _currentTask.value = updatedTask
                    _tasks.value = _tasks.value.map {
                        if (it.id == task.id) updatedTask else it
                    }

                    // Обновляем на сервере
                    repository.updateTask(updatedTask)
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
                    repository.addShipment(shipment)
                }
            }
        }
    }

    // Получение элементов задания
    fun getTaskItems(taskId: String): List<TaskItem> {
        return _taskItems.value[taskId] ?: emptyList()
    }

    // Приостановка задания
    fun pauseTask(taskId: String) {
        viewModelScope.launch {
            val task = _tasks.value.find { it.id == taskId } ?: return@launch
            val updatedTask = task.copy(isPaused = true)

            _tasks.value = _tasks.value.map {
                if (it.id == taskId) updatedTask else it
            }

            repository.updateTask(updatedTask)
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
        viewModelScope.launch {
            scannerManager.beep(frequency = 1000, duration = 100, volume = 15)
            kotlinx.coroutines.delay(150)
            scannerManager.beep(frequency = 1000, duration = 100, volume = 15)
        }
    }

    fun scannerVibrate() {
        scannerManager.vibrate(100)
    }

    // Очистка сообщения об ошибке
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Mock данные для элементов заданий
    private fun loadMockTaskItems() {
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
            )
        )
        _taskItems.value = mockItems
    }

    override fun onCleared() {
        super.onCleared()
        scannerManager.release()
    }
}

// UI состояние
data class WarehouseUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastAddedProduct: Product? = null
)