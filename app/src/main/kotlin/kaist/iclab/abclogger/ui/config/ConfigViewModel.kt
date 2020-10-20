package kaist.iclab.abclogger.ui.config

import android.app.Application
import android.content.Intent
import android.text.format.DateUtils
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.SavedStateHandle
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.commons.CollectorError
import kaist.iclab.abclogger.core.*
import kaist.iclab.abclogger.ui.base.BaseViewModel
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.commons.Formatter
import kaist.iclab.abclogger.commons.isPermissionGranted
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.Status
import kaist.iclab.abclogger.core.sync.SyncRepository
import kaist.iclab.abclogger.structure.config.Config
import kaist.iclab.abclogger.structure.config.config
import kaist.iclab.abclogger.ui.splash.SplashActivity
import kaist.iclab.abclogger.view.StatusColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import android.text.format.Formatter as FileSizeFormatter


class ConfigViewModel(
    private val collectorRepository: CollectorRepository,
    private val dataRepository: DataRepository,
    savedStateHandle: SavedStateHandle,
    application: Application
) : BaseViewModel(savedStateHandle, application) {
    fun getConfig() = flow { emit(buildConfig()) }.flowOn(Dispatchers.IO)

    fun getCollectorConfig(qualifiedName: String) = flow {
        emit(buildCollectorConfig(collectorRepository[qualifiedName]))
    }.flowOn(Dispatchers.IO)

    private suspend fun buildConfig(): Config {
        val currentTime = System.currentTimeMillis()
        val sizeDb = dataRepository.sizeOnDisk()

        return config {
            category(name = str(R.string.config_category_general)) {
                readonly(name = str(R.string.config_general_user_name_title)) {
                    format = {
                        val name = AuthRepository.name.takeUnless { it.isBlank() }
                            ?: str(R.string.general_mdash)
                        val email = AuthRepository.email.takeUnless { it.isBlank() }
                            ?: str(R.string.general_mdash)
                        "$name ($email)"
                    }
                }

                text(
                    name = str(R.string.config_general_group_name_title),
                    default = AuthRepository.groupName
                ) {
                    format = { it }
                    onAfterChange = { AuthRepository.groupName = value }
                }

                activity<Boolean, Array<String>, Map<String, Boolean>>(
                    name = str(R.string.config_general_permission_title),
                    default = isPermissionGranted(
                        getApplication(),
                        collectorRepository.permissions
                    ),
                    input = collectorRepository.permissions.toTypedArray(),
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    transform = {
                        isPermissionGranted(
                            getApplication(),
                            collectorRepository.permissions
                        )
                    }
                ) {
                    format = { value ->
                        str(if (value) R.string.config_general_permission_text_granted else R.string.config_general_permission_text_denied)
                    }
                    color = { value ->
                        if (value) StatusColor.NORMAL else StatusColor.ERROR
                    }
                }

                activity<Boolean, Intent, ActivityResult>(
                    name = str(R.string.config_general_battery_optimization_ignore_title),
                    default = CollectorRepository.isBatteryOptimizationIgnored(getApplication()),
                    contract = ActivityResultContracts.StartActivityForResult(),
                    input = CollectorRepository.getIgnoreBatteryOptimizationIntent(getApplication()),
                    transform = { CollectorRepository.isBatteryOptimizationIgnored(getApplication()) },
                ) {
                    format = { value ->
                        str(if (value) R.string.config_general_battery_optimization_ignore_text_ignored else R.string.config_general_battery_optimization_ignore_text_not_ignored)
                    }
                    color = { value ->
                        if (value) StatusColor.NORMAL else StatusColor.ERROR
                    }
                }

                actionable(
                    name = str(R.string.config_general_data_size_title),
                    default = sizeDb
                ) {
                    format = { size ->
                        FileSizeFormatter.formatShortFileSize(getApplication(), size)
                    }
                    confirmMessage = str(R.string.config_general_data_size_dialog)
                    action = {
                        launchIo {
                            dataRepository.flush()
                            value = dataRepository.sizeOnDisk()
                        }
                    }
                }
            }

            category(name = str(R.string.config_category_sync)) {
                actionable(
                    name = str(R.string.config_sync_status_title),
                    default = Preference.lastTimeDataSync,
                ) {
                    format = { value ->
                        if (value > 0) {
                            str(
                                R.string.config_sync_status_text_normal,
                                dateTime(value, currentTime),
                                timeSpan(value, currentTime)
                            )
                        } else {
                            str(R.string.config_sync_status_text_none)
                        }
                    }
                    action = {
                        launchUi {
                            SyncRepository.syncNow(getApplication(), Preference.isAutoSync)
                                ?.collectLatest {
                                    value = Preference.lastTimeDataSync
                                }
                        }
                    }
                }

                radio(
                    name = str(R.string.config_sync_network_title),
                    default = Preference.isSyncableWithWifiOnly,
                    options = arrayOf(true, false)
                ) {
                    format = { value ->
                        str(if (value) R.string.config_sync_network_text_wifi else R.string.config_sync_network_text_any)
                    }
                    formatOption = { value ->
                        str(if (value) R.string.config_sync_network_option_wifi else R.string.config_sync_network_option_any)
                    }
                    onAfterChange = { Preference.isSyncableWithWifiOnly = value }
                }

                radio(
                    name = str(R.string.config_sync_auto_sync_title),
                    default = Preference.isAutoSync,
                    options = arrayOf(true, false)
                ) {
                    format = { value ->
                        str(if (value) R.string.config_sync_auto_sync_text_auto else R.string.config_sync_auto_sync_text_manual)
                    }
                    formatOption = { value ->
                        str(if (value) R.string.config_sync_auto_sync_option_auto else R.string.config_sync_auto_sync_option_manual)
                    }
                    onAfterChange = {
                        launchIo {
                            Preference.isAutoSync = value
                            SyncRepository.setAutoSync(getApplication(), value)
                        }
                    }
                }
            }

            category(name = str(R.string.config_category_collector)) {
                collectorRepository.all.forEach { collector ->
                    collector(
                        name = collector.name,
                        status = collector.getStatus(),
                        qualifiedName = collector.qualifiedName
                    ) {
                        description = collector.description
                        color = { value ->
                            when (value) {
                                Status.On -> StatusColor.NORMAL
                                Status.Off -> StatusColor.NONE
                                is Status.Error -> StatusColor.ERROR
                            }
                        }
                        format = { value ->
                            when (value) {
                                Status.On -> str(R.string.config_collector_status_text_on)
                                Status.Off -> str(R.string.config_collector_status_text_off)
                                is Status.Error -> str(R.string.config_collector_status_text_error)
                            }
                        }
                    }
                }
            }

            category(name = str(R.string.config_category_others)) {
                readonly(name = str(R.string.config_others_version_title)) {
                    format = {
                        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    }
                }
                activity<Unit, Intent, ActivityResult>(
                    name = str(R.string.config_others_setting_title),
                    default = Unit,
                    input = CollectorRepository.getSettingIntent(getApplication()),
                    contract = ActivityResultContracts.StartActivityForResult(),
                    transform = { }
                ) {
                    format = {
                        str(R.string.config_others_setting_text)
                    }
                }

                activity<Unit, Intent, ActivityResult>(
                    name = str(R.string.config_others_ntf_management_title),
                    default = Unit,
                    contract = ActivityResultContracts.StartActivityForResult(),
                    input = NotificationRepository.getSettingIntent(getApplication()),
                    transform = { }
                ) {
                    format = {
                        str(R.string.config_others_ntf_management_text)
                    }
                }

                activity<Unit, Intent, ActivityResult>(
                    name = str(R.string.config_others_sign_out_title),
                    default = Unit,
                    contract = ActivityResultContracts.StartActivityForResult(),
                    input = Intent(getApplication(), SplashActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(SplashActivity.EXTRA_SIGN_OUT, true),
                    transform = { }
                ) {
                    format = {
                        str(R.string.config_others_sign_out_text)
                    }
                    confirmMessage = str(R.string.config_others_sign_out_dialog)
                }
            }
        }
    }

    private suspend fun <T : AbstractCollector<*>> buildCollectorConfig(collector: T?): Config {
        if (collector == null) return Config()

        val currentTime = System.currentTimeMillis()
        val nRecords = collector.count()

        return config {
            category(name = str(R.string.sub_config_category_collector_general)) {
                radio(
                    name = str(R.string.sub_config_collector_general_status_title),
                    default = collector.getStatus(),
                    options = arrayOf(Status.On, Status.Off)
                ) {
                    formatOption = { value ->
                        if (value == Status.On) {
                            str(R.string.sub_config_collector_general_status_dialog_on)
                        } else str(
                            R.string.sub_config_collector_general_status_dialog_off
                        )
                    }
                    format = { value ->
                        when (value) {
                            Status.On -> str(
                                R.string.sub_config_collector_general_status_text_on,
                                dateTime(collector.turnedOnTime, currentTime),
                                timeSpan(collector.turnedOnTime, currentTime)
                            )
                            Status.Off -> str(R.string.sub_config_collector_general_status_text_off)
                            is Status.Error -> str(
                                R.string.sub_config_collector_general_status_text_error,
                                value.message ?: str(R.string.general_unknown)
                            )
                        }
                    }
                    color = { value ->
                        when (value) {
                            Status.On -> StatusColor.NORMAL
                            Status.Off -> StatusColor.NONE
                            is Status.Error -> StatusColor.ERROR
                        }
                    }
                    onAfterChange = {
                        if (value == Status.On) {
                            collector.start()
                        } else if (value == Status.Off) {
                            collector.stop()
                        }
                        launchIo {
                            collector.statusFlow.collectLatest {
                                value = it
                            }
                        }
                    }
                }

                activity<Boolean, Array<String>, Map<String, Boolean>>(
                    name = str(R.string.sub_config_collector_general_permission_title),
                    default = isPermissionGranted(getApplication(), collector.permissions),
                    input = collector.permissions.toTypedArray(),
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    transform = { isPermissionGranted(getApplication(), collector.permissions) }
                ) {
                    format = { value ->
                        str(if (value) R.string.sub_config_collector_general_permission_text_granted else R.string.sub_config_collector_general_permission_text_denied)
                    }
                    color = { value ->
                        if (value) StatusColor.NORMAL else StatusColor.ERROR
                    }
                }

                activity<Boolean, Intent?, ActivityResult>(
                    name = str(R.string.sub_config_collector_general_availability_title),
                    default = collector.isAvailable(),
                    input = collector.setupIntent,
                    contract = ActivityResultContracts.StartActivityForResult(),
                    transform = { collector.isAvailable() }
                ) {
                    format = { value ->
                        str(if (value) R.string.sub_config_collector_general_availability_text_available else R.string.sub_config_collector_general_availability_text_unavailable)
                    }
                    color = { value ->
                        if (value) StatusColor.NORMAL else StatusColor.ERROR
                    }
                    onBeforeChange = {
                        if (collector.getStatus() != Status.Off) throw CollectorError.changeSettingDuringOperating(
                            collector.qualifiedName
                        )
                    }
                }
            }

            category(name = str(R.string.sub_config_category_collector_data)) {
                readonly(name = str(R.string.sub_config_collector_data_last_written_time_title)) {
                    format = {
                        if (collector.lastTimeDataWritten > 0) {
                            str(
                                R.string.sub_config_collector_data_last_written_time_text_written,
                                dateTime(collector.lastTimeDataWritten, currentTime),
                                timeSpan(collector.lastTimeDataWritten, currentTime)
                            )
                        } else {
                            str(R.string.sub_config_collector_data_last_written_time_text_none)
                        }
                    }
                }

                readonly(name = str(R.string.sub_config_collector_data_records_collected_title)) {
                    format = {
                        val compactNumber =
                            Formatter.formatCompactNumber(nRecords)
                        str(
                            R.string.sub_config_collector_data_records_collected_text,
                            compactNumber
                        )
                    }
                }

                readonly(name = str(R.string.sub_config_collector_data_records_uploaded_title)) {
                    format = {
                        val compactNumber =
                            Formatter.formatCompactNumber(collector.recordsUploaded)
                        str(R.string.sub_config_collector_data_records_uploaded_text, compactNumber)
                    }
                }
            }

            if (collector.getDescription().isNotEmpty()) {
                category(name = str(R.string.sub_config_category_collector_others)) {
                    collector.getDescription().forEach { info ->
                        readonly(name = str(info.stringRes)) {
                            format = { info.value.toString() }
                        }
                    }
                }
            }
        }
    }

    private fun str(res: Int) = getApplication<Application>().getString(res)

    private fun str(res: Int, vararg formats: Any) =
        getApplication<Application>().getString(res, *formats)

    private fun dateTime(then: Long, now: Long) =
        if (then > 0) Formatter.formatSameDateTime(
            getApplication(),
            then,
            now
        ) else str(R.string.general_mdash)

    private fun timeSpan(then: Long, now: Long) = if (then > 0)
        DateUtils.getRelativeTimeSpanString(
            then,
            now,
            DateUtils.MINUTE_IN_MILLIS
        ) else str(R.string.general_mdash)


}