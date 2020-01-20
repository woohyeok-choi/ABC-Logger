package kaist.iclab.abclogger.collector.sensor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.observe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.base.BaseSettingActivity
import kaist.iclab.abclogger.collector.putEntity
import kotlinx.android.synthetic.main.layout_setting_polar_h10.*
import org.koin.androidx.viewmodel.ext.android.viewModel
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

    private fun updateStatus() {
        CollectorPrefs.statusPolarH10 = "Id: ${ExternalDevicePrefs.polarH10DeviceId}; Connected: ${ExternalDevicePrefs.polarH10Connection == POLAR_CONNECTED}; Battery: ${ExternalDevicePrefs.polarH10BatteryLevel}"
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
        updateStatus()
    }

    override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceConnecting(polarDeviceInfo)
        ExternalDevicePrefs.polarH10Connection = POLAR_CONNECTING
        updateStatus()
    }

    override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
        super.deviceDisconnected(polarDeviceInfo)
        ExternalDevicePrefs.polarH10Connection = POLAR_DISCONNECTED
        updateStatus()
        disposables.clear()
    }

    override fun batteryLevelReceived(identifier: String, level: Int) {
        super.batteryLevelReceived(identifier, level)
        ExternalDevicePrefs.polarH10BatteryLevel = level
        updateStatus()
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
        updateStatus()
    }

    override fun onStop() {
        try {
            polarApi.disconnectFromDevice(ExternalDevicePrefs.polarH10DeviceId)
        } catch (e: PolarInvalidArgument) { }
        polarApi.setApiCallback(null)
        disposables.clear()
        ExternalDevicePrefs.polarH10Connection = POLAR_DISCONNECTED
        updateStatus()
    }

    override fun checkAvailability(): Boolean =
            !TextUtils.isEmpty(ExternalDevicePrefs.polarH10DeviceId) &&
                    ExternalDevicePrefs.polarH10Connection == POLAR_CONNECTED &&
                    context.checkPermission(requiredPermissions)

    override fun handleActivityResult(resultCode: Int, intent: Intent?) {
        if (resultCode != Activity.RESULT_OK) return

        ExternalDevicePrefs.polarH10DeviceId = intent?.getStringExtra(EXTRA_POLAR_H10_DEVICE_ID) ?: ""
    }

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

        const val EXTRA_POLAR_H10_DEVICE_ID = "${BuildConfig.APPLICATION_ID}.EXTRA_POLAR_H10_DEVICE_ID"
    }
}