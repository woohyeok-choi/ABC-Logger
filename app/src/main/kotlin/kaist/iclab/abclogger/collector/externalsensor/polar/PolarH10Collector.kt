package kaist.iclab.abclogger.collector.externalsensor.polar

import android.Manifest
import android.content.Context
import android.content.Intent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.collector.externalsensor.ExternalSensorEntity
import kaist.iclab.abclogger.collector.externalsensor.polar.setting.PolarH10SettingActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarEcgData
import polar.com.sdk.api.model.PolarHrData
import java.lang.Exception
import java.util.concurrent.TimeUnit

class PolarH10Collector(val context: Context) : BaseCollector, PolarBleApiCallback() {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null,
                      val deviceId: String? = null,
                      val connection: String? = null,
                      val batteryLevel: Int? = null) : BaseStatus() {

        override fun info(): String = "Id: ${deviceId ?: "UNKNOWN"}; " +
                "Connected: ${connection ?: "UNKNOWN"}; " +
                "Battery: ${batteryLevel ?: "UNKNOWN"}"
    }

    private val disposables = CompositeDisposable()

    private val polarApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
                context,
                PolarBleApi.FEATURE_HR or
                        PolarBleApi.FEATURE_BATTERY_INFO or
                        PolarBleApi.FEATURE_DEVICE_INFO or
                        PolarBleApi.FEATURE_POLAR_SENSOR_STREAMING
        ).apply {
            setPolarFilter(false)
            setAutomaticReconnection(true)
            setApiCallback(this@PolarH10Collector)
        }
    }

    private fun handleEcgStreaming(identifier: String) {
        val disposable = polarApi.requestEcgSettings(identifier)
                .flatMapPublisher { setting -> polarApi.startEcgStreaming(identifier, setting.maxSettings()) }
                .buffer(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe { data -> storeEcg(identifier, data) }

        disposables.add(disposable)
    }

    private fun storeEcg(identifier: String, data: List<PolarEcgData>) {
        val timestamp = System.currentTimeMillis()
        data.map { datum ->
            datum.samples.map { ecg ->
                ExternalSensorEntity(
                        sensorId = identifier,
                        name = "PolarH10",
                        description = "ECG",
                        firstValue = ecg.toString()
                ).fill(timeMillis = datum.timeStamp)
            }
        }.flatten().also { entity ->
            GlobalScope.launch {
                ObjBox.put(entity)
                setStatus(Status(lastTime = timestamp))
            }
        }
    }

    private fun storeHeartRate(identifier: String, data: PolarHrData) {
        val timestamp = System.currentTimeMillis()
        val heartRate = data.hr
        val contactStatus = data.contactStatus
        val contactStatusSupported = data.contactStatusSupported

        ExternalSensorEntity(
                sensorId = identifier,
                name = "PolarH10",
                description = "HR/ContactStatus/ContactStatusSupported",
                firstValue = heartRate.toString(),
                secondValue = contactStatus.toString(),
                thirdValue = contactStatusSupported.toString()
        ).fill(timeMillis = timestamp).also { entity ->
            GlobalScope.launch {
                ObjBox.put(entity)
                setStatus(Status(lastTime = timestamp))
            }
        }

        if (data.rrAvailable) {
            data.rrs.zip(data.rrsMs).map { (rrSec, rrMs) ->
                ExternalSensorEntity(
                        sensorId = identifier,
                        name = "PolarH10",
                        description = "RRsec/RRms/ContactStatus/ContactStatusSupported",
                        firstValue = rrSec.toString(),
                        secondValue = rrMs.toString(),
                        thirdValue = contactStatus.toString(),
                        fourthValue = contactStatusSupported.toString()
                ).fill(timeMillis = timestamp)
            }.also { entity ->
                GlobalScope.launch {
                    ObjBox.put(entity)
                    setStatus(Status(lastTime = timestamp))
                }
            }
        }
    }

    override fun ecgFeatureReady(identifier: String) {
        super.ecgFeatureReady(identifier)
        handleEcgStreaming(identifier)
    }

    override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceConnected(polarDeviceInfo)
        GlobalScope.launch { setStatus(Status(connection = "CONNECTED")) }
    }

    override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceConnecting(polarDeviceInfo)
        GlobalScope.launch { setStatus(Status(connection = "CONNECTING")) }
    }

    override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceDisconnected(polarDeviceInfo)
        GlobalScope.launch { setStatus(Status(connection = "DISCONNECTED")) }
        disposables.clear()
    }

    override fun batteryLevelReceived(identifier: String, level: Int) {
        super.batteryLevelReceived(identifier, level)
        GlobalScope.launch { setStatus(Status(batteryLevel = level)) }
    }

    override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
        super.hrNotificationReceived(identifier, data)
        storeHeartRate(identifier, data)
    }

    override suspend fun onStart() {
        disposables.clear()
        polarApi.connectToDevice((getStatus() as? Status)?.deviceId ?: "")
        polarApi.setApiCallback(this)
    }

    override suspend fun onStop() {
        disposables.clear()
        try { polarApi.disconnectFromDevice((getStatus() as? Status)?.deviceId ?: "") } catch (e: Exception) { }
        try { polarApi.setApiCallback(null) } catch (e: Exception) { }
    }

    override suspend fun checkAvailability(): Boolean = !(getStatus() as? Status)?.deviceId.isNullOrBlank() && context.checkPermission(requiredPermissions)

    override val requiredPermissions: List<String>
        get() = listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

    override val newIntentForSetUp: Intent?
        get() = Intent(context, PolarH10SettingActivity::class.java)
}