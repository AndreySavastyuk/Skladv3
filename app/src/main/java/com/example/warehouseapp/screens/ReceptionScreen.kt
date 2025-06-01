package com.example.warehouseapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.warehouseapp.viewmodel.WarehouseViewModel
import com.example.warehouseapp.data.*
import com.example.warehouseapp.printer.ConnectionState
import com.example.warehouseapp.printer.PrintingState
import java.util.UUID
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch

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
    var showPrinterDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scanResult by viewModel.scanResult.collectAsState()
    val printerState by viewModel.printerConnectionState.collectAsState()
    val printingState by viewModel.printingState.collectAsState()
    val scope = rememberCoroutineScope()

    // Парсинг QR кода
    val parsedQrData = remember(scannedCode) {
        if (scannedCode.contains("=")) {
            val parts = scannedCode.split("=")
            if (parts.size == 4) {
                mapOf(
                    "routeCard" to parts[0],
                    "orderNumber" to parts[1],
                    "partNumber" to parts[2],
                    "partName" to parts[3]
                )
            } else null
        } else null
    }

    // Update scanned code when scan result changes
    LaunchedEffect(scanResult) {
        if (scanResult.isNotEmpty()) {
            scannedCode = scanResult
            // Парсим QR код формата: номер_маршрутки=номер_заказа=номер_детали=название_детали
            val parts = scanResult.split("=")
            if (parts.size == 4) {
                productName = parts[3] // Используем название детали из QR кода
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок с индикатором принтера
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Приемка продукции",
                style = MaterialTheme.typography.headlineMedium
            )

            // Индикатор состояния принтера
            IconButton(onClick = { showPrinterDialog = true }) {
                Icon(
                    imageVector = when (printerState) {
                        ConnectionState.CONNECTED -> Icons.Filled.Print
                        ConnectionState.CONNECTING -> Icons.Filled.Sync
                        ConnectionState.DISCONNECTED -> Icons.Filled.PrintDisabled
                    },
                    contentDescription = "Принтер",
                    tint = when (printerState) {
                        ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                        ConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary
                        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }

        // Scan button
        Button(
            onClick = { isScanning = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = printingState != PrintingState.PRINTING
        ) {
            Icon(Icons.Filled.QrCodeScanner, null)
            Spacer(Modifier.width(8.dp))
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
                    Text(
                        "QR код отсканирован",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Показываем распарсенные данные
                    parsedQrData?.let { data ->
                        DetailRow("Маршрутная карта:", data["routeCard"] ?: "")
                        DetailRow("Номер заказа:", data["orderNumber"] ?: "")
                        DetailRow("Номер детали:", data["partNumber"] ?: "")
                        DetailRow("Название:", data["partName"] ?: "")
                    } ?: Text(
                        "Код: $scannedCode",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Product name input
        OutlinedTextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("Название продукта") },
            leadingIcon = { Icon(Icons.Filled.Label, null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = printingState != PrintingState.PRINTING
        )

        // Quantity and storage location in row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quantity input
            OutlinedTextField(
                value = quantity,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        quantity = it
                    }
                },
                label = { Text("Количество") },
                leadingIcon = { Icon(Icons.Filled.Numbers, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                enabled = printingState != PrintingState.PRINTING
            )

            // Storage location input
            OutlinedTextField(
                value = storageLocation,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isLetterOrDigit() }.uppercase()
                    if (filtered.length <= 4) {
                        storageLocation = filtered
                    }
                },
                label = { Text("Ячейка") },
                leadingIcon = { Icon(Icons.Filled.Inventory2, null) },
                placeholder = { Text("A1B2") },
                modifier = Modifier.weight(1f),
                enabled = printingState != PrintingState.PRINTING
            )
        }

        // Error message
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(error)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Save and print button
            Button(
                onClick = {
                    if (printerState != ConnectionState.CONNECTED) {
                        errorMessage = "Принтер не подключен"
                        showPrinterDialog = true
                    } else if (scannedCode.isNotEmpty() && quantity.isNotEmpty() &&
                        storageLocation.length == 4 && productName.isNotEmpty()) {
                        scope.launch {
                            val product = Product(
                                id = UUID.randomUUID().toString(),
                                name = productName,
                                qrCode = scannedCode,
                                quantity = quantity.toIntOrNull() ?: 0,
                                storageLocation = storageLocation,
                                type = ProductType.PART,
                                routeCard = parsedQrData?.get("routeCard"),
                                orderNumber = parsedQrData?.get("orderNumber"),
                                partNumber = parsedQrData?.get("partNumber")
                            )

                            viewModel.addProduct(product)

                            // Печать этикетки
                            val printed = viewModel.printReceptionLabel(product)

                            if (printed) {
                                showSuccessDialog = true
                                errorMessage = null
                            } else {
                                errorMessage = "Ошибка печати этикетки"
                            }
                        }
                    } else {
                        errorMessage = when {
                            scannedCode.isEmpty() -> "Отсканируйте QR код"
                            quantity.isEmpty() -> "Укажите количество"
                            storageLocation.isEmpty() -> "Укажите ячейку хранения"
                            storageLocation.length != 4 -> "Ячейка должна содержать 4 символа"
                            productName.isEmpty() -> "Укажите название продукта"
                            else -> "Заполните все поля"
                        }
                    }
                },
                enabled = printingState != PrintingState.PRINTING,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                when (printingState) {
                    PrintingState.PRINTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Печать...")
                    }
                    else -> {
                        Icon(Icons.Filled.Print, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранить и печатать бирку")
                    }
                }
            }

            // Clear button
            OutlinedButton(
                onClick = {
                    scannedCode = ""
                    quantity = ""
                    storageLocation = ""
                    productName = ""
                    errorMessage = null
                    viewModel.updateScanResult("")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = printingState != PrintingState.PRINTING
            ) {
                Icon(Icons.Filled.Clear, null)
                Spacer(Modifier.width(8.dp))
                Text("Очистить")
            }

            // Back button
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Назад")
            }
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
                        errorMessage = null
                        viewModel.updateScanResult("")
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Printer dialog
    if (showPrinterDialog) {
        PrinterConnectionDialog(
            currentState = printerState,
            onDismiss = { showPrinterDialog = false },
            onConnect = { macAddress ->
                scope.launch {
                    viewModel.connectPrinter(macAddress)
                }
            },
            onTestPrint = {
                scope.launch {
                    viewModel.testPrint()
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
                        viewModel.updateScanResult("МК-2024-001=ЗАК-123=ДЕТ-456=Корпус алюминиевый")
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

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PrinterConnectionDialog(
    currentState: ConnectionState,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
    onTestPrint: () -> Unit
) {
    var macAddress by remember { mutableStateOf("DC:0D:30:XX:XX:XX") } // Xprinter MAC template

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Print,
                    contentDescription = null,
                    tint = when (currentState) {
                        ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                        ConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary
                        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text("Настройки принтера")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Status
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (currentState) {
                            ConnectionState.CONNECTED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            ConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentState == ConnectionState.CONNECTING) {
                            // Бесконечный индикатор
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            // Индикатор с фиксированным значением
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                progress = 1f
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = when (currentState) {
                                ConnectionState.CONNECTED -> "Принтер подключен"
                                ConnectionState.CONNECTING -> "Подключение..."
                                ConnectionState.DISCONNECTED -> "Принтер не подключен"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                if (currentState != ConnectionState.CONNECTED) {
                    OutlinedTextField(
                        value = macAddress,
                        onValueChange = { macAddress = it },
                        label = { Text("MAC-адрес принтера") },
                        placeholder = { Text("DC:0D:30:XX:XX:XX") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Модель: Xprinter V3BT",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            when (currentState) {
                ConnectionState.DISCONNECTED -> {
                    TextButton(onClick = { onConnect(macAddress) }) {
                        Text("Подключить")
                    }
                }
                ConnectionState.CONNECTED -> {
                    TextButton(onClick = onTestPrint) {
                        Text("Тестовая печать")
                    }
                }
                ConnectionState.CONNECTING -> {
                    TextButton(onClick = {}, enabled = false) {
                        Text("Подключение...")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}