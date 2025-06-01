package com.example.warehouseapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CameraScannerDialog(
    onDismiss: () -> Unit,
    onScanResult: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Сканирование QR кода")
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Наведите камеру на QR код товара")

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Column {
                TextButton(
                    onClick = {
                        // Симуляция успешного сканирования (товар из задания)
                        onScanResult("МК-2024-001=ЗАК-123=ДЕТ-456=Корпус алюминиевый")
                    }
                ) {
                    Text(text = "Тест: Корпус (есть в задании)")
                }

                TextButton(
                    onClick = {
                        // Симуляция сканирования товара не из задания
                        onScanResult("PART:WRONG123")
                    }
                ) {
                    Text(text = "Тест: Неверный товар")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = "Отмена")
            }
        }
    )
}