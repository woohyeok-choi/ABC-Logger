package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.AbcCollector
import kaist.iclab.abclogger.commons.Notifications
import kaist.iclab.abclogger.Prefs
import kaist.iclab.abclogger.collector.BaseStatus
import kaist.iclab.abclogger.commons.checkPermission
import kaist.iclab.abclogger.commons.toCoroutine
import kaist.iclab.abclogger.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConfigViewModel(
        private val context: Context,
        private val abc: AbcCollector,
        navigator: ConfigNavigator
) : BaseViewModel<ConfigNavigator>(navigator) {
    val configs: MutableLiveData<ArrayList<ConfigData>> = MutableLiveData()
    override suspend fun onLoad(extras: Bundle?) {
        configs.postValue(arrayListOf<ConfigData>().apply {
            addAll(generalConfig() + dataConfig() + otherConfig())
        })
    }

    override suspend fun onStore() { }

    fun flush() = viewModelScope.launch(Dispatchers.IO) {
        val ntf = Notifications.build(
                context = context,
                channelId = Notifications.CHANNEL_ID_PROGRESS,
                title = context.getString(R.string.ntf_title_flush),
                text = context.getString(R.string.ntf_text_flush),
                progress = 0,
                indeterminate = true
        )
        NotificationManagerCompat.from(context).notify(Notifications.ID_FLUSH_PROGRESS, ntf)
        try {
            ObjBox.flush(context)
        } catch (e: Exception) {
            nav?.navigateError(e)
        }
        NotificationManagerCompat.from(context).cancel(Notifications.ID_FLUSH_PROGRESS)
    }

    fun logout() = viewModelScope.launch(Dispatchers.IO) {
        val ntf = Notifications.build(
                context = context,
                channelId = Notifications.CHANNEL_ID_PROGRESS,
                title = context.getString(R.string.ntf_title_logout),
                text = context.getString(R.string.ntf_text_logout),
                progress = 0,
                indeterminate = true
        )
        Notifications.notify(context, Notifications.ID_FLUSH_PROGRESS, ntf)

        try {
            GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().toCoroutine()
            FirebaseAuth.getInstance().signOut()

            AbcCollector.stop(context)
            abc.stopAll()
            abc.collectors.forEach { it.clear() }
            SyncWorker.requestStop(context)
            Notifications.cancelAll(context)

            ObjBox.flush(context)
            Prefs.clear()
            Notifications.cancel(context, Notifications.ID_FLUSH_PROGRESS)

            nav?.navigateAfterLogout()
        } catch (e: Exception) {
            Notifications.cancel(context, Notifications.ID_FLUSH_PROGRESS)

            nav?.navigateError(e)
        }
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
            title = context.getString(R.string.config_title_permission)
            description = if (context.checkPermission(abc.getAllRequiredPermissions())) {
                context.getString(R.string.general_granted)
            } else {
                context.getString(R.string.general_denied)
            }
            onAction = {
                nav?.navigateIntent(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:${context.packageName}"))
                )
            }
        }
        simple {
            title = context.getString(R.string.config_title_sync)
            description = String.format("%s: %s",
                    context.getString(R.string.general_last_sync_time),
                    if (Prefs.lastTimeDataSync > 0) {
                        DateUtils.formatDateTime(context, Prefs.lastTimeDataSync, DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
                    } else {
                        context.getString(R.string.general_none)
                    }
            )
            onAction = {
                SyncWorker.requestStart(
                        context = context,
                        forceStart = true,
                        enableMetered = Prefs.canUploadMeteredNetwork,
                        isPeriodic = Prefs.isAutoSync
                )
            }
        }
        switch {
            title = context.getString(R.string.config_title_auto_sync)
            description = context.getString(R.string.config_desc_auto_sync)
            isChecked = Prefs.isAutoSync
            onChange = { isChecked ->
                Prefs.isAutoSync = isChecked
                SyncWorker.requestStart(
                        context = context,
                        forceStart = false,
                        enableMetered = Prefs.canUploadMeteredNetwork,
                        isPeriodic = Prefs.isAutoSync
                )
            }
        }
        switch {
            title = context.getString(R.string.config_title_network_constraints)
            description = context.getString(R.string.config_desc_network_constraints)
            isChecked = Prefs.canUploadMeteredNetwork
            onChange = { isChecked ->
                Prefs.canUploadMeteredNetwork = isChecked
                SyncWorker.requestStart(
                        context = context,
                        forceStart = false,
                        enableMetered = Prefs.canUploadMeteredNetwork,
                        isPeriodic = Prefs.isAutoSync
                )
            }
        }
    }

    private suspend fun dataConfig() = configs {
        header {
            title = context.getString(R.string.config_category_collector)
        }

        abc.collectors.forEach { collector ->
            data {
                title = collector.name
                description = collector.description
                isChecked = collector.getStatus()?.hasStarted ?: false
                isAvailable = collector.checkAvailability()
                info = BaseStatus.information(collector.getStatus())
                collector.newIntentForSetUp?.let {
                    onAction = { nav?.navigateIntent(it) }
                }
                onChange = { isChecked ->
                    if (isChecked)
                        collector.start { t -> t?.let { nav?.navigateError(t) } }
                    else
                        collector.stop { t -> t?.let { nav?.navigateError(t) } }
                }
            }
        }
    }

    private suspend fun otherConfig() = configs {
        header {
            title = context.getString(R.string.config_category_others)
        }
        simple {
            title = context.getString(R.string.config_title_flush_data)
            description = listOf(
                    context.getString(R.string.general_current_db_size),
                    "${Formatter.formatFileSize(context, ObjBox.size(context))} / ${Formatter.formatFileSize(context, ObjBox.maxSizeInBytes())}"
            ).joinToString(": ")
            onAction = {
                nav?.navigateBeforeFlush()
            }
        }
        simple {
            title = context.getString(R.string.config_title_logout)
            description = context.getString(R.string.config_desc_logout)
            onAction = {
                nav?.navigateBeforeLogout()
            }
        }
    }
}