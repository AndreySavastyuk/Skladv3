package com.example.warehouseapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.warehouseapp.data.*
import com.example.warehouseapp.network.NetworkManager
import com.example.warehouseapp.database.WarehouseDatabase

class WarehouseViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask

    private val _scanResult = MutableStateFlow<String>("")
    val scanResult: StateFlow<String> = _scanResult

    private lateinit var database: WarehouseDatabase
    private lateinit var networkManager: NetworkManager

    fun initialize(database: WarehouseDatabase, networkManager: NetworkManager) {
        this.database = database
        this.networkManager = networkManager
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

    fun processQRCode(qrCode: String): QRCodeType {
        return when {
            qrCode.startsWith("PART:") -> QRCodeType.PART
            qrCode.startsWith("ASSEMBLY:") -> QRCodeType.ASSEMBLY
            else -> QRCodeType.UNKNOWN
        }
    }

    fun selectTask(task: Task) {
        _currentTask.value = task
    }

    fun completeTaskItem(itemId: String, quantity: Int) {
        viewModelScope.launch {
            _currentTask.value?.let { task ->
                val updatedItems = task.items.map { item ->
                    if (item.id == itemId) {
                        item.copy(
                            scannedQuantity = item.scannedQuantity + quantity,
                            isCompleted = (item.scannedQuantity + quantity) >= item.requiredQuantity
                        )
                    } else {
                        item
                    }
                }

                val updatedTask = task.copy(
                    items = updatedItems,
                    isCompleted = updatedItems.all { it.isCompleted }
                )

                _currentTask.value = updatedTask

                // Update task on server
                networkManager.updateTask(updatedTask)
            }
        }
    }
}