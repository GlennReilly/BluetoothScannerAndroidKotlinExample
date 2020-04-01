package com.glennreilly.nearbybluetoothlogger

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.joda.time.DateTime
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity::class.java.simpleName
        const val REQUEST_PERMISSION_MULTIPLE = 0
        const val REQUEST_PERMISSION_BLUETOOTH = 1
        const val REQUEST_PERMISSION_BLUETOOTH_ADMIN = 2
        const val REQUEST_PERMISSION_CAMERA = 3
    }

    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            AlertDialog.Builder(this)
                .setTitle("Sorry...")
                .setMessage("Your device doesn't appear to support Bluetooth")
                .setPositiveButton("Ok") { _, _ -> exitProcess(0) }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        } else {
            if (!bluetoothAdapter.isEnabled) {
                startActivityForResult(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQUEST_PERMISSION_BLUETOOTH
                )
            } else { // bluetooth exists and is enabled
                val pairedDevices: MutableSet<BluetoothDevice> = bluetoothAdapter.bondedDevices
                pairedDevices.forEach {
                    displayDeviceDetails(DeviceItem(it.name, it.address, true))
                }

                broadcastReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val action = intent?.action
                        if (BluetoothDevice.ACTION_FOUND == action) {
                            val device: BluetoothDevice? =
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                            device?.let {
                                displayDeviceDetails(DeviceItem(it.name, it.address, false))
                            }
                        }
                    }
                }

                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                this.registerReceiver(broadcastReceiver, filter)
            }
        }
    }

    private fun displayDeviceDetails(device: DeviceItem) {
        val connectedMessage = if (device.isConnected) "Connected" else "Disconnected"
        val displayString =
            "$connectedMessage Bluetooth device found:\n ${device.name} \n address: ${device.address} \n at: ${device.dateTimeFound}."
        Log.i(TAG, displayString)
        //println(displayString)
    }

    override fun onStop() {
        if (this::broadcastReceiver.isInitialized) {
            try {
                this.unregisterReceiver(broadcastReceiver)
            } catch (e: IllegalArgumentException) {
                // broadcastReceiver already unregistered
            }
        }
        super.onStop()
    }

    private fun checkPermissions(): Boolean {
        val permissionsRequired =
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.CAMERA
            )

        val permissionsStillRequired = mutableListOf<String>()

        permissionsRequired.forEach { requiredPermission ->
            if (ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    requiredPermission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.i(TAG, "Device version above 6.0 - Requesting location permissions.")

                    permissionsStillRequired.add(requiredPermission)
                }

            }
        }

        val x = permissionsStillRequired.map { it as String }.toTypedArray()
        val z = permissionsStillRequired.toTypedArray()

        if (permissionsStillRequired.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsRequired,
                REQUEST_PERMISSION_MULTIPLE
            )
            return false
        }

/*        if (permissionsStillRequired.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_PERMISSION_CAMERA
                //REQUEST_PERMISSION_MULTIPLE
            )
            return false
        }*/
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION_BLUETOOTH -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    println(" permission IS granted")
                } else {
                    println(" permission NOT granted")
                }
            }
            REQUEST_PERMISSION_BLUETOOTH_ADMIN -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    println(" permission IS granted")
                } else {
                    println(" permission NOT granted")
                }
            }
            REQUEST_PERMISSION_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    println(" permission IS granted")
                } else {
                    println(" permission NOT granted")
                }
            }
        }
    }
}

data class DeviceItem(
    val name: String?,
    val address: String?,
    var isConnected: Boolean,
    val dateTimeFound: DateTime = DateTime()
)
