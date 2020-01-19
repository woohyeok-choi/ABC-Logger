package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.collector.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ConfigViewModel(
        val context: Context,
        val abc: ABC
) : ViewModel() {
    data class DataStatus(val isAvailable: Boolean, val hasStarted: Boolean, val status: String? = null)

    val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
    val lastSyncTime = MutableLiveData<String>()
    val shouldUploadForNonMeteredNetwork = MutableLiveData<Boolean>()
    val sizeOfDb = MutableLiveData<String>()

    val statusActivity = MutableLiveData<DataStatus>()
    val statusAppUsage = MutableLiveData<DataStatus>()
    val statusBattery = MutableLiveData<DataStatus>()
    val statusBluetooth = MutableLiveData<DataStatus>()
    val statusCallLog = MutableLiveData<DataStatus>()
    val statusDataTraffic = MutableLiveData<DataStatus>()
    val statusDeviceEvent = MutableLiveData<DataStatus>()
    val statusInstalledApp = MutableLiveData<DataStatus>()
    val statusKeyTracking = MutableLiveData<DataStatus>()
    val statusLocation = MutableLiveData<DataStatus>()
    val statusMedia = MutableLiveData<DataStatus>()
    val statusMessage = MutableLiveData<DataStatus>()
    val statusNotification = MutableLiveData<DataStatus>()
    val statusPhysicalStatus = MutableLiveData<DataStatus>()
    val statusPolarH10 = MutableLiveData<DataStatus>()
    val statusSurvey = MutableLiveData<DataStatus>()
    val statusWifi = MutableLiveData<DataStatus>()

    private inline fun <reified T : BaseCollector> updateStatus(liveData: MutableLiveData<DataStatus>) {
        val isAvailable = abc.isAvailable<T>()
        val hasStarted = abc.hasStarted<T>()
        val prefix = context.getString(if(isAvailable) { R.string.general_available } else R.string.general_unavailable)
        val message = abc.status<T>()

        liveData.postValue(
                DataStatus(isAvailable, hasStarted, listOf(prefix, message).filter { it.isNotBlank() }.joinToString(": "))
        )
    }

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        lastSyncTime.postValue(if (GeneralPrefs.lastTimeDataSync > 0) {
            String.format("%s: %s",
                    context.getString(R.string.general_last_sync_time),
                    DateUtils.formatDateTime(context, GeneralPrefs.lastTimeDataSync, DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
            )
        } else {
            context.getString(R.string.general_none)
        })

        sizeOfDb.postValue(
                "${Formatter.formatFileSize(context, ObjBox.size(context))} / ${Formatter.formatFileSize(context, ObjBox.maxSizeInBytes())}"
        )
        shouldUploadForNonMeteredNetwork.postValue(GeneralPrefs.shouldUploadForNonMeteredNetwork)

        updateStatus<ActivityCollector>(statusActivity)
        updateStatus<AppUsageCollector>(statusAppUsage)
        updateStatus<BatteryCollector>(statusBattery)
        updateStatus<BluetoothCollector>(statusBluetooth)
        updateStatus<CallLogCollector>(statusCallLog)
        updateStatus<DataTrafficCollector>(statusDataTraffic)
        updateStatus<DeviceEventCollector>(statusDeviceEvent)
        updateStatus<InstalledAppCollector>(statusInstalledApp)
        updateStatus<KeyLogCollector>(statusKeyTracking)
        updateStatus<LocationCollector>(statusLocation)
        updateStatus<MediaCollector>(statusMedia)
        updateStatus<MessageCollector>(statusMessage)
        updateStatus<NotificationCollector>(statusNotification)
        updateStatus<PhysicalStatusCollector>(statusPhysicalStatus)
        updateStatus<PolarH10Collector>(statusPolarH10)
        updateStatus<SurveyCollector>(statusSurvey)
        updateStatus<WifiCollector>(statusWifi)
    }

    fun shouldUploadForNonMeteredNetwork(isEnabled: Boolean) {
        GeneralPrefs.shouldUploadForNonMeteredNetwork = isEnabled
    }

    fun flush() = GlobalScope.launch(Dispatchers.IO) {
        ObjBox.boxStore.deleteAllFiles()
    }

    fun signOut() = GlobalScope.launch(Dispatchers.IO) {
        ObjBox.boxStore.deleteAllFiles()
    }
}