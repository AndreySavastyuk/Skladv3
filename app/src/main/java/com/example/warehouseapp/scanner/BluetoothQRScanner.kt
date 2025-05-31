package com.example.warehouseapp.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

class BluetoothQRScanner(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var scannerSocket: BluetoothSocket? = null
    private var inputReader: BufferedReader? = null

    private val _scanResult = MutableStateFlow<String>("")
    val scanResult: StateFlow<String> = _scanResult

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    companion object {
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val TAG = "BluetoothQRScanner"
    }

    suspend fun connectToScanner(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e(TAG, "Bluetooth не доступен или выключен")
                return@withContext false
            }

            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            scannerSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID))

            scannerSocket?.connect()
            inputReader = BufferedReader(InputStreamReader(scannerSocket?.inputStream))

            _isConnected.value = true

            // Start reading loop
            startReadingLoop()

            Log.i(TAG, "Успешно подключено к сканеру")
            return@withContext true

        } catch (e: IOException) {
            Log.e(TAG, "Ошибка подключения к сканеру", e)
            disconnect()
            return@withContext false
        }
    }

    private suspend fun startReadingLoop() = withContext(Dispatchers.IO) {
        try {
            while (_isConnected.value) {
                val line = inputReader?.readLine()
                if (line != null && line.isNotEmpty()) {
                    _scanResult.value = line.trim()
                    Log.d(TAG, "Отсканировано: $line")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка чтения данных со сканера", e)
            disconnect()
        }
    }

    fun disconnect() {
        try {
            _isConnected.value = false
            inputReader?.close()
            scannerSocket?.close()
            inputReader = null
            scannerSocket = null
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка при отключении от сканера", e)
        }
    }

    fun getPairedScanners(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.filter { device ->
            device.name?.contains("Scanner", ignoreCase = true) == true ||
                    device.name?.contains("Barcode", ignoreCase = true) == true
        } ?: emptyList()
    }
}