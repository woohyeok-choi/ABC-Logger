package kaist.iclab.abclogger.background.collector

import android.Manifest
import androidx.lifecycle.MutableLiveData
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.background.Status
import kaist.iclab.abclogger.common.util.PermissionUtils
import kaist.iclab.abclogger.common.util.Utils
import kaist.iclab.abclogger.data.entities.BluetoothDeviceEntity
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BluetoothCollector(val context: Context) : BaseCollector {
    private var scheduledFuture: ScheduledFuture<*>? = null

    private lateinit var uuid: String
    private lateinit var group: String
    private lateinit var email: String
    private var isCollecting = false

    override fun stopCollection() {
        if(!isCollecting) return

        isCollecting = false
        bluetoothScanner.stopScan(bleScanCallback)
        scheduledFuture?.cancel(true)

        status.postValue(Status.CANCELED)
    }

    override fun startCollection(uuid: String, group: String, email: String) {
        if(isCollecting) return
        isCollecting = true

        status.postValue(Status.STARTED)

        try {
            this.uuid = uuid
            this.group = group
            this.email = email

            collectBleDevice(MIN_PERIOD_BLUETOOTH_IN_MS)

            status.postValue(Status.RUNNING)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is SecurityException) {
                stopCollection()
                status.postValue(Status.ABORTED(e))
            }
        }
    }

    private fun collectBleDevice(bluetoothPeriod: Long) {

        scheduledFuture?.cancel(true)
        scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            try {
                if(!checkEnableToCollect(context)) throw SecurityException("Bluetooth is not granted to be collected.")

                bluetoothScanner.startScan(bleScanCallback)
                status.postValue(Status.RUNNING)
            } catch (e: Exception) {
                e.printStackTrace()
                if(e is SecurityException) {
                    stopCollection()
                    status.postValue(Status.ABORTED(e))
                }
            }
        }, 5000, bluetoothPeriod, TimeUnit.MILLISECONDS)
    }

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            try {
                if (result != null && isCollecting) {
                    val entity = extractBluetoothDeviceEntity(result)
                    App.boxFor<BluetoothDeviceEntity>().put(entity)

                    Log.d(TAG, "Box.put(" +
                            "timestamp = ${entity.timestamp}, subjectEmail = ${entity.subjectEmail}, experimentUuid = ${entity.experimentUuid}, " +
                            "experimentGroup = ${entity.experimentGroup}, entity = $entity)")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "onScanFailed: $errorCode")
            stopCollection()
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d(TAG, "onBatchScanResults: ${results.toString()}")
        }
    }

    private fun extractBluetoothDeviceEntity(result: ScanResult) : BluetoothDeviceEntity {
        val now = System.currentTimeMillis()

        val scanRecord = result.scanRecord

        var txPower = Int.MIN_VALUE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            txPower = result.txPower
        }

        return BluetoothDeviceEntity(
                name = result.device.name?: "",
                deviceName = scanRecord?.deviceName?: "",
                address = result.device.address,
                serviceUuids = scanRecord?.serviceUuids.toString(),
                serviceData = scanRecord?.serviceData.toString(),
                manufacturerSpecificData = scanRecord?.manufacturerSpecificData.toString(),
                rssi = result.rssi,
                txPower = txPower,
                txPowerLevel = scanRecord?.txPowerLevel ?: Int.MIN_VALUE
        ).apply {
            timestamp = now
            utcOffset = Utils.utcOffsetInHour()
            subjectEmail = email
            experimentGroup = group
            experimentUuid = uuid
            isUploaded = false
        }
    }


    companion object {
        private val TAG: String = BluetoothCollector::class.java.simpleName

        private const val MIN_PERIOD_BLUETOOTH_IN_MS = 1000 * 15L
        private const val MAX_PERIOD_BLUETOOTH_IN_MS = 1000 * 60 * 1L

        private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        private val bluetoothScanner = bluetoothAdapter.bluetoothLeScanner

        fun checkEnableToCollect(context: Context) = PermissionUtils.checkPermissionAtRuntime(context, REQUIRED_PERMISSIONS)

        val REQUIRED_PERMISSIONS = listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

        val status = MutableLiveData<Status>().apply {
            postValue(Status.CANCELED)
        }
    }
}