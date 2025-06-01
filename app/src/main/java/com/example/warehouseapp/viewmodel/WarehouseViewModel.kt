package com.example.warehouseapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.warehouseapp.data.*
import com.example.warehouseapp.network.NetworkManager
import com.example.warehouseapp.database.WarehouseDatabase
import com.example.warehouseapp.printer.XprinterAdapter
import com.example.warehouseapp.scanner.NewlandScannerAdapter
import com.example.warehouseapp.printer.ConnectionState
import com.example.warehouseapp.printer.PrintingState
import com.example.warehouseapp.printer.PrinterManager

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

    private lateinit var database: WarehouseDatabase
    private lateinit var networkManager: NetworkManager
    private lateinit var printerManager: PrinterManager
    private lateinit var scannerAdapter: NewlandScannerAdapter

    fun initialize(
        database: WarehouseDatabase,
        networkManager: NetworkManager,
        printerManager: PrinterManager,
        scannerAdapter: NewlandScannerAdapter
    ) {
        this.database = database
        this.networkManager = networkManager
        this.printerManager = printerManager
        this.scannerAdapter = scannerAdapter

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

        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load products from database
            database.productDao().getAllProducts().collect { productList ->
                _products.value = productList
            }
        }

        viewModelScope.launch {
            // Load tasks from network
            val tasksFromServer = networkManager.fetchTasks()
            _tasks.value = tasksFromServer
        }
    }

    fun addProduct(product: Product) {
        viewModelScope.launch {
            database.productDao().insertProduct(product)
            networkManager.sendProductToServer(product)
        }
    }

    fun updateScanResult(result: String) {
        _scanResult.value = result
    }

    fun processQRCode(qrCode: String): ParsedQRData {
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
                // Получаем items для задачи (нужно добавить в базу данных)
                val items = getTaskItems(task.id)

                val updatedItems = items.map { item ->
                    if (item.id == itemId) {
                        item.copy(
                            scannedQuantity = item.scannedQuantity + quantity,
                            isCompleted = (item.scannedQuantity + quantity) >= item.requiredQuantity
                        )
                    } else {
                        item
                    }
                }

                // Проверяем, все ли items выполнены
                val isTaskCompleted = updatedItems.all { it.isCompleted }

                if (isTaskCompleted) {
                    val updatedTask = task.copy(isCompleted = true)
                    _currentTask.value = updatedTask
                    networkManager.updateTask(updatedTask)
                }

                // Сохраняем информацию о выдаче
                val taskItem = items.find { it.id == itemId }
                taskItem?.let {
                    val shipment = Shipment(
                        id = java.util.UUID.randomUUID().toString(),
                        taskId = task.id,
                        productId = it.productId,
                        productName = it.productName,
                        quantity = quantity,
                        storageLocation = it.storageLocation
                    )
                    saveShipment(shipment)
                }
            }
        }
    }

    private suspend fun getTaskItems(taskId: String): List<TaskItem> {
        // TODO: Загрузить из базы данных или сервера
        return emptyList()
    }

    private suspend fun saveShipment(shipment: Shipment) {
        // TODO: Сохранить в базу данных
        //_shipments.value = _shipments.value + shipment
        //networkManager.sendShipmentToServer(shipment)
    }

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

    // Методы для работы со сканером
    fun connectToScanner(device: android.bluetooth.BluetoothDevice) {
        viewModelScope.launch {
            scannerAdapter.connect(device)
        }
    }

    fun disconnectScanner() {
        scannerAdapter.disconnect()
    }

    fun setScannerCallback(callback: (String) -> Unit) {
        scannerAdapter.setScanCallback(callback)
    }
}