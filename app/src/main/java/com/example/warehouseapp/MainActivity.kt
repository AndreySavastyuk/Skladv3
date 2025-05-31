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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.example.warehouseapp.scanner.*

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var cameraScanner: CameraQRScanner
    private lateinit var bluetoothScanner: BluetoothQRScanner
    private lateinit var printerManager: PrinterManager
    private lateinit var networkManager: NetworkManager

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

        // Request permissions
        requestPermissions()

        setContent {
            WarehouseAppTheme {
                WarehouseApp()
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
fun WarehouseApp() {
    val navController = rememberNavController()
    val viewModel: WarehouseViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Складское приложение") }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("main") {
                MainScreen(
                    onNavigateToReception = { navController.navigate("reception") },
                    onNavigateToShipment = { navController.navigate("shipment") },
                    onNavigateToTasks = { navController.navigate("tasks") }
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
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToReception: () -> Unit,
    onNavigateToShipment: () -> Unit,
    onNavigateToTasks: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onNavigateToReception,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Приемка продукции")
        }

        Button(
            onClick = onNavigateToShipment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Комплектация и выдача")
        }

        Button(
            onClick = onNavigateToTasks,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Задания")
        }
    }
}