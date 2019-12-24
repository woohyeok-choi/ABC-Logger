package kaist.iclab.abclogger.collector

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kaist.iclab.abclogger.BluetoothEntity
import kaist.iclab.abclogger.fillBaseInfo
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class BluetoothCollector(val context: Context) : BaseCollector {
    private val bluetoothAdapter: BluetoothAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }

    private val discoveredBLEDevices: HashSet<String> = hashSetOf()

    private val isBLEScanning: AtomicBoolean = AtomicBoolean(false)

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
                        timestamp = System.currentTimeMillis()
                ).run { putEntity(this) }
            }
        }
    }

    private suspend fun requestBLEScan() = withContext(Dispatchers.IO) {
        if(isBLEScanning.get()) return@withContext
        isBLEScanning.set(true)

        val scanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

        scanner.startScan(scanCallback)
        delay(5000)
        scanner.stopScan(scanCallback)

        discoveredBLEDevices.clear()
        isBLEScanning.set(false)
    }

    private val receiver : BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when(intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val timestamp = System.currentTimeMillis()
                        val extras = intent.extras ?: return
                        val device = extras.getParcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                        val rssi = extras.getShort(BluetoothDevice.EXTRA_RSSI).toInt()

                        BluetoothEntity(
                                deviceName = device.name,
                                address = device.address,
                                rssi = rssi
                        ).fillBaseInfo(
                                timestamp = timestamp
                        ).run { putEntity(this) }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        GlobalScope.launch { requestBLEScan() }
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                                BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF
                        ) == BluetoothAdapter.STATE_OFF
                        if (state) {

                        } else {

                            bluetoothAdapter.startDiscovery()
                        }
                    }
                    ACTION_BLUETOOTH_SCAN -> {
                        bluetoothAdapter.startDiscovery()
                    }
                }
            }
        }
    }

    override fun start() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun checkAvailability(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRequiredPermissions(): List<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun newIntentForSetup(): Intent? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        private const val ACTION_BLUETOOTH_SCAN = "kaist.iclab.abclogger.ACTION_BLUETOOTH_SCAN"
        private const val REQUEST_CODE_BLUETOOTH_SCAN = 0xdd
    }
}