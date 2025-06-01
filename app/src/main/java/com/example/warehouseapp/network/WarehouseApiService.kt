package com.example.warehouseapp.network

import com.example.warehouseapp.data.Product
import com.example.warehouseapp.data.Task
import com.example.warehouseapp.data.Shipment
import retrofit2.Response
import retrofit2.http.*

interface WarehouseApiService {

    @POST("products")
    suspend fun addProduct(@Body product: Product): Response<Product>

    @GET("products")
    suspend fun getProducts(): Response<List<Product>>

    @PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") productId: String,
        @Body product: Product
    ): Response<Product>

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") productId: String): Response<Unit>

    @GET("tasks")
    suspend fun getTasks(): Response<List<Task>>

    @PUT("tasks/{id}")
    suspend fun updateTask(
        @Path("id") taskId: String,
        @Body task: Task
    ): Response<Task>

    @POST("shipments")
    suspend fun addShipment(@Body shipment: Shipment): Response<Shipment>

    @POST("sync/products")
    suspend fun syncProducts(@Body products: List<Product>): Response<SyncResult>

    @POST("sync/shipments")
    suspend fun syncShipments(@Body shipments: List<Shipment>): Response<SyncResult>
}

data class SyncResult(
    val success: Boolean,
    val syncedCount: Int,
    val errors: List<String>?
)