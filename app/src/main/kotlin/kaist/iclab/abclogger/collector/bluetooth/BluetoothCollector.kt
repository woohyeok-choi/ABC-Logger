package kaist.iclab.abclogger.collector.bluetooth

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.collector.stringifyBluetoothClass
import kaist.iclab.abclogger.collector.stringifyBluetoothDeviceBondState
import kaist.iclab.abclogger.collector.stringifyBluetoothDeviceType
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.Description
import kaist.iclab.abclogger.core.collector.with
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Bluetooth Collector operates every T minutes as follows:
 * 1. BluetoothAdapter.startDiscovery()
 * 1.2. BluetoothDevice.ACTION_FOUND -> write device profiles
 * 2. BluetoothLeScanner.startScan() (via ScanCallback or PendingIntent) when BluetoothAdapter.ACTION_DISCOVERY_FINISHED is received
 * 3. BluetoothLeScanner.stopScan() after 10 seconds
 *
 */
class BluetoothCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<BluetoothEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    override val permissions: List<String> = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override val setupIntent: Intent? = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    private val discoveredLeDevices: ConcurrentHashMap<String, BluetoothEntity> = ConcurrentHashMap()

    private val alarmManager by lazy {
        context.getSystemService<AlarmManager>()!!
    }

    private val intentScanRequest by lazy {
        PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BLUETOOTH_SCAN_REQUEST,
                Intent(ACTION_BLUETOOTH_SCAN_REQUEST),
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val intentLeDeviceFound by lazy {
        PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BLUETOOTH_LE_FOUND,
                Intent(ACTION_BLUETOOTH_LE_FOUND),
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            handleLeDeviceFound(result)
        }
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_BLUETOOTH_SCAN_REQUEST -> handleScanRequest()
                BluetoothDevice.ACTION_FOUND -> handleDeviceFound(intent)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> handleLeScanRequest()
                ACTION_BLUETOOTH_LE_FOUND -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.getParcelableArrayListExtra<ScanResult>(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)?.forEach {
                        handleLeDeviceFound(it)
                    }
                }
            }
        }
    }

    override fun isAvailable(): Boolean = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true

    override fun getDescription(): Array<Description> = arrayOf(
                    R.string.collector_bluetooth_info_ble with
                    if(BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner != null) {
                        context.getString(R.string.collector_bluetooth_info_ble_supported)
                    } else {
                        context.getString(R.string.collector_bluetooth_info_ble_none)
                    }
            )


    override suspend fun onStart() {
        val filter = IntentFilter().apply {
            addAction(ACTION_BLUETOOTH_SCAN_REQUEST)
            addAction(ACTION_BLUETOOTH_LE_FOUND)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        context.safeRegisterReceiver(receiver, filter)
        handleScanRequest()
    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)
        alarmManager.cancel(intentScanRequest)
    }

    override suspend fun count(): Long = dataRepository.count<BluetoothEntity>()

    override suspend fun flush(entities: Collection<BluetoothEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<BluetoothEntity> = dataRepository.find(0, limit)

    private fun handleScanRequest() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            handleLeScanRequest()
        } else {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            if (!adapter.isDiscovering && adapter.isEnabled) adapter.startDiscovery()
        }

        AlarmManagerCompat.setAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10),
                intentScanRequest
        )
    }

    private fun handleDeviceFound(intent: Intent) = launch {
        val extras = intent.extras ?: return@launch
        val rssi = extras.getShort(BluetoothDevice.EXTRA_RSSI, 0).toInt()
        val device = extras.getParcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                ?: return@launch
        val entity = buildEntity(device, rssi, false).apply {
            timestamp = System.currentTimeMillis()
        }
        put(entity)
    }

    private fun handleLeScanRequest() = launch {
        discoveredLeDevices.clear()

        val leScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner ?: return@launch

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            leScanner.startScan(null, null, intentLeDeviceFound)
        } else {
            leScanner.startScan(scanCallback)
        }

        delay(10 * 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            leScanner.stopScan(intentLeDeviceFound)
        } else {
            leScanner.stopScan(scanCallback)
        }

        discoveredLeDevices.values.forEach {
            put(it)
        }
    }

    private fun handleLeDeviceFound(scanResult: ScanResult) {
        discoveredLeDevices[scanResult.device.address] = buildEntity(scanResult.device, scanResult.rssi, true).apply {
            timestamp = System.currentTimeMillis()
        }
    }

    private fun buildEntity(device: BluetoothDevice, rssi: Int, isLowEnergy: Boolean) =
            BluetoothEntity(
                    name = device.name ?: "UNKNOWN",
                    address = device.address ?: "UNKNOWN",
                    alias = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) device.alias else null)
                            ?: "UNKNOWN",
                    rssi = rssi,
                    bondState = stringifyBluetoothDeviceBondState(device.bondState),
                    deviceType = stringifyBluetoothDeviceType(device.type),
                    classType = stringifyBluetoothClass(device.bluetoothClass.deviceClass),
                    isLowEnergy = isLowEnergy
            )

    companion object {
        private const val ACTION_BLUETOOTH_SCAN_REQUEST = "${BuildConfig.APPLICATION_ID}.ACTION_BLUETOOTH_SCAN_REQUEST"
        private const val ACTION_BLUETOOTH_LE_FOUND = "${BuildConfig.APPLICATION_ID}.ACTION_BLUETOOTH_LE_FOUND"
        private const val REQUEST_CODE_BLUETOOTH_SCAN_REQUEST = 0xdd
        private const val REQUEST_CODE_BLUETOOTH_LE_FOUND = 0xde
    }
}
