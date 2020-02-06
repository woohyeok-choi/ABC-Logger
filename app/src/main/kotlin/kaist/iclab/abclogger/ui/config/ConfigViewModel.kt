package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ConfigViewModel(
        private val context: Context,
        private val abcLogger: ABC
) : ViewModel() {
    val errorStatus: MutableLiveData<Status> = MutableLiveData(Status.init())
    val configs: MutableLiveData<ArrayList<ConfigData>> = MutableLiveData()

    fun update() = viewModelScope.launch {
        try {
            errorStatus.postValue(Status.init())
            configs.postValue(
                    arrayListOf<ConfigData>().apply {
                        (generalConfig() + dataConfig() + otherConfig()).forEach { add(it) }
                    }
            )
        } catch (e: Exception) {
            errorStatus.postValue(Status.failure(e))
        }
    }

    fun setUploadSetting(enableMetered: Boolean) {
        SyncWorker.requestStart(context, false, enableMetered)
    }

    fun requestStart(prefKey: String) = viewModelScope.launch {
        abcLogger.collectors.find { it.prefKey() == prefKey }?.start { _, throwable ->
            if (throwable != null) errorStatus.postValue(Status.failure(throwable))
        }
    }

    fun requestStop(prefKey: String) = viewModelScope.launch {
        abcLogger.collectors.find { it.prefKey() == prefKey }?.stop { _, throwable ->
            if (throwable != null) errorStatus.postValue(Status.failure(throwable))
        }
    }

    fun sync() {
        SyncWorker.requestStart(context, true)
    }

    fun flush() = GlobalScope.launch (Dispatchers.IO) {
        ObjBox.flush(context, true)
    }

    fun logout(onComplete: () -> Unit) = GlobalScope.launch (Dispatchers.IO) {
        ObjBox.flush(context, true)
        Prefs.clear()
        FirebaseAuth.getInstance().signOut()
        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().toCoroutine()
        abcLogger.stopAll()
        ABC.stopService(context)
        Notifications.cancelAll(context)
        SyncWorker.requestStop(context)
        onComplete.invoke()
    }

    private suspend fun generalConfig() = configs {
        header {
            title = context.getString(R.string.config_category_general)
        }
        simple {
            title = context.getString(R.string.config_title_user_name)
            description = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown"
        }
        simple {
            title = context.getString(R.string.config_title_email)
            description = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"
        }
        simple {
            key = PrefKeys.PERMISSION
            title = context.getString(R.string.config_title_permission)
            description = if (context.checkPermission(abcLogger.getAllRequiredPermissions())) {
                context.getString(R.string.general_granted)
            } else {
                context.getString(R.string.general_denied)
            }
        }
        simple {
            key = PrefKeys.LAST_TIME_SYNC
            title = context.getString(R.string.config_title_sync)
            description = if (Prefs.lastTimeDataSync > 0) {
                String.format("%s: %s",
                        context.getString(R.string.general_last_sync_time),
                        DateUtils.formatDateTime(
                                context, Prefs.lastTimeDataSync,
                                DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
                        )
                )
            } else {
                context.getString(R.string.general_none)
            }
        }
        switch {
            key = PrefKeys.CAN_UPLOAD_METERED_NETWORK
            title = context.getString(R.string.config_title_upload_setting)
            description = context.getString(R.string.config_desc_upload_setting)
            isChecked = Prefs.canUploadMeteredNetwork
        }
    }

    private suspend fun dataConfig() = configs {
        header {
            title = context.getString(R.string.config_category_collector)
        }
        abcLogger.collectors.forEach { collector ->
            data {
                key = collector.prefKey() ?: ""
                title = collector.nameRes()?.let { context.getString(it) } ?: ""
                description = collector.descriptionRes()?.let { context.getString(it) } ?: ""
                isChecked = collector.getStatus()?.hasStarted ?: false
                isAvailable = collector.checkAvailability()
                info = collector.getStatus()?.toInfo() ?: ""
                intent = collector.newIntentForSetUp
            }
        }
    }

    private suspend fun otherConfig() = configs {
        header {
            title = context.getString(R.string.config_category_others)
        }
        simple {
            key = PrefKeys.MAX_DB_SIZE
            title = context.getString(R.string.config_title_flush_data)
            description = listOf(
                    context.getString(R.string.general_current_db_size),
                    "${Formatter.formatFileSize(context, ObjBox.size(context))} / ${Formatter.formatFileSize(context, ObjBox.maxSizeInBytes())}"
            ).joinToString(": ")
        }
        simple {
            key = PrefKeys.LOGOUT
            title = context.getString(R.string.config_title_logout)
            description = context.getString(R.string.config_desc_logout)
        }
    }
}