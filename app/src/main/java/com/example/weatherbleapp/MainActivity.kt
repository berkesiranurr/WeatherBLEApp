// BLE Weather App - Açıklamalı Versiyon
package com.example.weatherbleapp

// Gerekli Android ve BLE (Bluetooth Low Energy) kütüphaneleri
import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlin.math.roundToInt
import com.example.weatherbleapp.ui.theme.WeatherBLEAppTheme
import java.util.*

class MainActivity : ComponentActivity() {

    // Bluetooth yönetimi için temel nesneler
    private lateinit var bluetoothAdapter: BluetoothAdapter // Telefonun Bluetooth adaptörü
    private var bluetoothLeScanner: BluetoothLeScanner? = null // BLE tarayıcı
    private var bluetoothGatt: BluetoothGatt? = null // GATT bağlantısı
    private val TAG = "BLEScan" // Log mesajları için etiket

    // BLE servisi ve karakteristikleri için UUID'ler
    private val WEATHER_SERVICE_UUID = UUID.fromString("00000002-0000-0000-FDFD-FDFDFDFDFDFD")
    private val TEMPERATURE_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb")
    private val HUMIDITY_UUID = UUID.fromString("00002A6F-0000-1000-8000-00805f9b34fb")

    // Kullanıcı arayüzünde sıcaklık ve nemi tutan değişkenler
    private var temperatureState: MutableState<Int?> = mutableStateOf(null)
    private var humidityState: MutableState<Int?> = mutableStateOf(null)

    // Uygulama başladığında çalışacak kod bloğu
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Kenarlıklar dahil ekranı kullan

        // Bluetooth yöneticisini al
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter // Bluetooth adaptörünü al

        // Gerekli izinler listesi
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        // Compose UI'yi başlat
        setContent {
            WeatherBLEAppTheme {
                val foundDevices = remember { mutableStateListOf<String>() } // Bulunan cihaz listesi
                temperatureState = remember { mutableStateOf(null) } // Sıcaklık UI state
                humidityState = remember { mutableStateOf(null) } // Nem UI state

                // Eğer tüm izinler verildiyse BLE taramasını başlat
                val allGranted = permissions.all {
                    ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                }

                if (allGranted) {
                    startBLEScan(foundDevices)
                } else {
                    ActivityCompat.requestPermissions(this, permissions, 1)
                    Log.d(TAG, "Permission not granted yet.")
                }

                // Scaffold UI bileşeni (Sayfa yapısı)
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .padding(16.dp)
                        ) {
                            Text("Found BLE Devices:")
                            for (device in foundDevices) {
                                Text("- $device")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Temperature: ${temperatureState.value ?: "-"} °C")
                            Text("Humidity: ${humidityState.value ?: "-"} %")
                        }
                    }
                )
            }
        }
    }

    // Kullanıcı izin sonuçları burada işleniyor
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Log.d(TAG, "Permissions granted via dialog.")
        } else {
            Log.e(TAG, "User denied permissions.")
        }
    }

    // BLE cihazlarını aramaya başla
    private fun startBLEScan(foundDevices: MutableList<String>) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Scan permission not granted")
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        try {
            bluetoothLeScanner?.startScan(object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)
                    val name = result.device.name
                    if (name != null && !foundDevices.contains(name)) {
                        foundDevices.add(name) // Yeni cihaz listeye eklenir
                        Log.d(TAG, "Device found: $name")
                    }
                    // Eğer IPVSWeather cihazı bulunduysa bağlan
                    if (name == "IPVSWeather") {
                        bluetoothLeScanner?.stopScan(this)
                        connectToDevice(result.device)
                    }
                }
            })
            Log.d(TAG, "Scan started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission for startScan: ${e.message}")
        }
    }

    // Cihaza bağlan
    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Connect permission not granted")
            return
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        Log.d(TAG, "Connecting to GATT server...")
    }

    // GATT olaylarını yöneten geri çağırma nesnesi
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.")
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices() // Servisleri keşfet
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(WEATHER_SERVICE_UUID)
                if (service != null) {
                    val tempChar = service.getCharacteristic(TEMPERATURE_UUID)
                    val humChar = service.getCharacteristic(HUMIDITY_UUID)

                    if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        // Sıcaklık karakteristiğini dinlemeye başla
                        if (tempChar != null) {
                            gatt.setCharacteristicNotification(tempChar, true)
                            val tempDescriptor = tempChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            tempDescriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(tempDescriptor)
                        }
                        // Nem karakteristiğini bir defaya mahsus oku
                        if (humChar != null) {
                            gatt.readCharacteristic(humChar)
                            Log.d(TAG, "Read humidity characteristic once after connection")
                        }
                    }
                } else {
                    Log.e(TAG, "Weather service not found")
                }
            } else {
                Log.e(TAG, "Service discovery failed, status: $status")
            }
        }

        // Karakteristik okunduğunda çağrılır
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicUpdate(characteristic)
            }
        }

        // Karakteristik değeri değiştiğinde (bildirim geldiğinde) çağrılır
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handleCharacteristicUpdate(characteristic)
        }

        // Karakteristik verilerini çözümle
        private fun handleCharacteristicUpdate(characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == TEMPERATURE_UUID) {
                val raw = characteristic.value
                Log.d(TAG, "Raw temperature bytes: ${raw.joinToString()}")

                if (raw != null && raw.size >= 5) {
                    // Byte 1 ve 2 sıcaklık (LSB + MSB), 0.01 hassasiyet
                    val tempRaw = ((raw[2].toInt() and 0xFF) shl 8) or (raw[1].toInt() and 0xFF)
                    val tempCelsius = tempRaw / 100.0
                    temperatureState.value = tempCelsius.toInt()
                    Log.d(TAG, "Parsed temperature: $tempCelsius °C")

                    // Byte 3 ve 4 nem (LSB + MSB), 0.1 hassasiyet
                    val humidityRaw = ((raw[3].toInt() and 0xFF) shl 8) or (raw[4].toInt() and 0xFF)
                    val humidity = (humidityRaw / 10.0).coerceIn(0.0, 100.0)
                    humidityState.value = humidity.roundToInt()
                    Log.d(TAG, "Parsed humidity: $humidity %")
                } else {
                    Log.e(TAG, "Geçersiz sıcaklık/nem verisi")
                }
            }
        }
    }
}
