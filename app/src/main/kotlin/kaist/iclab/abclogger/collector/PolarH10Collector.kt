package kaist.iclab.abclogger.collector

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.base.BaseCollector
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.errors.PolarInvalidArgument
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarEcgData
import polar.com.sdk.api.model.PolarHrData
import java.util.concurrent.TimeUnit

class PolarH10Collector(val context: Context) : BaseCollector, PolarBleApiCallback() {
    private val disposables = CompositeDisposable()

    private val polarApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
                context, PolarBleApi.FEATURE_HR or PolarBleApi.FEATURE_BATTERY_INFO or
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
                SensorEntity(
                        sensorId = identifier,
                        sensorName = "PolarH10",
                        valueType = "ECG",
                        valueDescription = "ECG",
                        firstValue = ecg.toFloat()
                ).fillBaseInfo(
                        timeMillis = datum.timeStamp
                )
            }
        }.flatten().run {
            putEntity(this)
        }
    }

    private fun storeHeartRate(identifier: String, data: PolarHrData) {
        val timestamp = System.currentTimeMillis()
        val heartRate = data.hr
        val contactStatus = if (data.contactStatus) 1.0F else 0.0F
        val contactStatusSupported = if (data.contactStatusSupported) 1.0F else 0.0F

        SensorEntity(
                sensorId = ExternalDevicePrefs.polarH10DeviceId,
                sensorName = "PolarH10",
                valueType = "HeartRate",
                valueDescription = "HR/ContactStatus/ContactStatusSupported",
                firstValue = heartRate.toFloat(),
                secondValue = contactStatus,
                thirdValue = contactStatusSupported
        ).fillBaseInfo(timeMillis = timestamp).run { putEntity(this) }

        if (data.rrAvailable) {
            data.rrs.zip(data.rrsMs).map { (rrSec, rrMs) ->
                SensorEntity(
                        sensorId = identifier,
                        sensorName = "PolarH10",
                        valueType = "RRInterval",
                        valueDescription = "RRsec/RRms/ContactStatus/ContactStatusSupported",
                        firstValue = rrSec.toFloat(),
                        secondValue = rrMs.toFloat(),
                        thirdValue = contactStatus,
                        fourthValue = contactStatusSupported
                ).fillBaseInfo(timeMillis = timestamp)
            }.run { putEntity(this) }
        }
    }

    override fun ecgFeatureReady(identifier: String) {
        super.ecgFeatureReady(identifier)
        handleEcgStreaming(identifier)
    }

    override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceConnected(polarDeviceInfo)
        ExternalDevicePrefs.polarH10Connection = POLAR_CONNECTED
    }

    override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceConnecting(polarDeviceInfo)
        ExternalDevicePrefs.polarH10Connection = POLAR_CONNECTING
    }

    override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceDisconnected(polarDeviceInfo)
        ExternalDevicePrefs.polarH10Connection = POLAR_DISCONNECTED
        disposables.clear()
    }

    override fun batteryLevelReceived(identifier: String, level: Int) {
        super.batteryLevelReceived(identifier, level)
        ExternalDevicePrefs.polarH10BatteryLevel = level
    }

    override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
        super.hrNotificationReceived(identifier, data)
        storeHeartRate(identifier, data)
    }

    override fun onStart() {
        try {
            polarApi.connectToDevice(ExternalDevicePrefs.polarH10DeviceId)
        } catch (e: PolarInvalidArgument) {
            ExternalDevicePrefs.polarH10Connection = POLAR_DISCONNECTED
        }
    }

    override fun onStop() {
        try {
            polarApi.disconnectFromDevice(ExternalDevicePrefs.polarH10DeviceId)
            polarApi.setApiCallback(null)
        } catch (e: PolarInvalidArgument) { }
        disposables.clear()
        ExternalDevicePrefs.polarH10Connection = POLAR_DISCONNECTED
    }

    override fun checkAvailability(): Boolean =
            !TextUtils.isEmpty(ExternalDevicePrefs.polarH10DeviceId) &&
                    ExternalDevicePrefs.polarH10Connection == POLAR_CONNECTED &&
                    context.checkPermission(requiredPermissions)

    override fun handleActivityResult(resultCode: Int, intent: Intent?) {}

    override val requiredPermissions: List<String>
        get() = listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

    override val newIntentForSetUp: Intent?
        get() = Intent(context, PolarH10SettingActivity::class.java)


    class PolarH10SettingActivity : BaseAppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
        }
    }

    companion object {
        const val POLAR_DISCONNECTED = 0
        const val POLAR_CONNECTING = 1
        const val POLAR_CONNECTED = 2
    }
}