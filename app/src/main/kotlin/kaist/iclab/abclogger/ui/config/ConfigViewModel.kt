package kaist.iclab.abclogger.ui.config

import android.app.Application
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.BaseObservable
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.*
import kaist.iclab.abclogger.base.BaseViewModel
import android.text.format.Formatter as FileSizeFormatter

class ConfigViewModel(
        private val collectorRepository: CollectorRepository,
        private val dataRepository: DataRepository,
        private val authRepository: AuthRepository,
        application: Application
) : BaseViewModel(application) {

    fun getConfig() = liveData {
        config {
            category(getString(R.string.config_category_general)) {
                readOnly<Unit>(getString(R.string.config_user_name)) {
                    format = {
                        val name = authRepository.signedName()
                                ?: getString(R.string.general_unknown)
                        val email = authRepository.signedEmail()
                                ?: getString(R.string.general_unknown)
                        "$name ($email)"
                    }
                }
                readOnly<String>(getString(R.string.config_app_version)) {
                    format = {
                        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    }
                }
                activity<Boolean, Array<String>, Map<String, Boolean>>(getString(R.string.config_permission)) {
                    value = isPermissionGranted(getApplication(), collectorRepository.permissions)
                    input = collectorRepository.permissions.toTypedArray()
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                    format = { isGranted ->
                        if (isGranted == true) {
                            getString(R.string.config_permission_text_granted)
                        } else {
                            getString(R.string.config_permission_text_denied)
                        }
                    }
                }
                activity<Boolean, Intent, ActivityResult>(getString(R.string.config_whitelist)) {
                    value = collectorRepository.isIgnoringBatteryOptimization(getApplication())
                    contract = ActivityResultContracts.StartActivityForResult()
                    input = collectorRepository.batteryOptimizationIgnoreIntent
                    format = { isGranted ->
                        if (isGranted == true) {
                            getString(R.string.config_whitelist_text_granted)
                        } else {
                            getString(R.string.config_whitelist_text_denied)
                        }
                    }
                }
            }
            category(getString(R.string.config_category_sync)) {
                actionable<Long>(getString(R.string.config_sync_status)) {
                    value = Preference.lastTimeDataSync
                    format = { value ->
                        getString(R.string.config_sync_status_text_last_sync_time,
                                Formatter.formatDateTime(value ?: Long.MIN_VALUE))
                    }
                    action = {
                        launch { SyncRepository.sync(getApplication()) }
                    }
                }
                boolean(getString(R.string.config_sync_via_only_wifi)) {
                    value = Preference.syncOnlyWifi
                    format = { value ->
                        if (value == true) {
                            getString(R.string.config_sync_via_only_wifi_text_only_wifi)
                        } else {
                            getString(R.string.config_sync_via_only_wifi_text_any_network)
                        }
                    }
                    onChange = {
                        Preference.syncOnlyWifi = it ?: false
                    }
                }
                actionable<String>(getString(R.string.config_sync_cancel)) {
                    value = getString(R.string.config_sync_cancel_text)
                    action = { SyncRepository.cancel(getApplication()) }
                }
            }
            category(getString(R.string.config_category_collector)) {
                collectorRepository.collectors.forEach { collector ->
                    collector {
                        value = collector
                        format = { collector ->
                            val isTurnedOn = collector?.isEnabled == true
                            val hasNoError = collector?.lastErrorMessage.isNullOrBlank()

                            when {
                                isTurnedOn && hasNoError -> getString(R.string.config_collector_status_text_normal_operating)
                                isTurnedOn && !hasNoError -> getString(R.string.config_collector_status_text_error)
                                else -> getString(R.string.config_collector_status_text_no_operating)
                            }
                        }
                    }
                }
            }
            category(getString(R.string.config_category_data)) {
                /**
                 * TODO: Is this works?
                 */
                actionable<Pair<Long, Long>>(getString(R.string.config_data_size)) {
                    fun getValue() = launch { value = dataRepository.sizeOnDiskInBytes() to dataRepository.maxSizeInBytes() }

                    getValue()

                    format = { value ->
                        value?.let { (curSize, maxSize) ->
                            val curSizeStr = FileSizeFormatter.formatShortFileSize(getApplication(), curSize)
                            val maxSizeStr = FileSizeFormatter.formatShortFileSize(getApplication(), maxSize)
                            getString(R.string.config_data_size_text, curSizeStr, maxSizeStr)
                        }
                    }
                    action = { getValue() }
                }
                actionable<Unit>(getString(R.string.config_data_flush)) {
                    format = {
                        getString(R.string.config_data_flush_text)
                    }
                    dialogMessage = getString(R.string.config_data_flush_dialog)
                    action = {
                        launch { io { dataRepository.flush() } }
                    }
                }
            }
            category(getString(R.string.config_category_others)) {
                activity<Unit, Intent, ActivityResult>(getString(R.string.config_others_setting)) {
                    format = {
                        getString(R.string.config_others_setting_text)
                    }
                    input = collectorRepository.settingIntent
                    contract = ActivityResultContracts.StartActivityForResult()
                }
                actionable<Unit>(getString(R.string.config_others_sign_out)) {
                    format = {
                        getString(R.string.config_others_sign_out_text)
                    }
                    dialogMessage = getString(R.string.config_others_sign_out_dialog)
                    action = {
                        launch {
                            io {
                                /**
                                 * TODO: Cancel all
                                 */

                                GoogleSignIn.getClient(getApplication(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().toCoroutine()
                                FirebaseAuth.getInstance().signOut()
                                collectorRepository.stop()
                                collectorRepository.collectors.forEach { it.clear() }
                                //Notifications.cancelAll()
                            }
                        }
                    }
                }
            }
        }
    }

    fun signOut() = liveData {

    }

    private fun getString(res: Int) = getApplication<Application>().getString(res)

    private fun getString(res: Int, vararg formats: Any) = getApplication<Application>().getString(res, *formats)
}