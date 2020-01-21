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
import kaist.iclab.abclogger.collector.activity.ActivityCollector
import kaist.iclab.abclogger.collector.appusage.AppUsageCollector
import kaist.iclab.abclogger.collector.battery.BatteryCollector
import kaist.iclab.abclogger.collector.bluetooth.BluetoothCollector
import kaist.iclab.abclogger.collector.call.CallLogCollector
import kaist.iclab.abclogger.collector.event.DeviceEventCollector
import kaist.iclab.abclogger.collector.install.InstalledAppCollector
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.collector.location.LocationCollector
import kaist.iclab.abclogger.collector.media.MediaCollector
import kaist.iclab.abclogger.collector.message.MessageCollector
import kaist.iclab.abclogger.collector.notification.NotificationCollector
import kaist.iclab.abclogger.collector.physicalstatus.PhysicalStatusCollector
import kaist.iclab.abclogger.collector.sensor.PolarH10Collector
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.traffic.DataTrafficCollector
import kaist.iclab.abclogger.collector.wifi.WifiCollector
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception

class ConfigViewModel(
        val context: Context,
        val abcLogger: ABCLogger
) : ViewModel() {
    val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
    val isAllPermissionGranted = MutableLiveData<Boolean>()
    val lastSyncTime = MutableLiveData<String>()
    val shouldUploadForNonMeteredNetwork = MutableLiveData<Boolean>()
    val sizeOfDb = MutableLiveData<String>()
    val collectors = MutableLiveData<ArrayList<BaseCollector>>()
    val flushStatus = MutableLiveData<Pair<Status, Boolean>>()

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        isAllPermissionGranted.postValue(context.checkPermission(abcLogger.getAllRequiredPermissions()))
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
        collectors.postValue(abcLogger.collectors)
    }

    fun setUploadForNonMeteredNetwork(isEnabled: Boolean) {
        GeneralPrefs.shouldUploadForNonMeteredNetwork = isEnabled
    }

    fun requestStart(collector: BaseCollector, onError: ((Throwable) -> Unit)? = null) = viewModelScope.launch {
        collector.start { _, throwable -> onError?.invoke(throwable) }
    }

    fun requestStop(collector: BaseCollector, onError: ((Throwable) -> Unit)? = null) = viewModelScope.launch {
        collector.stop { _, throwable -> onError?.invoke(throwable)}
    }

    fun flush() = GlobalScope.launch(Dispatchers.IO) {
        try {
            flushStatus.postValue(Status.loading() to false)
            ObjBox.boxStore.deleteAllFiles()
            flushStatus.postValue(Status.success() to false)
        } catch (e: Exception) {
            flushStatus.postValue(Status.failure(e) to false)
        }
    }

    fun signOut() = GlobalScope.launch(Dispatchers.IO) {
        try {
            flushStatus.postValue(Status.loading() to true)
            ObjBox.boxStore.deleteAllFiles()
            FirebaseAuth.getInstance().signOut()
            GeneralPrefs.clear()
            CollectorPrefs.clear()
            flushStatus.postValue(Status.success() to true)
        } catch (e: Exception) {
            flushStatus.postValue(Status.failure(e) to true)
        }
    }
}