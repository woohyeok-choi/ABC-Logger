package kaist.iclab.abclogger.collector.externalsensor.polar.setting

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.*
import io.reactivex.disposables.Disposable
import kaist.iclab.abclogger.commons.PolarH10Exception
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.externalsensor.polar.PolarH10Collector
import kaist.iclab.abclogger.ui.base.BaseViewModel
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarHrData

class PolarH10ViewModel(private val context: Context,
                        private val collector: PolarH10Collector,
                        navigator: PolarH10Navigator): BaseViewModel<PolarH10Navigator>(navigator) {
    val deviceId : MutableLiveData<String> = MutableLiveData()
    val state : MutableLiveData<String> = MutableLiveData(context.getString(R.string.general_disconnected))
    val battery : MutableLiveData<String> = MutableLiveData()
    val heartRate: MutableLiveData<String> = MutableLiveData()
    val rrInterval: MutableLiveData<String> = MutableLiveData()
    val ecg: MutableLiveData<String> = MutableLiveData()

    override suspend fun onLoad(extras: Bundle?) {
        deviceId.postValue(collector.getStatus()?.deviceId)
    }

    override suspend fun onStore() {
        disconnect()
        collector.setStatus(PolarH10Collector.Status(deviceId = deviceId.value))
        ui { nav?.navigateStore() }
    }

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

    private val callback = object : PolarBleApiCallback() {
        override fun ecgFeatureReady(identifier: String) {
            super.ecgFeatureReady(identifier)
            if (ecgDisposable?.isDisposed != false) ecgDisposable?.dispose()

            ecgDisposable = api.requestEcgSettings(identifier)
                    .map { setting ->
                        setting.maxSettings() ?: throw PolarH10Exception("Sensor is incorrectly set. Please try once again.")
                    }.retry { throwable ->
                        throwable is PolarH10Exception
                    }.flatMapPublisher { setting ->
                        api.startEcgStreaming(identifier, setting.maxSettings())
                    }.subscribe({ data ->
                        ecg.postValue(data.samples.lastOrNull()?.toString())
                    }, { t ->
                        launch {
                            ui {
                                nav?.navigateError(t)
                            }
                        }
                    })
        }

        override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
            state.postValue(context.getString(R.string.general_connected))
        }

        override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
            state.postValue(context.getString(R.string.general_connecting))
        }

        override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
            state.postValue(context.getString(R.string.general_disconnected))

            battery.postValue(null)
            heartRate.postValue(null)
            rrInterval.postValue(null)
            ecg.postValue(null)
        }

        override fun batteryLevelReceived(identifier: String, level: Int) {
            battery.postValue(level.toString())
        }

        override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
            heartRate.postValue(data.hr.toString())
            rrInterval.postValue(data.rrs?.lastOrNull()?.toString())
        }
    }

    private var ecgDisposable : Disposable? = null

    fun connect() = launch {
        try {
            ecgDisposable?.dispose()
            api.setApiCallback(callback)
            api.connectToDevice(deviceId.value ?: "")
        } catch (e: Exception) {
            ui { nav?.navigateError(e) }
            state.postValue(context.getString(R.string.general_disconnected))
            battery.postValue(null)
            heartRate.postValue(null)
            rrInterval.postValue(null)
            ecg.postValue(null)
        }
    }

    fun disconnect() = launch {
        try {
            api.disconnectFromDevice(deviceId.value ?: "")
            api.setApiCallback(null)
        } catch (e: Exception) { }

        state.postValue(context.getString(R.string.general_disconnected))
        battery.postValue(null)
        heartRate.postValue(null)
        rrInterval.postValue(null)
        ecg.postValue(null)
    }

    override fun onCleared() {
        ecgDisposable?.dispose()
        super.onCleared()
    }
}