package com.example.warehouseapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.warehouseapp.ui.theme.WarehouseAppTheme
import com.example.warehouseapp.viewmodel.WarehouseViewModel
import com.example.warehouseapp.screens.*
import com.example.warehouseapp.data.*
import com.example.warehouseapp.network.*
import com.example.warehouseapp.printer.*
import androidx.room.Room
import com.example.warehouseapp.database.WarehouseDatabase
import com.example.warehouseapp.scanner.BluetoothQRScanner
import com.example.warehouseapp.scanner.CameraQRScanner
import com.example.warehouseapp.scanner.NewlandScannerAdapter

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var cameraScanner: CameraQRScanner
    private lateinit var bluetoothScanner: BluetoothQRScanner
    private lateinit var printerManager: PrinterManager
    private lateinit var networkManager: NetworkManager
    private lateinit var database: WarehouseDatabase
    private lateinit var newlandScanner: NewlandScannerAdapter

    private val requestBluetoothPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize components
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        cameraScanner = CameraQRScanner(this)
        bluetoothScanner = BluetoothQRScanner(this)
        printerManager = PrinterManager(this)
        networkManager = NetworkManager(this)
        newlandScanner = NewlandScannerAdapter(this)

        // Initialize database
        database = Room.databaseBuilder(
            applicationContext,
            WarehouseDatabase::class.java,
            "warehouse_database"
        ).build()

        // Request permissions
        requestPermissions()

        setContent {
            WarehouseAppTheme {
                WarehouseApp(
                    database = database,
                    networkManager = networkManager,
                    printerManager = printerManager,
                    scannerAdapter = newlandScanner
                )
            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE
        )

        requestBluetoothPermission.launch(permissions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseApp(
    database: WarehouseDatabase,
    networkManager: NetworkManager,
    printerManager: PrinterManager,
    scannerAdapter: NewlandScannerAdapter
) {
    val navController = rememberNavController()
    val viewModel: WarehouseViewModel = viewModel()

    // Initialize ViewModel with dependencies
    LaunchedEffect(Unit) {
        viewModel.initialize(database, networkManager, printerManager, scannerAdapter)
    }

    Scaffold { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("main") {
                MainScreen(
                    onNavigateToReception = { navController.navigate("reception") },
                    onNavigateToShipment = { navController.navigate("shipment") },
                    onNavigateToTasks = { navController.navigate("tasks") },
                    onNavigateToJournal = { navController.navigate("journal") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("reception") {
                ReceptionScreen(viewModel, navController)
            }
            composable("shipment") {
                ShipmentScreen(viewModel, navController)
            }
            composable("tasks") {
                TasksScreen(viewModel, navController)
            }
            composable("journal") {
                JournalScreen(viewModel, navController)
            }
            composable("settings") {
                SettingsScreen(viewModel, navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToReception: () -> Unit,
    onNavigateToShipment: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val buttonBorder = BorderStroke(1.dp, borderColor)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Управление складом",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Логотип склада
            Icon(
                imageVector = Icons.Filled.Warehouse,
                contentDescription = "Логотип приложения",
                modifier = Modifier
                    .size(160.dp)
                    .padding(bottom = 48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Основные кнопки
            val buttonModifier = Modifier
                .fillMaxWidth()
                .height(72.dp)

            MainScreenButton(
                text = "Приемка продукции",
                onClick = onNavigateToReception,
                modifier = buttonModifier,
                border = buttonBorder,
                icon = Icons.Filled.Inventory
            )

            Spacer(modifier = Modifier.height(20.dp))

            MainScreenButton(
                text = "Комплектация заказа",
                onClick = onNavigateToShipment,
                modifier = buttonModifier,
                border = buttonBorder,
                icon = Icons.Filled.ShoppingCart
            )

            Spacer(modifier = Modifier.height(20.dp))

            MainScreenButton(
                text = "Журнал операций",
                onClick = onNavigateToJournal,
                modifier = buttonModifier,
                border = buttonBorder,
                icon = Icons.Filled.History
            )

            Spacer(modifier = Modifier.height(20.dp))

            MainScreenButton(
                text = "Настройки",
                onClick = onNavigateToSettings,
                modifier = buttonModifier,
                border = buttonBorder,
                icon = Icons.Filled.Settings
            )
        }
    }
}

@Composable
private fun MainScreenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}