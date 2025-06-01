package com.example.warehouseapp.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.warehouseapp.scanner.CameraQRScanner
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas

@Composable
fun ScannerScreen(
    onScanResult: (String) -> Unit,
    onDismiss: () -> Unit,
    useBluetoothScanner: Boolean = false
) {
    var isScanning by remember { mutableStateOf(true) }
    var lastScannedCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Заголовок
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (useBluetoothScanner)
                                "Bluetooth сканер"
                            else
                                "Сканирование QR кода",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                if (useBluetoothScanner) {
                    // Bluetooth сканер UI
                    BluetoothScannerUI(
                        isScanning = isScanning,
                        lastScannedCode = lastScannedCode,
                        onScanResult = { code ->
                            lastScannedCode = code
                            isScanning = false
                            onScanResult(code)
                        }
                    )
                } else {
                    // Камера UI
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    val cameraScanner = CameraQRScanner(ctx)
                                    cameraScanner.startScanning(
                                        lifecycleOwner,
                                        this
                                    ) { qrCode ->
                                        if (isScanning && qrCode != lastScannedCode) {
                                            lastScannedCode = qrCode
                                            isScanning = false
                                            onScanResult(qrCode)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Рамка сканирования
                        ScannerOverlay()
                    }
                }

                // Информационная панель
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (lastScannedCode.isNotEmpty()) {
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Код отсканирован успешно!",
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = if (useBluetoothScanner)
                                    "Ожидание данных от сканера..."
                                else
                                    "Наведите камеру на QR код",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (!useBluetoothScanner) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Автоматическое закрытие после успешного сканирования
    LaunchedEffect(lastScannedCode) {
        if (lastScannedCode.isNotEmpty()) {
            delay(1500)
            onDismiss()
        }
    }
}

@Composable
fun ScannerOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Затемнение вокруг рамки
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scanAreaSize = size.width * 0.7f
            val left = (size.width - scanAreaSize) / 2
            val top = (size.height - scanAreaSize) / 2

            // Затемнение
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = size
            )

            // Прозрачная область сканирования
            drawRect(
                color = Color.Transparent,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(scanAreaSize, scanAreaSize),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )
        }

        // Углы рамки
        Box(
            modifier = Modifier
                .size(250.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            // Верхний левый угол
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(50.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(4.dp)
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            // Верхний правый угол
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(50.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(4.dp)
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            // Нижний левый угол
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(50.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(4.dp)
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            // Нижний правый угол
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(50.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(4.dp)
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun BluetoothScannerUI(
    isScanning: Boolean,
    lastScannedCode: String,
    onScanResult: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.BluetoothSearching,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Bluetooth сканер активен",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        if (isScanning) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                "Ожидание сканирования...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Симуляция для тестирования
        if (lastScannedCode.isEmpty()) {
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = {
                    onScanResult("TEST=ORDER=001=Тестовый товар")
                }
            ) {
                Text("Симулировать сканирование")
            }
        }
    }
}

