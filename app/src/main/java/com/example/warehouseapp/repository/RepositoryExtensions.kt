package com.example.warehouseapp.repository

import com.example.warehouseapp.data.Product
import com.example.warehouseapp.data.Shipment
import com.example.warehouseapp.data.Task
import com.example.warehouseapp.network.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Состояния синхронизации
sealed class RepositorySyncState {
    object Loading : RepositorySyncState()
    data class Success(val syncedCount: Int) : RepositorySyncState()
    data class Error(val message: String) : RepositorySyncState()
}

sealed class RepositoryFetchState<out T> {
    object Loading : RepositoryFetchState<Nothing>()
    data class Success<T>(val data: T) : RepositoryFetchState<T>()
    data class Error(val message: String) : RepositoryFetchState<Nothing>()
}

// Расширения для WarehouseRepository
suspend fun WarehouseRepository.getUnsyncedProducts(): List<Product> {
    // Получаем продукты, которые не синхронизированы
    return getAllProductsList().filter { !it.isSynced }
}

suspend fun WarehouseRepository.getAllProductsList(): List<Product> {
    // Преобразуем Flow в List
    var productsList = emptyList<Product>()
    getAllProducts().collect { products ->
        productsList = products
    }
    return productsList
}

suspend fun WarehouseRepository.syncProductsList(products: List<Product>): Result<SyncResult> {
    return try {
        // Здесь должен быть вызов API для синхронизации
        // Пока возвращаем успех
        Result.success(SyncResult(
            success = true,
            syncedCount = products.size,
            errors = null
        ))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun WarehouseRepository.updateProduct(product: Product) {
    // Обновляем продукт в базе данных
    insertProduct(product)
}

// Методы для работы с заданиями
fun WarehouseRepository.fetchTasks(): Flow<FetchState<List<Task>>> = flow {
    emit(FetchState.Loading)
    try {
        // Здесь должен быть вызов API
        // Пока возвращаем пустой список
        emit(FetchState.Success(emptyList()))
    } catch (e: Exception) {
        emit(FetchState.Error(e.message ?: "Unknown error"))
    }
}

suspend fun WarehouseRepository.updateTask(task: Task) {
    // Обновление задания на сервере
    // TODO: Реализовать вызов API
}

suspend fun WarehouseRepository.addShipment(shipment: Shipment) {
    // Добавление записи о выдаче
    // TODO: Реализовать вызов API и сохранение в БД
}

// Метод синхронизации продуктов
fun WarehouseRepository.syncProducts(): Flow<SyncState> = flow {
    emit(SyncState.Loading)
    try {
        val unsyncedProducts = getUnsyncedProducts()
        if (unsyncedProducts.isEmpty()) {
            emit(SyncState.Success(0))
            return@flow
        }

        val result = syncProductsList(unsyncedProducts)
        if (result.isSuccess) {
            val syncResult = result.getOrNull()
            emit(SyncState.Success(syncResult?.syncedCount ?: 0))
        } else {
            emit(SyncState.Error(result.exceptionOrNull()?.message ?: "Sync failed"))
        }
    } catch (e: Exception) {
        emit(SyncState.Error(e.message ?: "Unknown error"))
    }
}