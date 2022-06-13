package com.example.beaconapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.altbeacon.beacon.*
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor


const val DEVICE_MAC_ADDRESS = "F8:20:74:F7:2B:82"
const val SERVICE_UUID = "0000feaa-0000-1000-8000-00805f9b34fb"

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), BeaconConsumer, RangeNotifier {

    private var beaconManager: BeaconManager? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)

        beaconManager =  BeaconManager.getInstanceForApplication(this)
//        val region = Region("all-beacons-region", null, null, null)

        // Detect the main identifier (UID) frame:
        beaconManager?.getBeaconParsers()!!
            .add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT))

        // Detect the URL frame:
        beaconManager?.getBeaconParsers()!!
            .add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT))

        // Detect the telemetry (TLM) frame:
        beaconManager?.getBeaconParsers()!!
            .add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT))
        beaconManager?.bind(this)


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
                beacon_url.text = url
            }

            if (beacon.serviceUuid === 0xfeaa && beacon.beaconTypeCode === 0x00) {
                // This is a Eddystone-UID frame
                val namespaceId = beacon.id1
                val instanceId = beacon.id2

                beacon_instanceId.text = "InstanceId: " + instanceId.toString()
                beacon_namespaceId.text = "NamespaceId:" + namespaceId.toString()

                beacon_voltage.text = beacon.extraDataFields.get(1).toString() + " V"
                beacon_distance.text = beacon.distance.toString() + " metres"
                val unsignedTemp = beacon.extraDataFields[2] shr 8
                val temperature =
                    if (unsignedTemp > 128) (unsignedTemp - 256).toDouble() else unsignedTemp + (beacon.extraDataFields[2] and 0xff) / 256.0
                beacon_temperature.text = temperature.toString() + "°C"

            }
        }
    }

}
