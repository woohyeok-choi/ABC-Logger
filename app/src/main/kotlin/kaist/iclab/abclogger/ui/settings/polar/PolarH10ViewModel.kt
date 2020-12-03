package kaist.iclab.abclogger.ui.settings.polar

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.collector.external.PolarH10Collector
import kaist.iclab.abclogger.ui.base.BaseViewModel
import kaist.iclab.abclogger.commons.AbcError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PolarH10ViewModel(
    private val collector: PolarH10Collector,
    savedStateHandle: SavedStateHandle,
    application: Application
) : BaseViewModel(savedStateHandle, application) {
    private val statusChannel = Channel<Connection?>()
    private val batteryChannel = Channel<Int?>()
    private val hrChannel = Channel<HeartRate?>()
    private val ecgChannel = Channel<Int?>()
    private val accChannel = Channel<Triple<Int, Int, Int>?>()
    private val errorChannel = Channel<Throwable>()

    private val callback = object : PolarH10Collector.Callback() {
        override fun onConnectionStateChanged(identifier: String, name: String, address: String, rssi: Int, state: String) {
            viewModelScope.launch(Dispatchers.IO) {
                statusChannel.send(Connection(name, address, rssi, state))
            }
        }

        override fun onBatteryChanged(identifier: String, level: Int) {
            viewModelScope.launch(Dispatchers.IO) {
                batteryChannel.send(level)
            }
        }

        override fun onHeartRateReceived(identifier: String, heartRate: Int, rrAvailable: Boolean, rrIntervalInSec: List<Int>, rrIntervalInMillis: List<Int>, contactStatusSupported: Boolean, contactStatus: Boolean) {
            viewModelScope.launch(Dispatchers.IO) {
                hrChannel.send(HeartRate(heartRate, rrIntervalInMillis, contactStatus))
            }
        }

        override fun onEcgChanged(identifier: String, samples: List<Int>) {
            viewModelScope.launch(Dispatchers.IO) {
                ecgChannel.send(samples.lastOrNull())
            }
        }

        override fun onAccelerometerChanged(identifier: String, samples: List<Triple<Int, Int, Int>>) {
            viewModelScope.launch(Dispatchers.IO) {
                accChannel.send(samples.lastOrNull())
            }
        }

        override fun onError(identifier: String, throwable: Throwable) {
            viewModelScope.launch(Dispatchers.IO) {
                errorChannel.send(throwable)
            }
        }
    }

    private val api = collector.getPolarApi(getApplication(), callback)

    var deviceId: String
        get() = collector.deviceId
        set(value) {
            collector.deviceId = value
        }
    val status = statusChannel.receiveAsFlow()
    val battery = batteryChannel.receiveAsFlow()
    val heartRate = hrChannel.receiveAsFlow()
    val ecg = ecgChannel.receiveAsFlow()
    val accelerometer = accChannel.receiveAsFlow()
    val error = errorChannel.receiveAsFlow()

    private var lastConnectedId: String? = null

    fun connect(deviceId: String) {
        lastConnectedId = deviceId
        try {
            api.connect(deviceId)
        } catch (e: Exception) {
            errorChannel.offer(AbcError.wrap(e))
        }
    }

    fun disconnect() {
        try {
            lastConnectedId?.let { api.disconnect(it) }

            viewModelScope.launch(Dispatchers.IO) {
                statusChannel.send(null)
                batteryChannel.send(null)
                hrChannel.send(null)
                accChannel.send(null)
                ecgChannel.send(null)
            }
        } catch (e: Exception) {
        }
    }
}