package com.example.warehouseapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.warehouseapp.viewmodel.WarehouseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WarehouseViewModel,
    navController: NavController
) {
    var showPrinterDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Настройки",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            "Назад",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Раздел "Оборудование"
            item {
                Text(
                    "Оборудование",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Настройки принтера
            item {
                SettingsCard(
                    title = "Принтер",
                    subtitle = "Xprinter V3BT",
                    icon = Icons.Filled.Print,
                    isConnected = true, // TODO: Получать из состояния
                    onClick = { showPrinterDialog = true }
                )
            }

            // Настройки сканера
            item {
                SettingsCard(
                    title = "Сканер QR",
                    subtitle = "Bluetooth сканер",
                    icon = Icons.Filled.QrCodeScanner,
                    isConnected = false, // TODO: Получать из состояния
                    onClick = { showScannerDialog = true }
                )
            }

            // Раздел "Сеть"
            item {
                Text(
                    "Сеть и синхронизация",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            // Настройки сервера
            item {
                SettingsCard(
                    title = "Сервер",
                    subtitle = "192.168.1.100:8080",
                    icon = Icons.Filled.Cloud,
                    onClick = { showServerDialog = true }
                )
            }

            // Синхронизация
            item {
                SettingsCard(
                    title = "Синхронизация",
                    subtitle = "Автоматически каждые 5 минут",
                    icon = Icons.Filled.Sync,
                    onClick = { /* TODO: Настройки синхронизации */ }
                )
            }

            // Раздел "Приложение"
            item {
                Text(
                    "Приложение",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            // Очистить кэш
            item {
                SettingsCard(
                    title = "Очистить кэш",
                    subtitle = "Освободить место на устройстве",
                    icon = Icons.Filled.DeleteSweep,
                    onClick = { /* TODO: Очистка кэша */ }
                )
            }

            // Экспорт данных
            item {
                SettingsCard(
                    title = "Экспорт данных",
                    subtitle = "Сохранить журнал в Excel",
                    icon = Icons.Filled.FileDownload,
                    onClick = { /* TODO: Экспорт */ }
                )
            }

            // О программе
            item {
                SettingsCard(
                    title = "О программе",
                    subtitle = "Версия 1.0",
                    icon = Icons.Filled.Info,
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    // Диалоги
    if (showPrinterDialog) {
        PrinterSettingsDialog(
            onDismiss = { showPrinterDialog = false }
        )
    }

    if (showScannerDialog) {
        ScannerSettingsDialog(
            onDismiss = { showScannerDialog = false }
        )
    }

    if (showServerDialog) {
        ServerSettingsDialog(
            onDismiss = { showServerDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isConnected: Boolean? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            isConnected?.let { connected ->
                Icon(
                    if (connected) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = if (connected) "Подключено" else "Отключено",
                    tint = if (connected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PrinterSettingsDialog(onDismiss: () -> Unit) {
    var printerAddress by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки принтера") },
        text = {
            Column {
                Text("Подключение к принтеру Xprinter V3BT")
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = printerAddress,
                    onValueChange = { printerAddress = it },
                    label = { Text("MAC-адрес принтера") },
                    placeholder = { Text("00:00:00:00:00:00") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* TODO: Поиск устройств */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.BluetoothSearching, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Найти принтеры")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { /* TODO: Сохранить */ }) {
                Text("Подключить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun ScannerSettingsDialog(onDismiss: () -> Unit) {
    var scannerType by remember { mutableStateOf("bluetooth") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки сканера") },
        text = {
            Column {
                Text("Выберите тип сканера:")

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = scannerType == "bluetooth",
                        onClick = { scannerType = "bluetooth" }
                    )
                    Text("Bluetooth сканер")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = scannerType == "camera",
                        onClick = { scannerType = "camera" }
                    )
                    Text("Камера планшета")
                }

                if (scannerType == "bluetooth") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { /* TODO: Поиск сканеров */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.BluetoothSearching, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Найти сканеры")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { /* TODO: Сохранить */ }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun ServerSettingsDialog(onDismiss: () -> Unit) {
    var serverUrl by remember { mutableStateOf("192.168.1.100") }
    var serverPort by remember { mutableStateOf("8080") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки сервера") },
        text = {
            Column {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("IP-адрес сервера") },
                    placeholder = { Text("192.168.1.100") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = serverPort,
                    onValueChange = { serverPort = it },
                    label = { Text("Порт") },
                    placeholder = { Text("8080") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* TODO: Тест соединения */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Проверить соединение")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { /* TODO: Сохранить */ }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warehouse,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Складское приложение")
            }
        },
        text = {
            Column {
                Text("Версия: 0.1")
                Text("Сборка: ${android.os.Build.VERSION.RELEASE}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Разработано для управления складскими операциями")
                Spacer(modifier = Modifier.height(8.dp))
                Text("© 2025 Warehouse App")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}