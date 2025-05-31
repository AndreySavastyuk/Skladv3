package com.example.warehouseapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.warehouseapp.viewmodel.WarehouseViewModel
import com.example.warehouseapp.data.*
import java.util.UUID

@Composable
fun ReceptionScreen(
    viewModel: WarehouseViewModel,
    navController: NavController
) {
    var isScanning by remember { mutableStateOf(false) }
    var scannedCode by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var storageLocation by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val scanResult by viewModel.scanResult.collectAsState()

    // Update scanned code when scan result changes
    LaunchedEffect(scanResult) {
        if (scanResult.isNotEmpty()) {
            scannedCode = scanResult
            val qrType = viewModel.processQRCode(scanResult)
            // Auto-fill product name based on QR type
            productName = when (qrType) {
                //QRCodeType.PART -> "Деталь: ${scanResult.removePrefix("PART:")}"
                //QRCodeType.ASSEMBLY -> "Сборка: ${scanResult.removePrefix("ASSEMBLY:")}"
                else -> ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Приемка продукции",
            style = MaterialTheme.typography.headlineMedium
        )

        // Scan button
        Button(
            onClick = { isScanning = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сканировать QR код")
        }

        // Display scanned code
        if (scannedCode.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("QR код: $scannedCode")
                    //Text("Тип: ${viewModel.processQRCode(scannedCode).name}")
                }
            }
        }

        // Product name input
        OutlinedTextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("Название продукта") },
            modifier = Modifier.fillMaxWidth()
        )

        // Quantity input
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Количество") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Storage location input
        OutlinedTextField(
            value = storageLocation,
            onValueChange = { storageLocation = it },
            label = { Text("Номер хранения") },
            modifier = Modifier.fillMaxWidth()
        )

        // Save and print button
        Button(
            onClick = {
                if (scannedCode.isNotEmpty() && quantity.isNotEmpty() && storageLocation.isNotEmpty()) {
                    val product = Product(
                        id = UUID.randomUUID().toString(),
                        name = productName,
                        qrCode = scannedCode,
                        quantity = quantity.toIntOrNull() ?: 0,
                        storageLocation = storageLocation,
                        type = when (viewModel.processQRCode(scannedCode)) {
                            //QRCodeType.PART -> ProductType.PART
                            //QRCodeType.ASSEMBLY -> ProductType.ASSEMBLY
                            else -> ProductType.PART
                        }
                    )

                    viewModel.addProduct(product)

                    // Print label
                    // printerManager.printLabel(product)

                    showSuccessDialog = true
                }
            },
            enabled = scannedCode.isNotEmpty() && quantity.isNotEmpty() && storageLocation.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить и печатать бирку")
        }

        // Back button
        TextButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Назад")
        }
    }

    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Успешно") },
            text = { Text("Продукт добавлен и бирка отправлена на печать") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        // Reset form
                        scannedCode = ""
                        quantity = ""
                        storageLocation = ""
                        productName = ""
                        viewModel.updateScanResult("")
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Scanner modal
    if (isScanning) {
        // Here would be the camera scanner UI
        // For now, we'll simulate with a dialog
        AlertDialog(
            onDismissRequest = { isScanning = false },
            title = { Text("Сканирование QR кода") },
            text = { Text("Наведите камеру на QR код...") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Simulate scan result
                        viewModel.updateScanResult("PART:12345")
                        isScanning = false
                    }
                ) {
                    Text("Симулировать сканирование")
                }
            },
            dismissButton = {
                TextButton(onClick = { isScanning = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}