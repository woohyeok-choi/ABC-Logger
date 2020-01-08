package kaist.iclab.abclogger.collector

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.MutableLiveData
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseCollector
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class BluetoothCollector(val context: Context) : BaseCollector {
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val scanCallback: ScanCallback by lazy {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                val address = result?.device?.address ?: return
                if (address in discoveredBLEDevices) return

                discoveredBLEDevices.add(address)

                BluetoothEntity(
                        deviceName = result.device.name ?: "",
                        address = address,
                        rssi = result.rssi
                ).fillBaseInfo(
                        timeMillis = System.currentTimeMillis()
                ).run { putEntity(this) }
            }
        }
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> handleActionFound(intent)
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> handleBluetoothDiscovered()
                    ACTION_BLUETOOTH_SCAN -> handleBluetoothScanRequest()
                }
            }
        }
    }

    private val filter = IntentFilter().apply {
        addAction(ACTION_BLUETOOTH_SCAN)
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    }

    private val intent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_BLUETOOTH_SCAN,
            Intent(ACTION_BLUETOOTH_SCAN), PendingIntent.FLAG_UPDATE_CURRENT
    )

    private val discoveredBLEDevices: HashSet<String> = hashSetOf()
    private val isBLEScanning: AtomicBoolean = AtomicBoolean(false)


    private fun handleBluetoothDiscovered() = GlobalScope.launch(Dispatchers.IO) {
        if (isBLEScanning.get()) return@launch

        isBLEScanning.set(true)

        val scanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

        scanner.startScan(scanCallback)
        delay(5000)
        scanner.stopScan(scanCallback)

        discoveredBLEDevices.clear()
        isBLEScanning.set(false)
    }

    private fun handleActionFound(intent: Intent) {
        val timestamp = System.currentTimeMillis()
        val extras = intent.extras ?: return
        val device = extras.getParcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        val rssi = extras.getShort(BluetoothDevice.EXTRA_RSSI).toInt()

        BluetoothEntity(
                deviceName = device.name,
                address = device.address,
                rssi = rssi
        ).fillBaseInfo(
                timeMillis = timestamp
        ).run { putEntity(this) }
    }

    private fun handleBluetoothScanRequest() {
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()

        if (!bluetoothAdapter.isDiscovering && bluetoothAdapter.isEnabled) {
            bluetoothAdapter.startDiscovery()
        }
    }

    override fun onStart() {
        SharedPrefs.isProvidedBluetooth = true

        context.registerReceiver(receiver, filter)

        val curTime = System.currentTimeMillis()

        alarmManager.cancel(intent)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, curTime + 5000, 5 * 60 * 1000, intent)
    }

    override fun onStop() {
        SharedPrefs.isProvidedBluetooth
        context.unregisterReceiver(receiver)

        alarmManager.cancel(intent)
    }

    override fun checkAvailability(): Boolean =
            bluetoothAdapter.isEnabled && Utils.checkPermissionAtRuntime(context, requiredPermissions)

    override fun handleActivityResult(resultCode: Int, intent: Intent?) { }

    override val requiredPermissions: List<String>
        get() = listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )

    override val newIntentForSetUp: Intent?
        get() = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    override val nameRes: Int?
        get() = R.string.data_name_bluetooth

    override val descriptionRes: Int?
        get() = R.string.data_desc_bluetooth

    companion object {
        private const val ACTION_BLUETOOTH_SCAN = "${BuildConfig.APPLICATION_ID}.ACTION_BLUETOOTH_SCAN"
        private const val REQUEST_CODE_BLUETOOTH_SCAN = 0xdd
    }
}
