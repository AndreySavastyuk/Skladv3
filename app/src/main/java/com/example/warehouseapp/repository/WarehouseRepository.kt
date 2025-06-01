package com.example.warehouseapp.repository

import com.example.warehouseapp.data.Product
import com.example.warehouseapp.data.Shipment
import com.example.warehouseapp.data.Task
import com.example.warehouseapp.database.ProductDao
import com.example.warehouseapp.network.NetworkManager
import com.example.warehouseapp.network.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для работы с данными склада
 */
@Singleton
class WarehouseRepository @Inject constructor(
    private val productDao: ProductDao,
    private val networkManager: NetworkManager
) {

    // Получение всех продуктов
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()

    // Вставка продукта
    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
        // Отправляем на сервер в фоне
        try {
            networkManager.sendProductToServer(product)
        } catch (e: Exception) {
            // Игнорируем ошибки сети, продукт сохранен локально
        }
    }

    // Получение несинхронизированных продуктов
    suspend fun getUnsyncedProducts(): List<Product> {
        return productDao.getUnsyncedProducts()
    }

    // Обновление продукта
    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    // Синхронизация списка продуктов
    suspend fun syncProductsList(products: List<Product>): Result<SyncResult> {
        return try {
            // Здесь должен быть вызов API через NetworkManager
            // Пока возвращаем заглушку
            Result.success(SyncResult(
                success = true,
                syncedCount = products.size,
                errors = null
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Получение заданий
    fun fetchTasks(): Flow<FetchState<List<Task>>> = flow {
        emit(FetchState.Loading)
        try {
            val tasks = networkManager.fetchTasks()
            emit(FetchState.Success(tasks))
        } catch (e: Exception) {
            emit(FetchState.Error(e.message ?: "Unknown error"))
        }
    }

    // Обновление задания
    suspend fun updateTask(task: Task) {
        try {
            networkManager.updateTask(task)
        } catch (e: Exception) {
            // Обработка ошибки
        }
    }

    // Добавление записи о выдаче
    suspend fun addShipment(shipment: Shipment) {
        // TODO: Сохранить в локальную БД
        // TODO: Отправить на сервер
    }

    // Синхронизация продуктов
    fun syncProducts(): Flow<SyncState> = flow {
        emit(SyncState.Loading)
        try {
            val unsyncedProducts = getUnsyncedProducts()
            if (unsyncedProducts.isEmpty()) {
                emit(SyncState.Success(0))
                return@flow
            }

            val result = syncProductsList(unsyncedProducts)
            if (result.isSuccess) {
                // Помечаем продукты как синхронизированные
                unsyncedProducts.forEach { product ->
                    updateProduct(product.copy(isSynced = true))
                }

                val syncResult = result.getOrNull()
                emit(SyncState.Success(syncResult?.syncedCount ?: unsyncedProducts.size))
            } else {
                emit(SyncState.Error(result.exceptionOrNull()?.message ?: "Sync failed"))
            }
        } catch (e: Exception) {
            emit(SyncState.Error(e.message ?: "Unknown error"))
        }
    }
}

// Состояния синхронизации
sealed class SyncState {
    object Loading : SyncState()
    data class Success(val syncedCount: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

// Состояния загрузки
sealed class FetchState<out T> {
    object Loading : FetchState<Nothing>()
    data class Success<T>(val data: T) : FetchState<T>()
    data class Error(val message: String) : FetchState<Nothing>()
}