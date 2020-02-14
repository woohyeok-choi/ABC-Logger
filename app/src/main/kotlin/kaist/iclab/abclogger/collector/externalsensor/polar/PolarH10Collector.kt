package kaist.iclab.abclogger.collector.externalsensor.polar

import android.Manifest
import android.content.Context
import android.content.Intent
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.collector.externalsensor.ExternalSensorEntity
import kaist.iclab.abclogger.collector.externalsensor.polar.setting.PolarH10SettingActivity
import kaist.iclab.abclogger.commons.PolarH10Exception
import kaist.iclab.abclogger.commons.checkPermission
import kotlinx.coroutines.launch
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarHrData
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class PolarH10Collector(private val context: Context) : BaseCollector<PolarH10Collector.Status>(context) {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null,
                      override val lastError: Throwable? = null,
                      val deviceId: String? = null,
                      val connection: String? = null,
                      val batteryLevel: Int? = null) : BaseStatus() {

        override fun info(): String = "Id: ${deviceId ?: "UNKNOWN"}; " +
                "Connected: ${connection ?: "UNKNOWN"}; " +
                "Battery: ${batteryLevel ?: "UNKNOWN"}"
    }

    override val clazz: KClass<Status> = Status::class

    override val name: String = context.getString(R.string.data_name_polar_h10)

    override val description: String = context.getString(R.string.data_desc_polar_h10)

    override val requiredPermissions: List<String> = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    override val newIntentForSetUp: Intent? = Intent(context, PolarH10SettingActivity::class.java)

    override suspend fun checkAvailability(): Boolean = !getStatus()?.deviceId.isNullOrBlank() && context.checkPermission(requiredPermissions)

    override suspend fun onStart() {
        ecgDisposable?.dispose()
        hrDisposable?.dispose()

        api.setApiCallback(callback)
        api.connectToDevice(getStatus()?.deviceId ?: "")

        hrDisposable = hrSubject.buffer(
                10, TimeUnit.SECONDS
        ).subscribe { entities ->
            launch {
                ObjBox.put(entities)
                setStatus(Status(lastTime = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun onStop() {
        ecgDisposable?.dispose()
        hrDisposable?.dispose()

        try { api.disconnectFromDevice(getStatus()?.deviceId ?: "") } catch (e: Exception) { }
        try { api.setApiCallback(null) } catch (e: Exception) { }
    }

    private val hrSubject: PublishSubject<ExternalSensorEntity> = PublishSubject.create()

    private var ecgDisposable : Disposable? = null

    private var hrDisposable : Disposable? = null

    private val api: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
                context,
                PolarBleApi.FEATURE_HR or
                        PolarBleApi.FEATURE_BATTERY_INFO or
                        PolarBleApi.FEATURE_DEVICE_INFO or
                        PolarBleApi.FEATURE_POLAR_SENSOR_STREAMING
        ).apply {
            setPolarFilter(false)
            setAutomaticReconnection(true)
        }
    }

    private val callback : PolarBleApiCallback by lazy {
        object : PolarBleApiCallback() {
            override fun ecgFeatureReady(identifier: String) {
                handleEcgFeatureReady(identifier)
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                handleConnectionRetrieval(context.getString(R.string.general_connected))
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                handleConnectionRetrieval(context.getString(R.string.general_connecting))
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                handleConnectionRetrieval(context.getString(R.string.general_disconnected))
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                handleBatteryRetrieval(level)
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                handleHeartRateRetrieval(identifier, data)
            }
        }
    }

    private fun handleConnectionRetrieval(connection: String?) {
        launch {
            setStatus(Status(connection = connection))
        }
    }

    private fun handleHeartRateRetrieval(identifier: String, data: PolarHrData) {
        val timestamp = System.currentTimeMillis()
        val heartRate = data.hr
        val contactStatus = data.contactStatus
        val contactStatusSupported = data.contactStatusSupported

        ExternalSensorEntity(
                sensorId = identifier,
                name = "PolarH10",
                description = "HR,ContactStatus,ContactStatusSupported",
                firstValue = heartRate.toFloat(),
                secondValue = if (contactStatus) 1.0F else 0.0F,
                thirdValue = if (contactStatusSupported) 1.0F else 0.0F
        ).fill(timeMillis = timestamp).let {
            hrSubject.onNext(it)
        }

        if (data.rrAvailable) {
            ExternalSensorEntity(
                    sensorId = identifier,
                    name = "PolarH10",
                    description = "ContactStatus,ContactStatusSupported,RRsec",
                    thirdValue = if (contactStatus) 1.0F else 0.0F,
                    fourthValue = if (contactStatusSupported) 1.0F else 0.0F,
                    collection = data.rrs?.joinToString(",") ?: ""
            ).fill(timeMillis = timestamp).let {
                hrSubject.onNext(it)
            }

            ExternalSensorEntity(
                    sensorId = identifier,
                    name = "PolarH10",
                    description = "ContactStatus,ContactStatusSupported,RRmillis",
                    thirdValue = if (contactStatus) 1.0F else 0.0F,
                    fourthValue = if (contactStatusSupported) 1.0F else 0.0F,
                    collection = data.rrsMs?.joinToString(",") ?: ""
            ).fill(timeMillis = timestamp).let {
                hrSubject.onNext(it)
            }
        }
    }

    private fun handleBatteryRetrieval(level: Int) {
        launch {
            setStatus(Status(batteryLevel = level))
        }
    }

    private fun handleEcgFeatureReady(identifier: String) {
        if (ecgDisposable?.isDisposed != false) ecgDisposable?.dispose()

        ecgDisposable = api.requestEcgSettings(identifier)
                .map { setting ->
                    setting.maxSettings()
                            ?: throw PolarH10Exception("Sensor is incorrectly set. Please try once again.")
                }.retry { throwable ->
                    throwable is PolarH10Exception
                }.flatMapPublisher { setting ->
                    api.startEcgStreaming(identifier, setting.maxSettings())
                }.map { data ->
                    ExternalSensorEntity(
                            sensorId = identifier,
                            name = "PolarH10",
                            description = "ECG/mV",
                            collection = data.samples?.joinToString(",") ?: ""
                    ).fill(timeMillis = System.currentTimeMillis())
                }.buffer(
                        5, TimeUnit.SECONDS
                ).subscribe({ entities ->
                    launch {
                        ObjBox.put(entities)
                        setStatus(Status(lastTime = System.currentTimeMillis()))
                    }
                }, { throwable ->
                    launch {
                        setStatus(Status(lastError = throwable))
                    }
                })
    }
}