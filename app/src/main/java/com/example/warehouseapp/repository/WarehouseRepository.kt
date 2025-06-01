package com.example.warehouseapp.repository

import com.example.warehouseapp.data.*
import com.example.warehouseapp.database.ProductDao
import com.example.warehouseapp.network.WarehouseApiService
import com.example.warehouseapp.network.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class WarehouseRepository @Inject constructor(
    private val productDao: ProductDao,
    private val apiService: WarehouseApiService
) {

    // Локальные операции
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getProductById(id: String): Product? = productDao.getProductById(id)

    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    // Сетевые операции
    suspend fun syncProducts(): Flow<SyncState> = flow {
        emit(SyncState.Loading)

        try {
            // Получаем несинхронизированные продукты
            val unsyncedProducts = productDao.getUnsyncedProducts()

            if (unsyncedProducts.isEmpty()) {
                emit(SyncState.Success("Нет данных для синхронизации"))
                return@flow
            }

            // Отправляем на сервер
            val response = apiService.syncProducts(unsyncedProducts)

            if (response.isSuccessful && response.body()?.success == true) {
                // Помечаем как синхронизированные
                unsyncedProducts.forEach { product ->
                    productDao.updateProduct(product.copy(isSynced = true))
                }

                emit(SyncState.Success("Синхронизировано: ${response.body()?.syncedCount} записей"))
            } else {
                emit(SyncState.Error("Ошибка синхронизации: ${response.message()}"))
            }

        } catch (e: Exception) {
            emit(SyncState.Error("Ошибка сети: ${e.message}"))
        }
    }

    suspend fun fetchTasks(): Flow<FetchState<List<Task>>> = flow {
        emit(FetchState.Loading)

        try {
            val response = apiService.getTasks()

            if (response.isSuccessful) {
                val tasks = response.body() ?: emptyList()
                emit(FetchState.Success(tasks))
            } else {
                emit(FetchState.Error("Ошибка загрузки заданий: ${response.message()}"))
            }

        } catch (e: Exception) {
            emit(FetchState.Error("Ошибка сети: ${e.message}"))
        }
    }

    suspend fun updateTask(task: Task): Boolean {
        return try {
            val response = apiService.updateTask(task.id, task)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addShipment(shipment: Shipment): Boolean {
        return try {
            val response = apiService.addShipment(shipment)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

// Состояния для UI
sealed class SyncState {
    object Loading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

sealed class FetchState<out T> {
    object Loading : FetchState<Nothing>()
    data class Success<T>(val data: T) : FetchState<T>()
    data class Error(val message: String) : FetchState<Nothing>()
}