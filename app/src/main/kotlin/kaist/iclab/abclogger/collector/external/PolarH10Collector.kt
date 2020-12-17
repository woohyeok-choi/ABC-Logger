package kaist.iclab.abclogger.collector.external

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.AlarmManagerCompat
import io.reactivex.disposables.Disposable
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.ui.settings.polar.PolarH10SettingActivity
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.core.collector.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.errors.PolarDeviceDisconnected
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarHrData
import java.util.concurrent.TimeUnit

class PolarH10Collector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractExternalSensorCollector(
    context,
    qualifiedName,
    name,
    description,
    dataRepository,
    DEVICE_TYPE
) {
    var deviceId by ReadWriteStatusString("")

    var deviceConnectionStatus by ReadWriteStatusString("")
        private set
    var deviceBatteryLevel by ReadWriteStatusInt(Int.MIN_VALUE)
        private set
    var deviceRssi by ReadWriteStatusInt(Int.MIN_VALUE)
        private set
    var deviceName by ReadWriteStatusString("")
        private set
    var deviceAddress by ReadWriteStatusString("")
        private set

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val intent by lazy {
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CHECK_CONNECTION_STATUS,
            Intent(ACTION_CHECK_CONNECTION_STATUS),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(javaClass, "${intent?.action}")

            launch {
                if (isEnabled) throw PolarDeviceDisconnected()
            }
        }
    }

    open class Callback {
        open fun onConnectionStateChanged(
            identifier: String,
            name: String,
            address: String,
            rssi: Int,
            state: String
        ) {
        }

        open fun onBatteryChanged(
            identifier: String,
            level: Int
        ) {
        }

        open fun onHeartRateReceived(
            identifier: String,
            heartRate: Int,
            rrAvailable: Boolean,
            rrIntervalInSec: List<Int>,
            rrIntervalInMillis: List<Int>,
            contactStatusSupported: Boolean,
            contactStatus: Boolean
        ) {
        }

        open fun onEcgChanged(identifier: String, samples: List<Int>) {}
        open fun onAccelerometerChanged(identifier: String, samples: List<Triple<Int, Int, Int>>) {}
        open fun onError(identifier: String, throwable: Throwable) {}
    }

    class Api(val context: Context, val callback: Callback) {
        private val api = PolarBleApiDefaultImpl.defaultImplementation(
            context,
            PolarBleApi.FEATURE_HR or
                    PolarBleApi.FEATURE_BATTERY_INFO or
                    PolarBleApi.FEATURE_DEVICE_INFO or
                    PolarBleApi.FEATURE_POLAR_SENSOR_STREAMING
        ).apply {
            setPolarFilter(false)
            setAutomaticReconnection(true)
        }

        private var ecgDisposable: Disposable? = null

        private var accDisposable: Disposable? = null

        private val apiCallback = object : PolarBleApiCallback() {
            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                callback.onConnectionStateChanged(
                    identifier = polarDeviceInfo.deviceId ?: "",
                    name = polarDeviceInfo.name ?: "",
                    address = polarDeviceInfo.address ?: "",
                    rssi = polarDeviceInfo.rssi,
                    state = CONNECTED
                )
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                callback.onConnectionStateChanged(
                    identifier = polarDeviceInfo.deviceId ?: "",
                    name = polarDeviceInfo.name ?: "",
                    address = polarDeviceInfo.address ?: "",
                    rssi = polarDeviceInfo.rssi,
                    state = CONNECTING
                )
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                callback.onConnectionStateChanged(
                    identifier = polarDeviceInfo.deviceId ?: "",
                    name = polarDeviceInfo.name ?: "",
                    address = polarDeviceInfo.address ?: "",
                    rssi = polarDeviceInfo.rssi,
                    state = DISCONNECTED
                )
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                callback.onBatteryChanged(
                    identifier = identifier,
                    level = level
                )
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                callback.onHeartRateReceived(
                    identifier = identifier,
                    heartRate = data.hr,
                    rrAvailable = data.rrAvailable,
                    rrIntervalInSec = data.rrs,
                    rrIntervalInMillis = data.rrsMs,    // @swkang: strange definition.
                    contactStatus = data.contactStatus,
                    contactStatusSupported = data.contactStatus
                )
            }

            override fun ecgFeatureReady(identifier: String) {
                if (ecgDisposable?.isDisposed == false) return

                ecgDisposable = api.requestEcgSettings(identifier).toFlowable().flatMap { setting ->
                    val maxSetting = setting.maxSettings() ?: throw PolarError.noSetting()
                    api.startEcgStreaming(identifier, maxSetting)
                }.subscribe({
                    callback.onEcgChanged(identifier, it.samples)
                }, {
                    callback.onError(identifier, it)
                })
            }

            override fun accelerometerFeatureReady(identifier: String) {
                if (accDisposable?.isDisposed == false) return

                accDisposable = api.requestAccSettings(identifier).toFlowable().flatMap { setting ->
                    val maxSetting = setting.maxSettings() ?: throw PolarError.noSetting()
                    api.startAccStreaming(identifier, maxSetting)
                }.map { data ->
                    data.samples.map { Triple(it.x, it.y, it.z) }
                }.subscribe({
                    callback.onAccelerometerChanged(identifier, it)
                }, {
                    callback.onError(identifier, it)
                })
            }
        }

        fun connect(deviceId: String) {
            api.setApiCallback(apiCallback)
            api.connectToDevice(deviceId)
        }

        fun disconnect(deviceId: String) {
            api.stopRecording(deviceId)

            ecgDisposable?.dispose()
            accDisposable?.dispose()
            ecgDisposable = null
            accDisposable = null

            api.disconnectFromDevice(deviceId)
            //api.cleanup()
            //api.shutDown()
            api.setApiCallback(null)      // check needed.
        }
    }

    private val buffer: Subject<ExternalSensorEntity> = PublishSubject.create()

    fun getPolarApi(context: Context, callback: Callback) = Api(context, callback)

    private val defaultCallback = object : Callback() {
        override fun onConnectionStateChanged(
            identifier: String,
            name: String,
            address: String,
            rssi: Int,
            state: String
        ) {
            deviceConnectionStatus = state

            if (state == CONNECTED) {
                deviceName = name
                deviceAddress = address
                deviceRssi = rssi
            } else {
                deviceName = ""
                deviceAddress = ""
                deviceRssi = 0
            }

            if (state != CONNECTED) {
                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1),
                    intent
                )
            } else {
                alarmManager.cancel(intent)
            }
        }

        override fun onBatteryChanged(identifier: String, level: Int) {
            deviceBatteryLevel = level
        }

        override fun onHeartRateReceived(
            identifier: String,
            heartRate: Int,
            rrAvailable: Boolean,
            rrIntervalInSec: List<Int>,
            rrIntervalInMillis: List<Int>,
            contactStatusSupported: Boolean,
            contactStatus: Boolean
        ) {
            val timestamp = System.currentTimeMillis()

            val hrEntity = ExternalSensorEntity(
                deviceType = DEVICE_TYPE,
                valueType = "HR",
                identifier = identifier,
                others = mapOf(
                    "contactStatus" to contactStatus.toString(),
                    "contactStatusSupported" to contactStatusSupported.toString(),
                    "rrAvailable" to rrAvailable.toString()
                ),
                valueFormat = "INT",
                valueUnit = "BPM",
                values = listOf(heartRate.toString())
            ).apply {
                this.timestamp = timestamp
            }
            buffer.onNext(hrEntity)

            if (!rrAvailable) return

            val rrIntervalSecEntity = ExternalSensorEntity(
                deviceType = DEVICE_TYPE,
                valueType = "RR-Interval-1/1024-Second",
                identifier = identifier,
                others = mapOf(
                    "contactStatus" to contactStatus.toString(),
                    "contactStatusSupported" to contactStatusSupported.toString(),
                    "rrAvailable" to rrAvailable.toString()
                ),
                valueFormat = "INT",
                valueUnit = "1/1024 SEC",
                values = rrIntervalInSec.map { it.toString() }
            ).apply{
                this.timestamp = timestamp
            }

            val rrIntervalMillisEntity = ExternalSensorEntity(
                deviceType = DEVICE_TYPE,
                valueType = "RR-Interval-Millis",
                identifier = identifier,
                others = mapOf(
                    "contactStatus" to contactStatus.toString(),
                    "contactStatusSupported" to contactStatusSupported.toString(),
                    "rrAvailable" to rrAvailable.toString()
                ),
                valueFormat = "INT",
                valueUnit = "MS",
                values = rrIntervalInMillis.map { it.toString() }
            ).apply{
                this.timestamp = timestamp
            }

            buffer.onNext(rrIntervalSecEntity)
            buffer.onNext(rrIntervalMillisEntity)
        }

        override fun onEcgChanged(identifier: String, samples: List<Int>) {
            val timestamp = System.currentTimeMillis()
            val entity = ExternalSensorEntity(
                deviceType = DEVICE_TYPE,
                valueType = "ECG",
                identifier = identifier,
                valueFormat = "INT",
                valueUnit = "MICRO_VOLT",
                values = samples.map { it.toString() }
            ).apply{
                this.timestamp = timestamp
            }

            buffer.onNext(entity)
        }

        override fun onAccelerometerChanged(
            identifier: String,
            samples: List<Triple<Int, Int, Int>>
        ) {
            val timestamp = System.currentTimeMillis()
            val entity = ExternalSensorEntity(
                deviceType = DEVICE_TYPE,
                valueType = "ACCELEROMETER",
                identifier = identifier,
                valueFormat = "INT;INT;INT (X;Y;Z)",
                valueUnit = "MILLI_G",
                values = samples.map { "${it.first};${it.second};${it.third}" }
            ).apply{
                this.timestamp = timestamp
            }

            buffer.onNext(entity)
        }

        override fun onError(identifier: String, throwable: Throwable) {
            deviceConnectionStatus = ACCIDENTALLY_DISCONNECTED
            launch { throw throwable }
        }
    }

    private val api by lazy { getPolarApi(context, defaultCallback) }

    override val permissions: List<String> = listOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override val setupIntent: Intent? = Intent(context, PolarH10SettingActivity::class.java)

    override fun isAvailable(): Boolean = deviceId.isNotEmpty()

    override fun getDescription(): Array<Description> = arrayOf(
        R.string.collector_polar_h10_info_device_id with deviceId,
        R.string.collector_polar_h10_info_status with deviceConnectionStatus,
        R.string.collector_polar_h10_info_name with deviceName,
        R.string.collector_polar_h10_info_address with deviceAddress,
        R.string.collector_polar_h10_info_battery with (deviceBatteryLevel.takeIf { it >= 0 } ?: ""),
        R.string.collector_polar_h10_info_rssi with (deviceRssi.takeIf { it <= 0 } ?: "")
    )

    override suspend fun onStart() {
        //if (deviceConnectionStatus != CONNECTED) {}
        api.connect(deviceId)
        context.safeRegisterReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_CHECK_CONNECTION_STATUS)
        })

        buffer.buffer(
            10, TimeUnit.SECONDS
        ).toFlowable(BackpressureStrategy.BUFFER).asFlow().collect { entities ->
            entities.forEach {
                launch { put(it) }
            }
        }
    }

    override suspend fun onStop() {
        deviceConnectionStatus = DISCONNECTED_BY_USER
        deviceRssi = 0
        context.safeUnregisterReceiver(receiver)

        try {
            api.disconnect(deviceId)
        } catch (e: Exception) {
            deviceConnectionStatus = ACCIDENTALLY_DISCONNECTED
            Log.d(javaClass, "onStop(): $e")
        }

        alarmManager.cancel(intent)
    }

    override suspend fun count(): Long = dataRepository.count<ExternalSensorEntity>()

    override suspend fun flush(entities: Collection<ExternalSensorEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<ExternalSensorEntity> = dataRepository.find(0, limit)

    companion object {
        private const val DISCONNECTED = "DISCONNECTED"
        private const val CONNECTING = "CONNECTING"
        private const val CONNECTED = "CONNECTED"
        private const val ACCIDENTALLY_DISCONNECTED = "ACCIDENTALLY_DISCONNECTED"
        private const val DISCONNECTED_BY_USER = "DISCONNECTED_BY_USER"

        private const val ACTION_CHECK_CONNECTION_STATUS =
            "${BuildConfig.APPLICATION_ID}.ACTION_CHECK_CONNECTION_STATUS"
        private const val REQUEST_CODE_CHECK_CONNECTION_STATUS = 0x06

        private const val DEVICE_TYPE = "POLAR H10"

        private const val DEFAULT_EPOCH_TIMESTAMP_MS = 946684800000L    // 2000.01.01
    }
}