package com.example.beaconapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.ContentValues
import android.content.Context
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.altbeacon.beacon.*
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor
import java.util.*


const val DEVICE_MAC_ADDRESS = "F8:20:74:F7:2B:82"
const val SERVICE_UUID = "0000feaa-0000-1000-8000-00805f9b34fb"

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), BeaconConsumer, RangeNotifier {

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
//        val region = Region("all-beacons-region", null, null, null)
        beaconManager?.getBeaconParsers()!!
            .add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT))
        beaconManager?.getBeaconParsers()!!
            .add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT))
        beaconManager?.bind(this)

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

    override fun onBeaconServiceConnect() {
        val region = Region("all-beacons-region", null, null, null)
        try {
            beaconManager?.startRangingBeaconsInRegion(region)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        beaconManager?.setRangeNotifier(this)
    }

    override fun didRangeBeaconsInRegion(
        beacons: MutableCollection<org.altbeacon.beacon.Beacon>?,
        region: Region?
    ) {

        for (beacon in beacons!!) {
            if (beacon.serviceUuid === 0xfeaa && beacon.beaconTypeCode === 0x10) {
                // This is a Eddystone-URL frame
                val url = UrlBeaconUrlCompressor.uncompress(beacon.id1.toByteArray())
                beacon_url.text = "URL: " + url
                beacon_voltage.text = "Voltage: " + beacon.extraDataFields.get(1)
//                var result = characteristic.getFloatValue(52,1)
//
//                tv_temp_value.text = "$result°C";
                beacon_temperature.text = "Temp: " + beacon.extraDataFields.get(2)
                beacon_distance.text = "Dist: " + beacon.distance

                for (kot in beacon.extraDataFields){
                    Log.i("Kot", "$kot")
                }

                Log.i(
                    "TAG", "I see a beacon transmitting a url: " + url +
                            " approximately " + beacon.distance + " meters away."
                )
            }

            if (beacon.serviceUuid === 0xfeaa && beacon.beaconTypeCode === 0x00) {
                // This is a Eddystone-UID frame
                val namespaceId = beacon.id1
                val instanceId = beacon.id2
                Log.d(
                    "TAG", "I see a beacon transmitting namespace id: " + namespaceId +
                            " and instance id: " + instanceId +
                            " approximately " + beacon.distance + " meters away."
                )

                // Do we have telemetry data?
                if (beacon.extraDataFields.size > 0) {
                    val telemetryVersion = beacon.extraDataFields[0]
                    val batteryMilliVolts = beacon.extraDataFields[1]
                    val pduCount = beacon.extraDataFields[3]
                    val uptime = beacon.extraDataFields[4]
                    beacon_voltage.text = "voltage" + batteryMilliVolts
                    Log.i(
                        "TAG", "The above beacon is sending telemetry version " + telemetryVersion +
                                ", has been up for : " + uptime + " seconds" +
                                ", has a battery level of " + batteryMilliVolts + " mV" +
                                ", and has transmitted " + pduCount + " advertisements."
                    )
                }
            }
        }
    }

}
