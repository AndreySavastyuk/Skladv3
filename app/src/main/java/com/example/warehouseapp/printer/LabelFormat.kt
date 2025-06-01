package com.example.warehouseapp.printer

import android.graphics.*
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Базовый интерфейс для форматов этикеток
 */
interface LabelFormat {
    val widthMm: Double
    val heightMm: Double
    val dpi: Int

    val widthPx: Int
        get() = (widthMm / 25.4 * dpi).toInt()

    val heightPx: Int
        get() = (heightMm / 25.4 * dpi).toInt()

    fun createBitmap(data: LabelData): Bitmap
}

/**
 * Формат этикетки 57x40 мм для приемки
 */
class AcceptanceLabelFormat57x40 : LabelFormat {
    override val widthMm = 57.0
    override val heightMm = 40.0
    override val dpi = 203

    companion object {
        private const val TAG = "AcceptanceLabel"
    }

    override fun createBitmap(data: LabelData): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val margin = 8f

        // Рамка этикетки
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, widthPx - 1f, heightPx - 1f, borderPaint)

        // QR-код
        val qrX = 16f
        val qrY = 95f
        val qrSize = 200

        val qrBitmap = generateQRCode(data.qrData, qrSize)
        canvas.drawBitmap(qrBitmap, qrX, qrY, null)

        // Номер детали - крупно по центру
        val partNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create("Arial", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val centerX = widthPx / 2f

        // Адаптивное масштабирование текста
        var fontSize = 42f
        partNumberPaint.textSize = fontSize
        var textWidth = partNumberPaint.measureText(data.partNumber)
        val maxWidth = widthPx - 16f

        while (textWidth > maxWidth && fontSize > 20f) {
            fontSize -= 2f
            partNumberPaint.textSize = fontSize
            textWidth = partNumberPaint.measureText(data.partNumber)
        }

        canvas.drawText(data.partNumber, centerX, 47f, partNumberPaint)

        // Рамка ячейки
        val cellBoxX = 233f
        val cellBoxY = 177f
        val cellBoxWidth = 195f
        val cellBoxHeight = 119f
        canvas.drawRect(cellBoxX, cellBoxY, cellBoxX + cellBoxWidth, cellBoxY + cellBoxHeight, borderPaint)

        // Текст ячейки - крупно
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 79f
            typeface = Typeface.create("Arial", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellTextY = cellBoxY + (cellBoxHeight + cellPaint.textSize) / 2 - 6f
        canvas.drawText(data.location, cellBoxX + cellBoxWidth / 2, cellTextY, cellPaint)

        // Наименование детали
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }

        // Обрезаем длинное наименование
        val maxNameWidth = widthPx - 253f
        var displayName = data.description
        if (namePaint.measureText(displayName) > maxNameWidth) {
            while (namePaint.measureText(displayName + "...") > maxNameWidth && displayName.length > 1) {
                displayName = displayName.substring(0, displayName.length - 1)
            }
            displayName += "..."
        }
        canvas.drawText(displayName, 243f, 82f, namePaint)

        // Номер заказа
        val orderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        canvas.drawText(data.orderNumber, 223f, 154f, orderPaint)

        // Количество
        val quantityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        data.quantity?.let { qty ->
            canvas.drawText("Кол-во: $qty шт", 21f, 87f, quantityPaint)
        }

        // Дата
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        data.acceptanceDate?.let { date ->
            canvas.drawText("Дата: $date", 110f, 86f, datePaint)
        }

        return bitmap
    }

    /**
     * Генерация QR-кода с поддержкой UTF-8
     */
    private fun generateQRCode(data: String, size: Int): Bitmap {
        return try {
            val writer = QRCodeWriter()
            val hints = HashMap<EncodeHintType, Any>().apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }

            Log.d(TAG, "Generating QR code: $data")

            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate QR code", e)
            // Возвращаем пустой белый квадрат в случае ошибки
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
        }
    }
}

/**
 * Формат этикетки для комплектации
 */
class PickingLabelFormat57x40 : LabelFormat {
    override val widthMm = 57.0
    override val heightMm = 40.0
    override val dpi = 203

    companion object {
        private const val TAG = "PickingLabel"
    }

    override fun createBitmap(data: LabelData): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val margin = 8f

        // Рамка
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, widthPx - 1f, heightPx - 1f, borderPaint)

        // Заголовок "КОМПЛЕКТАЦИЯ"
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create("Arial", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        var yPos = margin + 16f
        canvas.drawText("КОМПЛЕКТАЦИЯ", widthPx / 2f, yPos, headerPaint)

        // Линия после заголовка
        yPos += 6f
        canvas.drawLine(margin, yPos, widthPx - margin, yPos, borderPaint)
        yPos += 10f

        // Информация о товаре
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }

        canvas.drawText("Товар: ${data.partNumber}", margin, yPos, textPaint)
        yPos += 20f

        canvas.drawText("Количество: ${data.quantity} шт", margin, yPos, textPaint)
        yPos += 20f

        canvas.drawText("Ячейка: ${data.location}", margin, yPos, textPaint)

        // QR-код справа внизу
        val qrSize = 100
        val qrX = widthPx - qrSize - margin.toInt()
        val qrY = heightPx - qrSize - margin.toInt()
        val qrBitmap = generateQRCode(data.qrData, qrSize)
        canvas.drawBitmap(qrBitmap, qrX.toFloat(), qrY.toFloat(), null)

        return bitmap
    }

    /**
     * Генерация QR-кода
     */
    private fun generateQRCode(data: String, size: Int): Bitmap {
        return try {
            val writer = QRCodeWriter()
            val hints = HashMap<EncodeHintType, Any>().apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 0)
            }

            Log.d(TAG, "Generating picking QR: $data")

            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate picking QR", e)
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
        }
    }
}