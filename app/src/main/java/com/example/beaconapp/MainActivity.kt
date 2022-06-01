package com.example.beaconapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.ContentValues
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_main.*
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import java.util.*


const val DEVICE_MAC_ADDRESS = "F8:20:74:F7:2B:82"
const val SERVICE_UUID = "0000feaa-0000-1000-8000-00805f9b34fb"

class MainActivity : AppCompatActivity() {

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private  var  bluetoothLeScanner: BluetoothLeScanner? = null
    private var beaconManager: BeaconManager? = null
    var bluetoothGatt: BluetoothGatt? = null

    var scanning = false
    val handler = Handler()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)

        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e(ContentValues.TAG, "Unable to initialize BluetoothManager.")
            }
        }

        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            Log.e(ContentValues.TAG, "Unable to obtain a BluetoothAdapter.")
        }
        if (mBluetoothAdapter != null) {
            bluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner

            if (bluetoothLeScanner == null) {
                Log.e(ContentValues.TAG, "Unable to obtain a BluetoothAdapter.")
            }
        }

        beaconManager =  BeaconManager.getInstanceForApplication(this)

        scanLeDevice()

        update_data.setOnClickListener{
            print("erisa")
        }

    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner!!.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true

            val filters: MutableList<ScanFilter> = ArrayList()
            val filter = ScanFilter.Builder()
                .setDeviceAddress(DEVICE_MAC_ADDRESS)
                .build()
            filters.add(filter)
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            bluetoothLeScanner!!.startScan(filters, settings, leScanCallback)

        } else {
            scanning = false
            bluetoothLeScanner!!.stopScan(leScanCallback)
        }
        invalidateOptionsMenu()
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.i(ContentValues.TAG, "Hello $result")

            val scanRecord = result.scanRecord
            val beacon = Beacon(result.device.address)
            beacon.manufacturer = result.device.name
            beacon.rssi = result.rssi
            if (scanRecord != null) {

                val serviceUuids = scanRecord.serviceUuids
                if (serviceUuids != null && serviceUuids.size > 0 && serviceUuids.contains(
                        ParcelUuid.fromString(SERVICE_UUID)
                    )
                ) {
                    val serviceData = scanRecord.getServiceData(ParcelUuid.fromString(SERVICE_UUID))
                    if (serviceData != null && serviceData.size > 18) {
                        val eddystoneUUID =
                            toHexString(Arrays.copyOfRange(serviceData, 2, 18))
                        val namespace = String(eddystoneUUID.toCharArray().sliceArray(0..19))
                        val instance = String(
                            eddystoneUUID.toCharArray()
                                .sliceArray(20 until eddystoneUUID.toCharArray().size)
                        )
                        beacon.type = Beacon.beaconType.eddystoneUID
                        beacon.namespace = namespace
                        beacon.instance = instance

                        beacon_id.text = namespace + instance
                    }
                }

            }

            beaconManager?.getBeaconParsers()?.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT))
            
//            beaconSet.add(beacon)
//            (recyclerView.adapter as BeaconsAdapter).updateData(beaconSet.toList(),beaconTypePositionSelected)
        }

        }


    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("HEY", " GATT Successfully Connected ")
                gatt!!.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                Log.i("HEY", " GATT Successfully disconnected")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
//            temperatureCharacteristic = gatt?.getService(UUID.fromString(DEVICE_UIID))
//                ?.getCharacteristic(UUID.fromString(TEMP_CHARACTERISTIC))
//            humidityCharacteristic = gatt?.getService(UUID.fromString(DEVICE_UIID))
//                ?.getCharacteristic(UUID.fromString(HUMIDITY_CHARACTERISTIC))
//
//            enableButtons()

        }

        @SuppressLint("SetTextI18n")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (characteristic != null) {
//                if(characteristic.uuid.toString() == TEMP_CHARACTERISTIC){
//                    var result = characteristic.getFloatValue(52,1)
//
//                    tv_temp_value.text = "$result°C";
//                }
//                else if(characteristic.uuid.toString() == HUMIDITY_CHARACTERISTIC){
//
//                    val result = characteristic.getIntValue(18,0)
//
//                    var degrees = result / 100
//                    var points = result % 100
//
//                    tv_humidity_value.text = "$degrees.$points%";
//                }
            }


        }


    }

    private val HEX = "0123456789ABCDEF".toCharArray()
    fun toHexString(bytes: ByteArray): String {
        if (bytes.isEmpty()) {
            return ""
        }
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = (bytes[j].toInt() and 0xFF)
            hexChars[j * 2] = HEX[v ushr 4]
            hexChars[j * 2 + 1] = HEX[v and 0x0F]
        }
        return String(hexChars)
    }

}
