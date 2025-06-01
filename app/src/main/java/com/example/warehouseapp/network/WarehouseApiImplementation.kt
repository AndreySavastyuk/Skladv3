package com.example.warehouseapp.network

import com.example.warehouseapp.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация API для работы с сервером склада
 */
@Singleton
class WarehouseApiImplementation @Inject constructor() {

    companion object {
        private const val DEFAULT_BASE_URL = "http://192.168.1.100:8080/api/"
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 30L
        private const val WRITE_TIMEOUT = 30L
    }

    private var retrofit: Retrofit? = null
    private var apiService: WarehouseApiService? = null

    /**
     * Инициализация/обновление базового URL
     */
    fun updateBaseUrl(baseUrl: String) {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit?.create(WarehouseApiService::class.java)
    }

    init {
        updateBaseUrl(DEFAULT_BASE_URL)
    }

    /**
     * Добавление продукта
     */
    suspend fun addProduct(product: Product): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.addProduct(product)
                ?: return@withContext Result.failure(Exception("API Service not initialized"))

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Server error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Получение списка заданий
     */
    suspend fun getTasks(): Result<List<Task>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.getTasks()
                ?: return@withContext Result.failure(Exception("API Service not initialized"))

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Обновление задания
     */
    suspend fun updateTask(taskId: String, task: Task): Result<Task> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.updateTask(taskId, task)
                ?: return@withContext Result.failure(Exception("API Service not initialized"))

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Добавление записи о выдаче
     */
    suspend fun addShipment(shipment: Shipment): Result<Shipment> = withContext(Dispatchers.IO) {
        try {
            val response = apiService?.addShipment(shipment)
                ?: return@withContext Result.failure(Exception("API Service not initialized"))

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Синхронизация несинхронизированных продуктов
     */
    suspend fun syncProducts(products: List<Product>): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            if (products.isEmpty()) {
                return@withContext Result.success(SyncResult(true, 0, null))
            }

            val response = apiService?.syncProducts(products)
                ?: return@withContext Result.failure(Exception("API Service not initialized"))

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Проверка соединения с сервером
     */
    suspend fun checkConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Попытка получить список продуктов для проверки соединения
            val response = apiService?.getProducts()
                ?: return@withContext Result.failure(Exception("API Service not initialized"))

            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Расширение для NetworkManager для использования новой реализации
 */
suspend fun NetworkManager.sendProductToServerV2(product: Product): Result<Product> {
    // Здесь можно вызвать новую реализацию API
    // Это временное решение для обратной совместимости
    return try {
        val success = sendProductToServer(product)
        if (success) {
            Result.success(product)
        } else {
            Result.failure(Exception("Failed to send product"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}