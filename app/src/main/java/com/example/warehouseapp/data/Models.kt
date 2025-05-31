package com.example.warehouseapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val id: String,
    val name: String,
    val qrCode: String,
    val quantity: Int,
    val storageLocation: String,
    val type: ProductType,
    val receivedDate: Date = Date(),
    val isSynced: Boolean = false,
    // Дополнительные поля для расширенной информации
    val routeCard: String? = null,
    val orderNumber: String? = null,
    val partNumber: String? = null
)

enum class ProductType {
    PART,
    ASSEMBLY
}

enum class QRCodeType {
    PART,
    ASSEMBLY,
    FIXED_FORMAT, // Формат: номер_маршрутки=номер_заказа=номер_детали=название_детали
    UNKNOWN
}

data class ParsedQRData(
    val routeCard: String? = null,
    val orderNumber: String? = null,
    val partNumber: String? = null,
    val partName: String? = null,
    val type: QRCodeType = QRCodeType.UNKNOWN
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdDate: Date,
    val isCompleted: Boolean = false,
    val isPaused: Boolean = false,
    val isSynced: Boolean = false
)

@Entity(tableName = "task_items")
data class TaskItem(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val productId: String,
    val productName: String,
    val storageLocation: String,
    val requiredQuantity: Int,
    val scannedQuantity: Int = 0,
    val isCompleted: Boolean = false
)

@Entity(tableName = "shipments")
data class Shipment(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val storageLocation: String,
    val shipmentDate: Date = Date(),
    val operatorId: String? = null,
    val isSynced: Boolean = false
)

data class PrintLabel(
    val productName: String,
    val quantity: Int,
    val storageLocation: String,
    val qrCode: String,
    val date: Date,
    val additionalInfo: Map<String, String>? = null
)