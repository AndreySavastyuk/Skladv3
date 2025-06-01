package com.example.warehouseapp.screens.dialogs

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
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
import com.example.warehouseapp.printer.ConnectionState
import com.example.warehouseapp.printer.PrintingState
import kotlinx.coroutines.launch

@Composable
fun PrinterSelectionDialog(
    currentState: ConnectionState,
    printingState: PrintingState,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onTestPrint: () -> Unit,
    onSearchDevices: () -> List<BluetoothDevice>
) {
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualMacAddress by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Загружаем список устройств при открытии диалога
    LaunchedEffect(Unit) {
        pairedDevices = onSearchDevices()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                    Text("Принтер Xprinter V3BT")
                }

                // Кнопка обновления списка
                if (currentState == ConnectionState.DISCONNECTED) {
                    IconButton(
                        onClick = {
                            pairedDevices = onSearchDevices()
                        }
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Обновить список",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Статус подключения
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (currentState) {
                            ConnectionState.CONNECTED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            ConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (currentState) {
                            ConnectionState.CONNECTING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    when (currentState) {
                                        ConnectionState.CONNECTED -> Icons.Filled.CheckCircle
                                        else -> Icons.Filled.Error
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = when (currentState) {
                                        ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when (currentState) {
                                    ConnectionState.CONNECTED -> "Подключен"
                                    ConnectionState.CONNECTING -> "Подключение..."
                                    ConnectionState.DISCONNECTED -> "Не подключен"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            if (printingState == PrintingState.PRINTING) {
                                Text(
                                    "Идет печать...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Если не подключен, показываем список устройств
                if (currentState == ConnectionState.DISCONNECTED) {
                    if (!showManualInput) {
                        // Список спаренных принтеров
                        Text(
                            "Спаренные принтеры:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (pairedDevices.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Filled.BluetoothDisabled,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Принтеры не найдены",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Убедитесь, что принтер включен и сопряжен с устройством",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pairedDevices) { device ->
                                    PrinterDeviceCard(
                                        device = device,
                                        isSelected = selectedDevice == device,
                                        onClick = { selectedDevice = device }
                                    )
                                }
                            }
                        }

                        // Кнопка для ручного ввода
                        TextButton(
                            onClick = { showManualInput = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Ввести MAC-адрес вручную")
                        }
                    } else {
                        // Ручной ввод MAC-адреса
                        OutlinedTextField(
                            value = manualMacAddress,
                            onValueChange = { manualMacAddress = it.uppercase() },
                            label = { Text("MAC-адрес принтера") },
                            placeholder = { Text("DC:0D:30:XX:XX:XX") },
                            leadingIcon = {
                                Icon(Icons.Filled.Bluetooth, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        TextButton(
                            onClick = {
                                showManualInput = false
                                manualMacAddress = ""
                            }
                        ) {
                            Text("Вернуться к списку")
                        }
                    }
                }

                // Информация о принтере
                if (currentState == ConnectionState.CONNECTED) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Информация о принтере",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Модель: Xprinter V3BT")
                            Text("Формат этикетки: 57x40 мм")
                            Text("Разрешение: 203 DPI")
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (currentState) {
                ConnectionState.DISCONNECTED -> {
                    Button(
                        onClick = {
                            val macToConnect = when {
                                showManualInput && manualMacAddress.isNotBlank() -> manualMacAddress
                                selectedDevice != null -> selectedDevice!!.address
                                else -> null
                            }

                            macToConnect?.let { mac ->
                                onConnect(mac)
                            }
                        },
                        enabled = (showManualInput && manualMacAddress.isNotBlank()) ||
                                (!showManualInput && selectedDevice != null)
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Подключить")
                    }
                }
                ConnectionState.CONNECTED -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            enabled = printingState != PrintingState.PRINTING
                        ) {
                            Icon(Icons.Filled.LinkOff, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Отключить")
                        }

                        Button(
                            onClick = onTestPrint,
                            enabled = printingState != PrintingState.PRINTING
                        ) {
                            when (printingState) {
                                PrintingState.PRINTING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                else -> {
                                    Icon(Icons.Filled.Print, contentDescription = null)
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (printingState == PrintingState.PRINTING)
                                    "Печать..."
                                else
                                    "Тест печати"
                            )
                        }
                    }
                }
                ConnectionState.CONNECTING -> {
                    Button(
                        onClick = {},
                        enabled = false
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Подключение...")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = currentState != ConnectionState.CONNECTING &&
                        printingState != PrintingState.PRINTING
            ) {
                Text("Закрыть")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterDeviceCard(
    device: BluetoothDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Print,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = try {
                        device.name ?: "Неизвестное устройство"
                    } catch (e: SecurityException) {
                        "Неизвестное устройство"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Выбрано",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}