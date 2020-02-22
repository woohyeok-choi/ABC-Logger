package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.BaseStatus
import kaist.iclab.abclogger.commons.Notifications
import kaist.iclab.abclogger.commons.checkPermission
import kaist.iclab.abclogger.commons.toCoroutine
import kaist.iclab.abclogger.ui.Status
import kaist.iclab.abclogger.ui.base.BaseViewModel

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

    override suspend fun onStore() {}

    fun flush() = launch {
        storeStatus.postValue(Status.loading())
        try {
            io { ObjBox.flush(context) }
            storeStatus.postValue(Status.success())
        } catch (e: Exception) {
            ui { nav?.navigateError(e) }
            storeStatus.postValue(Status.failure(e))
        }
    }

    fun logout() = launch {
        storeStatus.postValue(Status.loading())
        try {
            io {
                GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().toCoroutine()
                FirebaseAuth.getInstance().signOut()

                AbcCollector.stop(context)
                abc.stopAll()
                abc.collectors.forEach { it.clear() }
                SyncWorker.requestStop(context)
                Notifications.cancelAll(context)

                ObjBox.flush(context)
                Prefs.clear()
            }
            ui { nav?.navigateAfterLogout() }
            storeStatus.postValue(Status.success())
        } catch (e: Exception) {
            ui { nav?.navigateError(e) }
            storeStatus.postValue(Status.failure(e))
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
                launch {
                    ui {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${context.packageName}"))
                        nav?.navigateIntent(intent)
                    }
                }
            }
        }
        simple {
            title = context.getString(R.string.config_title_sync)
            description = context.getString(
                    R.string.general_last_sync_time,
                    if (Prefs.lastTimeDataSync > 0) {
                        DateUtils.formatDateTime(context, Prefs.lastTimeDataSync, DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
                    } else {
                        context.getString(R.string.general_unknown)
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
                    onAction = {
                        launch {
                            ui { nav?.navigateIntent(it) }
                        }
                    }
                }
                onChange = { isChecked ->
                    if (isChecked) collector.start() else collector.stop()
                }
            }
        }
    }

    private suspend fun otherConfig() = configs {
        header {
            title = context.getString(R.string.config_category_others)
        }
        simple {
            title = context.getString(R.string.config_title_version)
            description = BuildConfig.VERSION_NAME
        }
        simple {
            title = context.getString(R.string.config_title_flush_data)
            description = context.getString(
                    R.string.general_current_db_size,
                    Formatter.formatFileSize(context, ObjBox.size(context)),
                    Formatter.formatFileSize(context, ObjBox.maxSizeInBytes())
            )
            onAction = {
                launch {
                    ui { nav?.navigateBeforeFlush() }
                }

            }
        }
        simple {
            title = context.getString(R.string.config_title_logout)
            description = context.getString(R.string.config_desc_logout)
            onAction = {
                launch {
                    ui { nav?.navigateBeforeLogout() }
                }
            }
        }
    }
}