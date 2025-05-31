package com.example.warehouseapp.database

import androidx.room.*
import com.example.warehouseapp.data.Product
import com.example.warehouseapp.data.ProductType
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY receivedDate DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products WHERE isSynced = 0")
    suspend fun getUnsyncedProducts(): List<Product>
}

@Database(
    entities = [Product::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WarehouseDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromProductType(type: ProductType): String {
        return type.name
    }

    @TypeConverter
    fun toProductType(type: String): ProductType {
        return ProductType.valueOf(type)
    }
}