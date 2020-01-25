package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.collector.start
import kaist.iclab.abclogger.collector.stop
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception

class ConfigViewModel(
        val context: Context,
        val abcLogger: ABC
) : ViewModel() {
    val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
    val isAllPermissionGranted = MutableLiveData<Boolean>(permissionGranted())
    val lastSyncTime = MutableLiveData<String>(lastSyncTime())
    val shouldUploadForNonMeteredNetwork = MutableLiveData<Boolean>(GeneralPrefs.shouldUploadForNonMeteredNetwork)
    val sizeOfDb = MutableLiveData<String>(sizeOfDb())
    val collectors = MutableLiveData<Array<BaseCollector>>(abcLogger.collectors)
    val flushStatus = MutableLiveData<Status>()

    private fun permissionGranted() = context.checkPermission(abcLogger.getAllRequiredPermissions())

    private fun lastSyncTime() =
            if (GeneralPrefs.lastTimeDataSync > 0) {
        String.format("%s: %s",
                context.getString(R.string.general_last_sync_time),
                DateUtils.formatDateTime(context, GeneralPrefs.lastTimeDataSync, DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
        )
    } else {
        context.getString(R.string.general_none)
    }

    private fun sizeOfDb() = "${Formatter.formatFileSize(context, ObjBox.size(context))} / ${Formatter.formatFileSize(context, ObjBox.maxSizeInBytes())}"

    fun setUploadForNonMeteredNetwork(isEnabled: Boolean) {
        GeneralPrefs.shouldUploadForNonMeteredNetwork = isEnabled
        shouldUploadForNonMeteredNetwork.postValue(GeneralPrefs.shouldUploadForNonMeteredNetwork)
    }

    fun updatePermission() {
        isAllPermissionGranted.postValue(permissionGranted())
    }

    fun updateCollectors() {
        collectors.postValue(abcLogger.collectors)
    }

    fun requestStart(collector: BaseCollector, onComplete: ((BaseCollector, Throwable?) -> Unit)? = null) = viewModelScope.launch {
        collector.start { collector, throwable -> onComplete?.invoke(collector, throwable) }
    }

    fun requestStop(collector: BaseCollector, onComplete: ((BaseCollector, Throwable?) -> Unit)? = null) = viewModelScope.launch {
        collector.stop { collector, throwable -> onComplete?.invoke(collector, throwable) }
    }

    fun sync() = GlobalScope.launch(Dispatchers.IO) {

    }

    fun flush(onComplete: ((isSuccessful: Boolean) -> Unit)? = null) = GlobalScope.launch {
        try {
            flushStatus.postValue(Status.loading())

            ObjBox.flush(context)

            flushStatus.postValue(Status.success())
            sizeOfDb.postValue(sizeOfDb())
            onComplete?.invoke(true)
        } catch (e: Exception) {
            flushStatus.postValue(Status.failure(e))

            onComplete?.invoke(true)
        }
    }

    fun signOut(onComplete: ((isSuccessful: Boolean) -> Unit)? = null) = GlobalScope.launch {
        try {
            flushStatus.postValue(Status.loading())
            ABC.signOut(context)
            flushStatus.postValue(Status.success())
            sizeOfDb.postValue(sizeOfDb())
            onComplete?.invoke(true)
        } catch (e: Exception) {
            flushStatus.postValue(Status.failure(e))

            onComplete?.invoke(false)
        }
    }
}