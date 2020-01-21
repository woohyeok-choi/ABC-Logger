package kaist.iclab.abclogger.collector.sensor

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import kaist.iclab.abclogger.CollectorPrefs
import kaist.iclab.abclogger.R
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.errors.PolarInvalidArgument
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarHrData

class PolarH10ViewModel(val context: Context): ViewModel() {
    val deviceId : MutableLiveData<String> = MutableLiveData(CollectorPrefs.polarH10DeviceId)
    val state : MutableLiveData<String> = MutableLiveData(context.getString(R.string.general_disconnected))
    val battery : MutableLiveData<String> = MutableLiveData("0")
    val heartRate: MutableLiveData<String> = MutableLiveData("0")
    val ecg: MutableLiveData<String> = MutableLiveData("0")

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
                        .subscribe { data -> ecg.postValue(data.samples.lastOrNull()?.toString() ?: "") }
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
                battery.postValue(level.toString())
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                super.hrNotificationReceived(identifier, data)
                heartRate.postValue(data.hr.toString())
            }
        })
    }

    fun connect() {
        try {
            polarApi.connectToDevice(deviceId.value ?: "")
        } catch (e: PolarInvalidArgument) {
            state.postValue(context.getString(R.string.general_disconnected))
            battery.postValue("")
            heartRate.postValue(null)
            ecg.postValue(null)
        }
    }

    fun disconnect() {
        try {
            polarApi.disconnectFromDevice(deviceId.value ?: "")
        } catch (e: PolarInvalidArgument) { }

        polarApi.setApiCallback(null)
        disposables.clear()
    }
}