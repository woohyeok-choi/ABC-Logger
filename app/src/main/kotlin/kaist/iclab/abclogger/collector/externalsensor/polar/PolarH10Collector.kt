package kaist.iclab.abclogger.collector.externalsensor.polar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.collector.externalsensor.ExternalSensorEntity
import kaist.iclab.abclogger.collector.sensor.SensorEntity
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarEcgData
import polar.com.sdk.api.model.PolarHrData
import java.util.concurrent.TimeUnit

class PolarH10Collector(val context: Context) : BaseCollector, PolarBleApiCallback() {
    private val disposables = CompositeDisposable()

    private val polarApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
                context,
                PolarBleApi.FEATURE_HR or
                        PolarBleApi.FEATURE_BATTERY_INFO or
                PolarBleApi.FEATURE_DEVICE_INFO or
                PolarBleApi.FEATURE_POLAR_SENSOR_STREAMING
        ).also { api ->
            api.setPolarFilter(false)
            api.setAutomaticReconnection(true)
            api.setApiCallback(this)
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
        data.map { datum ->
            datum.samples.map { ecg ->
                ExternalSensorEntity(
                        sensorId = identifier,
                        name = "PolarH10",
                        description= "ECG",
                        firstValue = ecg.toString()
                ).fill(timeMillis = datum.timeStamp)
            }
        }.flatten().run { ObjBox.put(this) }
    }

    private fun storeHeartRate(identifier: String, data: PolarHrData) {
        val timestamp = System.currentTimeMillis()
        val heartRate = data.hr
        val contactStatus = data.contactStatus
        val contactStatusSupported = data.contactStatusSupported

        ExternalSensorEntity(
                sensorId = CollectorPrefs.polarH10DeviceId,
                name = "PolarH10",
                description = "HR/ContactStatus/ContactStatusSupported",
                firstValue = heartRate.toString(),
                secondValue = contactStatus.toString(),
                thirdValue = contactStatusSupported.toString()
        ).fill(timeMillis = timestamp).run { ObjBox.put(this) }

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
            }.run { ObjBox.put(this) }
        }
    }

    override fun ecgFeatureReady(identifier: String) {
        super.ecgFeatureReady(identifier)
        handleEcgStreaming(identifier)
    }

    override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceConnected(polarDeviceInfo)
        CollectorPrefs.polarH10Connection = POLAR_CONNECTED
    }

    override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceConnecting(polarDeviceInfo)
        CollectorPrefs.polarH10Connection = POLAR_CONNECTING
    }

    override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceDisconnected(polarDeviceInfo)
        CollectorPrefs.polarH10Connection = POLAR_DISCONNECTED
        disposables.clear()
    }

    override fun batteryLevelReceived(identifier: String, level: Int) {
        super.batteryLevelReceived(identifier, level)
        CollectorPrefs.polarH10BatteryLevel = level
    }

    override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
        super.hrNotificationReceived(identifier, data)
        storeHeartRate(identifier, data)
    }

    override suspend fun onStart() {
        disposables.clear()
        polarApi.connectToDevice(CollectorPrefs.polarH10DeviceId)
        polarApi.setApiCallback(this)
    }

    override suspend fun onStop() {
        disposables.clear()
        polarApi.disconnectFromDevice(CollectorPrefs.polarH10DeviceId)
        polarApi.setApiCallback(null)
    }

    override fun checkAvailability(): Boolean =
            !TextUtils.isEmpty(CollectorPrefs.polarH10DeviceId) && context.checkPermission(requiredPermissions)

    override val requiredPermissions: List<String>
        get() = listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

    override val newIntentForSetUp: Intent?
        get() = Intent(context, PolarH10SettingActivity::class.java)

    companion object {
        const val POLAR_DISCONNECTED = 0
        const val POLAR_CONNECTING = 1
        const val POLAR_CONNECTED = 2
    }
}