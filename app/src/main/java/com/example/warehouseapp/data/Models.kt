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
    val isSynced: Boolean = false
)

enum class ProductType {
    PART,
    ASSEMBLY
}

enum class QRCodeType {
    PART,
    ASSEMBLY,
    UNKNOWN
}

data class Task(
    val id: String,
    val name: String,
    val items: List<TaskItem>,
    val createdDate: Date,
    val isCompleted: Boolean = false,
    val isPaused: Boolean = false
)

data class TaskItem(
    val id: String,
    val productId: String,
    val productName: String,
    val storageLocation: String,
    val requiredQuantity: Int,
    val scannedQuantity: Int = 0,
    val isCompleted: Boolean = false
)

data class PrintLabel(
    val productName: String,
    val quantity: Int,
    val storageLocation: String,
    val qrCode: String,
    val date: Date
)
