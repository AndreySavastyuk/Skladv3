package com.example.warehouseapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.warehouseapp.viewmodel.WarehouseViewModel
import com.example.warehouseapp.data.*
import com.example.warehouseapp.printer.ConnectionState
import com.example.warehouseapp.printer.PrintingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipmentScreen(
    viewModel: WarehouseViewModel,
    navController: NavController
) {
    val currentTask by viewModel.currentTask.collectAsState()
    var isScanning by remember { mutableStateOf(false) }
    var showQuantityDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<TaskItem?>(null) }
    var enteredQuantity by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scanResult by viewModel.scanResult.collectAsState()
    val scannerState by viewModel.scannerConnectionState.collectAsState()
    val printerState by viewModel.printerConnectionState.collectAsState()
    val printingState by viewModel.printingState.collectAsState()
    val scope = rememberCoroutineScope()

    // Получаем элементы текущего задания
    val taskItems = currentTask?.let { task ->
        viewModel.getTaskItems(task.id)
    } ?: emptyList()

    // Обработка результата сканирования
    LaunchedEffect(scanResult) {
        if (scanResult.isNotEmpty() && currentTask != null) {
            // Парсим QR код
            val parsedQr = viewModel.parseQRCode(scanResult)

            // Ищем соответствующий товар в задании
            val matchingItem = taskItems.find { item ->
                when (parsedQr.type) {
                    QRCodeType.FIXED_FORMAT -> {
                        // Для фиксированного формата сравниваем номер детали
                        item.productId == parsedQr.partNumber
                    }
                    QRCodeType.PART, QRCodeType.ASSEMBLY -> {
                        // Для простых форматов
                        item.productId == parsedQr.partNumber
                    }
                    else -> {
                        // Пробуем найти ID продукта в отсканированной строке
                        scanResult.contains(item.productId)
                    }
                }
            }

            if (matchingItem != null) {
                selectedItem = matchingItem
                showQuantityDialog = true
                // Звуковое подтверждение
                viewModel.scannerBeep()
            } else {
                // Звуковой сигнал ошибки
                viewModel.scannerBeepError()
                errorMessage = "Товар не найден в текущем задании"
                showErrorDialog = true
            }
            viewModel.updateScanResult("")
        }
    }

    if (currentTask == null) {
        // Нет выбранного задания
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет активного задания",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Выберите задание из списка",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { navController.navigate("tasks") }
            ) {
                Text("К списку заданий")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок задания
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTask?.name ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (currentTask?.isPaused == true) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Приостановлено") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Прогресс выполнения
                val completedCount = taskItems.count { it.isCompleted }
                val totalCount = taskItems.size
                val progress = if (totalCount > 0) {
                    completedCount.toFloat() / totalCount.toFloat()
                } else {
                    0f
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Выполнено: $completedCount из $totalCount (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Индикаторы устройств
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Состояние сканера
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = when (scannerState) {
                        com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED ->
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (scannerState) {
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED ->
                                Icons.Default.QrCodeScanner
                            else -> Icons.Default.QrCode
                        },
                        contentDescription = null,
                        tint = when (scannerState) {
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED ->
                                Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (scannerState) {
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED ->
                                "BLE сканер"
                            else -> "Камера"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Состояние принтера
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = when (printerState) {
                        ConnectionState.CONNECTED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (printerState) {
                            ConnectionState.CONNECTED -> Icons.Default.Print
                            else -> Icons.Default.PrintDisabled
                        },
                        contentDescription = null,
                        tint = when (printerState) {
                            ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Принтер",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка сканирования
        if (scannerState == com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Сканируйте QR код товара")
                }
            }
        } else {
            Button(
                onClick = { isScanning = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Сканировать камерой")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Список позиций
        Text(
            text = "Позиции для сборки:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (taskItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Нет позиций для сборки",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(taskItems) { item ->
                    TaskItemCard(
                        item = item,
                        onItemClick = {
                            selectedItem = item
                            showQuantityDialog = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопки действий
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    currentTask?.let { task ->
                        viewModel.pauseTask(task.id)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Отложить")
            }

            val isTaskCompleted = taskItems.isNotEmpty() && taskItems.all { it.isCompleted }

            Button(
                onClick = {
                    if (isTaskCompleted) {
                        scope.launch {
                            // Отправляем на сервер
                            navController.navigate("tasks")
                        }
                    }
                },
                enabled = isTaskCompleted,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Завершить")
            }
        }
    }

    // Диалог ввода количества
    if (showQuantityDialog && selectedItem != null) {
        AlertDialog(
            onDismissRequest = {
                showQuantityDialog = false
                enteredQuantity = ""
            },
            title = { Text("Введите количество") },
            text = {
                Column {
                    Text("Товар: ${selectedItem!!.productName}")
                    Text("Ячейка: ${selectedItem!!.storageLocation}")

                    val remaining = selectedItem!!.requiredQuantity - selectedItem!!.scannedQuantity
                    Text("Требуется: $remaining шт.")

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = enteredQuantity,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() }) {
                                enteredQuantity = value
                            }
                        },
                        label = { Text("Количество") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = enteredQuantity.toIntOrNull()?.let { it > remaining } ?: false,
                        supportingText = {
                            val entered = enteredQuantity.toIntOrNull() ?: 0
                            if (entered > remaining) {
                                Text(
                                    "Превышает требуемое количество",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val quantity = enteredQuantity.toIntOrNull() ?: 0
                        val remaining = selectedItem!!.requiredQuantity - selectedItem!!.scannedQuantity

                        if (quantity > 0 && quantity <= remaining) {
                            scope.launch {
                                viewModel.completeTaskItem(selectedItem!!.id, quantity)

                                // Печать этикетки
                                if (printerState == ConnectionState.CONNECTED) {
                                    viewModel.printShipmentLabel(selectedItem!!, quantity)
                                }

                                showQuantityDialog = false
                                enteredQuantity = ""

                                // Звуковое подтверждение
                                viewModel.scannerBeep()
                            }
                        }
                    },
                    enabled = enteredQuantity.toIntOrNull()?.let {
                        it > 0 && it <= (selectedItem!!.requiredQuantity - selectedItem!!.scannedQuantity)
                    } ?: false
                ) {
                    Text("Печать и подтверждение")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showQuantityDialog = false
                        enteredQuantity = ""
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог ошибки
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ошибка")
                }
            },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    errorMessage = ""
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Диалог сканирования камерой
    if (isScanning) {
        CameraScannerDialog(
            onDismiss = {
                isScanning = false
            },
            onScanResult = { result ->
                viewModel.updateScanResult(result)
                isScanning = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItemCard(
    item: TaskItem,
    onItemClick: () -> Unit
) {
    Card(
        onClick = onItemClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Ячейка: ${item.storageLocation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Собрано: ${item.scannedQuantity} / ${item.requiredQuantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (item.isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Выполнено",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp)
                ) {
                    CircularProgressIndicator(
                        progress = {
                            if (item.requiredQuantity > 0) {
                                item.scannedQuantity.toFloat() / item.requiredQuantity.toFloat()
                            } else {
                                0f
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "${(item.scannedQuantity * 100 / item.requiredQuantity.coerceAtLeast(1))}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}