package kaist.iclab.abclogger.collector

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.observe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.base.BaseSettingActivity
import kotlinx.android.synthetic.main.layout_setting_polar_h10.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.errors.PolarInvalidArgument
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarEcgData
import polar.com.sdk.api.model.PolarHrData
import polar.com.sdk.api.model.PolarSensorSetting
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
        get() = Intent(context, SettingActivity::class.java)

    companion object {
        const val POLAR_DISCONNECTED = 0
        const val POLAR_CONNECTING = 1
        const val POLAR_CONNECTED = 2

        const val EXTRA_POLAR_H10_DEVICE_ID = "${BuildConfig.APPLICATION_ID}.EXTRA_POLAR_H10_DEVICE_ID"
    }

    class PolarViewModel(val context: Context): ViewModel() {
        val battery : MutableLiveData<Int> = MutableLiveData(0)
        val state : MutableLiveData<String> = MutableLiveData()
        val heartRate: MutableLiveData<PolarHrData> = MutableLiveData()
        val ecg: MutableLiveData<PolarEcgData> = MutableLiveData()

        private val disposables = CompositeDisposable()

        private val polarApi = PolarBleApiDefaultImpl.defaultImplementation(
                context,
                PolarBleApi.FEATURE_HR or
                        PolarBleApi.FEATURE_BATTERY_INFO or
                        PolarBleApi.FEATURE_DEVICE_INFO or
                        PolarBleApi.FEATURE_POLAR_SENSOR_STREAMING
        ).also { api ->
            api.setPolarFilter(false)
            api.setAutomaticReconnection(true)
            api.setApiCallback(object : PolarBleApiCallback() {
                override fun ecgFeatureReady(identifier: String) {
                    super.ecgFeatureReady(identifier)
                    val disposable = api.requestEcgSettings(identifier)
                            .flatMapPublisher { setting -> api.startEcgStreaming(identifier, setting.maxSettings()) }
                            .subscribe { data -> ecg.postValue(data) }
                    disposables.add(disposable)
                }

                override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                    state.postValue(context.getString(R.string.general_connected))
                }

                override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                    state.postValue(context.getString(R.string.general_connecting))
                }

                override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                    state.postValue(context.getString(R.string.general_disconnected))
                }

                override fun batteryLevelReceived(identifier: String, level: Int) {
                    super.batteryLevelReceived(identifier, level)
                    battery.postValue(level)
                }

                override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                    super.hrNotificationReceived(identifier, data)
                    heartRate.postValue(data)
                }
            })
        }

        fun connect(deviceId: String) {
            try {
                polarApi.connectToDevice(deviceId)
            } catch (e: PolarInvalidArgument) {
                state.postValue(context.getString(R.string.general_disconnected))
                battery.postValue(0)
                heartRate.postValue(null)
                ecg.postValue(null)
            }
        }

        fun disconnect(deviceId: String) {
            try {
                polarApi.disconnectFromDevice(deviceId)
            } catch (e: PolarInvalidArgument) { }

            polarApi.setApiCallback(null)
            disposables.clear()
        }
    }

    class SettingActivity : BaseSettingActivity() {
        private val viewModel : PolarViewModel by viewModel()

        override val contentLayoutRes: Int
            get() = R.layout.layout_setting_polar_h10

        override val titleStringRes: Int
            get() = R.string.data_name_polar_h10

        override fun initializeSetting() {
            viewModel.state.observe(this) { state -> txt_connection_state.text = state ?: getString(R.string.general_disconnected) }
            viewModel.battery.observe(this) { battery -> txt_battery_state.text = battery?.toString() ?: "0" }
            viewModel.heartRate.observe(this) { heartRate -> txt_heart_rate.text = heartRate?.hr?.toString() ?: "0"}
            viewModel.ecg.observe(this) { ecg -> txt_ecg.text = ecg?.samples?.lastOrNull()?.toString() ?: "0"}

            btn_connect.setOnClickListener { viewModel.connect(edt_device_id.text?.toString() ?: "") }
        }

        override suspend fun generateResultIntent(): Intent = extraIntentFor(
                EXTRA_POLAR_H10_DEVICE_ID to (edt_device_id.text?.toString() ?: "")
        )

        override fun onDestroy() {
            super.onDestroy()
            viewModel.disconnect(edt_device_id.text?.toString() ?: "")
        }
    }
}