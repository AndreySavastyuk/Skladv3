package com.example.warehouseapp.network

import android.content.Context
import android.util.Log
import com.example.warehouseapp.data.Product
import com.example.warehouseapp.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class NetworkManager(private val context: Context) {
    private val client = OkHttpClient()

    companion object {
        private const val BASE_URL = "http://192.168.1.100:8080/api" // Replace with your server URL
        private const val TAG = "NetworkManager"
    }

    suspend fun sendProductToServer(product: Product): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", product.id)
                put("name", product.name)
                put("qrCode", product.qrCode)
                put("quantity", product.quantity)
                put("storageLocation", product.storageLocation)
                put("type", product.type.name)
                put("receivedDate", product.receivedDate.time)
            }

            val requestBody = json.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/products")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отправки продукта на сервер", e)
            return@withContext false
        }
    }

    suspend fun fetchTasks(): List<Task> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/tasks")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(response.body?.string() ?: "[]")
                    return@withContext parseTasksFromJson(jsonArray)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения заданий с сервера", e)
        }
        return@withContext emptyList()
    }

    suspend fun updateTask(task: Task): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = taskToJson(task)

            val requestBody = json.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/tasks/${task.id}")
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления задания на сервере", e)
            return@withContext false
        }
    }

    private fun parseTasksFromJson(jsonArray: JSONArray): List<Task> {
        // Implement JSON parsing logic
        return emptyList()
    }

    private fun taskToJson(task: Task): JSONObject {
        // Implement task to JSON conversion
        return JSONObject()
    }
}