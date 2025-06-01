package com.example.warehouseapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.warehouseapp.viewmodel.WarehouseViewModel
import com.example.warehouseapp.data.*
import com.example.warehouseapp.printer.ConnectionState
import com.example.warehouseapp.printer.PrintingState
import java.util.UUID
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.launch

@Composable
fun ReceptionScreen(
    viewModel: WarehouseViewModel,
    navController: NavController
) {
    var isScanning by remember { mutableStateOf(false) }
    var showBlePairing by remember { mutableStateOf(false) }
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
    val scannerState by viewModel.scannerConnectionState.collectAsState()
    val pairingQrCode by viewModel.scannerPairingQrCode.collectAsState()
    val scope = rememberCoroutineScope()

    // Парсинг QR кода
    val parsedQrData = remember(scannedCode) {
        viewModel.parseQRCode(scannedCode)
    }

    // Обработка результата сканирования
    LaunchedEffect(scanResult) {
        if (scanResult.isNotEmpty()) {
            scannedCode = scanResult

            // Автоматическое заполнение полей из QR
            val parsed = viewModel.parseQRCode(scanResult)
            when (parsed.type) {
                QRCodeType.FIXED_FORMAT -> {
                    productName = parsed.partName ?: ""
                    // Звуковое подтверждение успешного сканирования
                    viewModel.scannerBeep()
                }
                QRCodeType.PART, QRCodeType.ASSEMBLY -> {
                    productName = parsed.partNumber ?: ""
                    viewModel.scannerBeep()
                }
                QRCodeType.UNKNOWN -> {
                    errorMessage = "Неизвестный формат QR кода"
                    // Двойной сигнал для ошибки
                    viewModel.scannerBeepError()
                }
            }

            // Очищаем результат для следующего сканирования
            viewModel.updateScanResult("")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок с индикаторами устройств
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Приемка продукции",
                style = MaterialTheme.typography.headlineMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Индикатор сканера
                IconButton(onClick = { showBlePairing = true }) {
                    Icon(
                        imageVector = when (scannerState) {
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED -> Icons.Filled.QrCodeScanner
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTING -> Icons.Filled.Bluetooth
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.DISCONNECTED -> Icons.Filled.BluetoothDisabled
                        },
                        contentDescription = "Сканер",
                        tint = when (scannerState) {
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error
                        }
                    )
                }

                // Индикатор принтера
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
        }

        // Кнопка сканирования (для камеры, если BLE не подключен)
        if (scannerState != com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED) {
            Button(
                onClick = { isScanning = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = printingState != PrintingState.PRINTING
            ) {
                Icon(Icons.Filled.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("Сканировать камерой")
            }
        } else {
            // Информация о подключенном сканере
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("BLE сканер подключен. Сканируйте QR код.")
                }
            }
        }

        // Отображение отсканированного кода
        if (scannedCode.isNotEmpty()) {
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
                            "QR код отсканирован",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                scannedCode = ""
                                productName = ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, "Очистить")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Показываем распарсенные данные
                    when (parsedQrData.type) {
                        QRCodeType.FIXED_FORMAT -> {
                            DetailRow("Маршрутная карта:", parsedQrData.routeCard ?: "")
                            DetailRow("Номер заказа:", parsedQrData.orderNumber ?: "")
                            DetailRow("Номер детали:", parsedQrData.partNumber ?: "")
                            DetailRow("Название:", parsedQrData.partName ?: "")
                        }
                        QRCodeType.PART -> {
                            DetailRow("Тип:", "Деталь")
                            DetailRow("Номер:", parsedQrData.partNumber ?: "")
                        }
                        QRCodeType.ASSEMBLY -> {
                            DetailRow("Тип:", "Сборка")
                            DetailRow("Номер:", parsedQrData.partNumber ?: "")
                        }
                        QRCodeType.UNKNOWN -> {
                            Text(
                                "Код: $scannedCode",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Поля ввода
        OutlinedTextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("Название продукта") },
            leadingIcon = { Icon(Icons.Filled.Label, null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = printingState != PrintingState.PRINTING,
            isError = productName.isEmpty() && scannedCode.isNotEmpty()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                enabled = printingState != PrintingState.PRINTING,
                isError = quantity.isEmpty() && scannedCode.isNotEmpty()
            )

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
                enabled = printingState != PrintingState.PRINTING,
                isError = (storageLocation.isEmpty() || storageLocation.length != 4) && scannedCode.isNotEmpty()
            )
        }

        // Сообщение об ошибке
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

        // Кнопки действий
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val validationErrors = mutableListOf<String>()

                    if (printerState != ConnectionState.CONNECTED) {
                        validationErrors.add("Принтер не подключен")
                    }
                    if (scannedCode.isEmpty()) {
                        validationErrors.add("Отсканируйте QR код")
                    }
                    if (quantity.isEmpty()) {
                        validationErrors.add("Укажите количество")
                    }
                    if (storageLocation.isEmpty()) {
                        validationErrors.add("Укажите ячейку")
                    } else if (storageLocation.length != 4) {
                        validationErrors.add("Ячейка должна содержать 4 символа")
                    }
                    if (productName.isEmpty()) {
                        validationErrors.add("Укажите название продукта")
                    }

                    if (validationErrors.isNotEmpty()) {
                        errorMessage = validationErrors.first()
                        if (validationErrors.contains("Принтер не подключен")) {
                            showPrinterDialog = true
                        }
                    } else {
                        scope.launch {
                            val product = Product(
                                id = UUID.randomUUID().toString(),
                                name = productName,
                                qrCode = scannedCode,
                                quantity = quantity.toIntOrNull() ?: 0,
                                storageLocation = storageLocation,
                                type = when (parsedQrData.type) {
                                    QRCodeType.ASSEMBLY -> ProductType.ASSEMBLY
                                    else -> ProductType.PART
                                },
                                routeCard = parsedQrData.routeCard,
                                orderNumber = parsedQrData.orderNumber,
                                partNumber = parsedQrData.partNumber
                            )

                            viewModel.addProduct(product)

                            // Печать этикетки
                            val printed = viewModel.printReceptionLabel(product)

                            if (printed) {
                                showSuccessDialog = true
                                errorMessage = null
                                // Звуковое подтверждение
                                viewModel.scannerBeep()
                            } else {
                                errorMessage = "Ошибка печати этикетки"
                                viewModel.scannerBeepError()
                            }
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

            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Назад")
            }
        }
    }

    // Диалог успеха
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Успешно")
                }
            },
            text = { Text("Продукт добавлен и бирка отправлена на печать") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        // Сброс формы
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

    // Диалог принтера
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

    // Диалог сопряжения BLE сканера
    if (showBlePairing) {
        BleScannerPairingDialog(
            currentState = scannerState,
            pairingQrCode = pairingQrCode,
            onDismiss = {
                showBlePairing = false
                viewModel.stopScannerPairing()
            },
            onStartPairing = {
                viewModel.startScannerPairing()
            },
            onConnect = { device ->
                scope.launch {
                    viewModel.connectToScanner(device)
                }
            }
        )
    }

    // Диалог сканирования камерой
    if (isScanning) {
        CameraScannerDialog(
            onDismiss = { isScanning = false },
            onScanResult = { result ->
                viewModel.updateScanResult(result)
                isScanning = false
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
fun BleScannerPairingDialog(
    currentState: com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState,
    pairingQrCode: android.graphics.Bitmap?,
    onDismiss: () -> Unit,
    onStartPairing: () -> Unit,
    onConnect: (android.bluetooth.BluetoothDevice) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint = when (currentState) {
                        com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                        com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary
                        com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text("BLE Сканер")
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Статус
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (currentState) {
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (currentState) {
                            com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTING -> {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                            else -> {
                                Icon(
                                    when (currentState) {
                                        com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED -> Icons.Filled.Bluetooth
                                        else -> Icons.Filled.BluetoothDisabled
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = when (currentState) {
                                com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED -> "Сканер подключен"
                                com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTING -> "Подключение..."
                                com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.DISCONNECTED -> "Сканер не подключен"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // QR код для сопряжения
                if (pairingQrCode != null && currentState == com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.DISCONNECTED) {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Отсканируйте QR код сканером Newland",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Image(
                                bitmap = pairingQrCode.asImageBitmap(),
                                contentDescription = "QR код для сопряжения",
                                modifier = Modifier.size(200.dp)
                            )
                        }
                    }
                }

                // Инструкции
                if (currentState == com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.DISCONNECTED && pairingQrCode == null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Подготовка сканера:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("1. Переведите сканер в BLE режим")
                            Text("2. Нажмите 'Создать QR для сопряжения'")
                            Text("3. Отсканируйте QR код сканером")
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (currentState) {
                com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.DISCONNECTED -> {
                    if (pairingQrCode == null) {
                        TextButton(onClick = onStartPairing) {
                            Text("Создать QR для сопряжения")
                        }
                    }
                }
                com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTED -> {
                    TextButton(onClick = onDismiss) {
                        Text("Готово")
                    }
                }
                com.example.warehouseapp.scanner.NewlandBleManager.ConnectionState.CONNECTING -> {
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

@Composable
fun PrinterConnectionDialog(
    currentState: ConnectionState,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
    onTestPrint: () -> Unit
) {
    var macAddress by remember { mutableStateOf("DC:0D:30:XX:XX:XX") } // Шаблон MAC адреса Xprinter

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
                // Статус
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                when (currentState) {
                                    ConnectionState.CONNECTED -> Icons.Filled.Print
                                    else -> Icons.Filled.PrintDisabled
                                },
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
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