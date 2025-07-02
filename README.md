# 🌤️ Weather BLE App

**Weather BLE App** is an Android application built with Jetpack Compose that scans for nearby Bluetooth Low Energy (BLE) devices and reads temperature and humidity data from a custom weather beacon ("IPVSWeather").

## 🚀 Features

- BLE scanning using Android's BluetoothLeScanner
- Automatic connection to the device named **"IPVSWeather"**
- Reads and parses temperature and humidity from GATT characteristics
- Real-time UI updates using Jetpack Compose
- Written entirely in Kotlin

## 🛠️ Technologies Used

- **Kotlin**
- **Jetpack Compose**
- **Bluetooth Low Energy (BLE)**
- **GATT Protocol**
- **Android 12+ SDK**

## 📦 Requirements

- Android device with BLE support
- Android 12 or higher
- Location & Bluetooth permissions enabled
- BLE device broadcasting with:
  - `WEATHER_SERVICE_UUID = 00000002-0000-0000-FDFD-FDFDFDFDFDFD`
  - `TEMPERATURE_UUID = 00002A1C-0000-1000-8000-00805f9b34fb`
  - `HUMIDITY_UUID = 00002A6F-0000-1000-8000-00805f9b34fb`

