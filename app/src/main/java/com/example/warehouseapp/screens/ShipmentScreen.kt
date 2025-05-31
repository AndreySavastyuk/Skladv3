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
    val scanResult by viewModel.scanResult.collectAsState()
    val scope = rememberCoroutineScope()

    // Process scan result
    LaunchedEffect(scanResult) {
        if (scanResult.isNotEmpty() && currentTask != null) {
            val matchingItem = currentTask?.items?.find { item ->
                // Match by product ID from QR code
                scanResult.contains(item.productId)
            }

            if (matchingItem != null) {
                selectedItem = matchingItem
                showQuantityDialog = true
                // Play success sound
            } else {
                // Play error sound
                showErrorDialog = true
            }
            viewModel.updateScanResult("")
        }
    }

    if (currentTask == null) {
        // No task selected - show message
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
        // Task header
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
                    Text(
                        text = currentTask!!.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentTask!!.isPaused) {
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

                // Progress indicator
                val progress = currentTask!!.items.count { it.isCompleted }.toFloat() /
                        currentTask!!.items.size.toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${(progress * 100).toInt()}% выполнено",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scan button
        Button(
            onClick = { isScanning = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сканировать товар")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Items list
        Text(
            text = "Позиции для сборки:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currentTask!!.items) { item ->
                TaskItemCard(
                    item = item,
                    onItemClick = {
                        selectedItem = item
                        showQuantityDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    // Pause task logic
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

            Button(
                onClick = {
                    // Complete task logic
                    if (currentTask!!.isCompleted) {
                        scope.launch {
                            // Send completion to server
                            navController.navigate("tasks")
                        }
                    }
                },
                enabled = currentTask!!.isCompleted,
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

    // Quantity input dialog
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
                    Text("Требуется: ${selectedItem!!.requiredQuantity - selectedItem!!.scannedQuantity}")

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = enteredQuantity,
                        onValueChange = { enteredQuantity = it },
                        label = { Text("Количество") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val quantity = enteredQuantity.toIntOrNull() ?: 0
                        if (quantity > 0) {
                            viewModel.completeTaskItem(selectedItem!!.id, quantity)
                            // Print label
                            // printerManager.printShipmentLabel(selectedItem!!, quantity)
                        }
                        showQuantityDialog = false
                        enteredQuantity = ""
                    }
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

    // Error dialog for wrong QR
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ошибка")
                }
            },
            text = { Text("Отсканированный товар не найден в текущем задании") },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Scanner modal
    if (isScanning) {
        ScannerDialog(
            onDismiss = { isScanning = false },
            onScanResult = { result ->
                viewModel.updateScanResult(result)
                isScanning = false
            }
        )
    }
}

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
                Text(
                    text = "Собрано: ${item.scannedQuantity} / ${item.requiredQuantity}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (item.isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Выполнено",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
            } else {
                CircularProgressIndicator(
                    progress = { item.scannedQuantity.toFloat() / item.requiredQuantity.toFloat() },
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
fun ScannerDialog(
    onDismiss: () -> Unit,
    onScanResult: (String) -> Unit
) {
    // In real implementation, this would show camera preview
    // For now, it's a simulation
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сканирование QR кода") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Наведите камеру на QR код товара")
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Simulate successful scan
                    onScanResult("PART:ITEM123")
                }
            ) {
                Text("Симулировать сканирование")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}