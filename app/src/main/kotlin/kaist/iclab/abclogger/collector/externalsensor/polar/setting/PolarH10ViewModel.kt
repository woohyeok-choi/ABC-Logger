package kaist.iclab.abclogger.collector.externalsensor.polar.setting

import android.content.Context
import androidx.lifecycle.*
import io.reactivex.disposables.CompositeDisposable
import kaist.iclab.abclogger.PolarH10Exception
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.externalsensor.polar.PolarH10Collector
import kaist.iclab.abclogger.collector.getStatus
import kaist.iclab.abclogger.collector.setStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarHrData
import java.lang.Exception

class PolarH10ViewModel(private val context: Context, private val collector: PolarH10Collector): ViewModel() {
    val deviceId : MutableLiveData<String> = MutableLiveData()
    val state : MutableLiveData<String> = MutableLiveData(context.getString(R.string.general_disconnected))
    val battery : MutableLiveData<String> = MutableLiveData()
    val heartRate: MutableLiveData<String> = MutableLiveData()
    val rrInterval: MutableLiveData<String> = MutableLiveData()
    val ecg: MutableLiveData<String> = MutableLiveData()

    private var disposables: CompositeDisposable = CompositeDisposable()

    private val callback = object : PolarBleApiCallback() {
        override fun ecgFeatureReady(identifier: String) {
            super.ecgFeatureReady(identifier)
            val disposable = polarApi.requestEcgSettings(identifier)
                    .flatMapPublisher { setting ->
                        val maxSetting = try {
                            setting.maxSettings()
                        } catch (e: Exception) {
                            null
                        } ?: throw PolarH10Exception("Sensor is incorrectly set. Please try once again.")
                        polarApi.startEcgStreaming(identifier, maxSetting)
                    }.subscribe{ data ->
                        ecg.postValue(data.samples.lastOrNull()?.toString() ?: "")
                    }

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

            battery.postValue(null)
            heartRate.postValue(null)
            rrInterval.postValue(null)
            ecg.postValue(null)
        }

        override fun batteryLevelReceived(identifier: String, level: Int) {
            super.batteryLevelReceived(identifier, level)
            battery.postValue(level.toString())
        }

        override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
            super.hrNotificationReceived(identifier, data)
            heartRate.postValue(data.hr.toString())
            rrInterval.postValue(data.rrs?.lastOrNull()?.toString())
        }
    }

    private val polarApi = PolarBleApiDefaultImpl.defaultImplementation(
            context,
            PolarBleApi.FEATURE_HR or
                    PolarBleApi.FEATURE_BATTERY_INFO or
                    PolarBleApi.FEATURE_DEVICE_INFO or
                    PolarBleApi.FEATURE_POLAR_SENSOR_STREAMING
    ).apply {
        setPolarFilter(false)
        setAutomaticReconnection(true)
    }

    init {
        viewModelScope.launch {
            val storedDeviceId = (collector.getStatus() as? PolarH10Collector.Status)?.deviceId ?: context.getString(R.string.general_unknown)
            deviceId.postValue(storedDeviceId)
        }
    }

    fun update(newDeviceId: String? = null) {
        newDeviceId?.let { deviceId.postValue(it) }
    }

    fun connect() = viewModelScope.launch {
        disposables.clear()

        try {
            polarApi.setApiCallback(callback)
            polarApi.connectToDevice(deviceId.value ?: "")
        } catch (e: Exception) {
            state.postValue(context.getString(R.string.general_disconnected))

            battery.postValue(null)
            heartRate.postValue(null)
            rrInterval.postValue(null)
            ecg.postValue(null)
        }
    }

    fun disconnect() = GlobalScope.launch {
        disposables.clear()

        try {
            polarApi.disconnectFromDevice(deviceId.value ?: "")
            polarApi.setApiCallback(null)
        } catch (e: Exception) { }
    }

    fun save() = GlobalScope.launch {
        collector.setStatus(PolarH10Collector.Status(deviceId = deviceId.value))
    }
}