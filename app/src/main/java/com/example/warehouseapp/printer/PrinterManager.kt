package com.example.warehouseapp.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import com.example.warehouseapp.data.Product
import com.example.warehouseapp.data.TaskItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class PrinterManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var printerSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    companion object {
        private const val PRINTER_NAME = "Xprinter"
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val TAG = "PrinterManager"

        // ESC/POS Commands
        private val ESC_INIT = byteArrayOf(0x1B, 0x40)
        private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        private val ESC_FONT_SIZE_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)
        private val ESC_FONT_SIZE_LARGE = byteArrayOf(0x1D, 0x21, 0x11)
        private val ESC_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x00)
        private val ESC_LINE_FEED = byteArrayOf(0x0A)
    }

    suspend fun connectToPrinter(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e(TAG, "Bluetooth не доступен или выключен")
                return@withContext false
            }

            val pairedDevices = bluetoothAdapter.bondedDevices
            val printerDevice = pairedDevices.find { device ->
                device.name.contains(PRINTER_NAME, ignoreCase = true)
            }

            if (printerDevice == null) {
                Log.e(TAG, "Принтер $PRINTER_NAME не найден в списке сопряженных устройств")
                return@withContext false
            }

            printerSocket = printerDevice.createRfcommSocketToServiceRecord(
                UUID.fromString(SPP_UUID)
            )

            printerSocket?.connect()
            outputStream = printerSocket?.outputStream

            Log.i(TAG, "Успешно подключено к принтеру")
            return@withContext true

        } catch (e: IOException) {
            Log.e(TAG, "Ошибка подключения к принтеру", e)
            disconnect()
            return@withContext false
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            printerSocket?.close()
            outputStream = null
            printerSocket = null
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка при отключении от принтера", e)
        }
    }

    suspend fun printLabel(product: Product): Boolean = withContext(Dispatchers.IO) {
        try {
            if (outputStream == null) {
                if (!connectToPrinter()) {
                    return@withContext false
                }
            }

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

            // Initialize printer
            outputStream?.write(ESC_INIT)

            // Print header
            outputStream?.write(ESC_ALIGN_CENTER)
            outputStream?.write(ESC_FONT_SIZE_LARGE)
            outputStream?.write(ESC_BOLD_ON)
            outputStream?.write("СКЛАДСКАЯ БИРКА\n".toByteArray(Charsets.UTF_8))
            outputStream?.write(ESC_BOLD_OFF)
            outputStream?.write(ESC_FONT_SIZE_NORMAL)
            outputStream?.write(ESC_LINE_FEED)

            // Print product info
            outputStream?.write(ESC_ALIGN_LEFT)
            outputStream?.write("Товар: ${product.name}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("Количество: ${product.quantity}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("Ячейка: ${product.storageLocation}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("Дата: ${dateFormat.format(product.receivedDate)}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write(ESC_LINE_FEED)

            // Print QR code
            val qrBitmap = generateQRCode(product.qrCode)
            if (qrBitmap != null) {
                printBitmap(qrBitmap)
            }

            // Print QR text
            outputStream?.write(ESC_ALIGN_CENTER)
            outputStream?.write(product.qrCode.toByteArray(Charsets.UTF_8))
            outputStream?.write(ESC_LINE_FEED)
            outputStream?.write(ESC_LINE_FEED)

            // Cut paper
            outputStream?.write(ESC_CUT_PAPER)

            outputStream?.flush()

            Log.i(TAG, "Бирка успешно напечатана")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка печати", e)
            return@withContext false
        }
    }

    suspend fun printShipmentLabel(item: TaskItem, quantity: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            if (outputStream == null) {
                if (!connectToPrinter()) {
                    return@withContext false
                }
            }

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

            // Initialize printer
            outputStream?.write(ESC_INIT)

            // Print header
            outputStream?.write(ESC_ALIGN_CENTER)
            outputStream?.write(ESC_FONT_SIZE_LARGE)
            outputStream?.write(ESC_BOLD_ON)
            outputStream?.write("ОТГРУЗКА\n".toByteArray(Charsets.UTF_8))
            outputStream?.write(ESC_BOLD_OFF)
            outputStream?.write(ESC_FONT_SIZE_NORMAL)
            outputStream?.write(ESC_LINE_FEED)

            // Print item info
            outputStream?.write(ESC_ALIGN_LEFT)
            outputStream?.write("Товар: ${item.productName}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("Количество: $quantity\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("Ячейка: ${item.storageLocation}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("Дата: ${dateFormat.format(Date())}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write(ESC_LINE_FEED)
            outputStream?.write(ESC_LINE_FEED)

            // Cut paper
            outputStream?.write(ESC_CUT_PAPER)

            outputStream?.flush()

            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка печати отгрузочной бирки", e)
            return@withContext false
        }
    }

    private fun generateQRCode(text: String): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка генерации QR кода", e)
            null
        }
    }

    private fun printBitmap(bitmap: Bitmap) {
        // Convert bitmap to ESC/POS format
        // This is a simplified version - real implementation would need proper bitmap to ESC/POS conversion
        // For Xprinter V3BT, you would use specific commands for bitmap printing
    }
}