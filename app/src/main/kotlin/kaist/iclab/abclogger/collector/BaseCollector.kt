package kaist.iclab.abclogger.collector

import android.content.Intent
import androidx.lifecycle.LiveData
import kaist.iclab.abclogger.Base
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.background.Status
import kaist.iclab.abclogger.common.util.PermissionUtils
import kaist.iclab.abclogger.fillBaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Default interface for daata collector
 */
interface BaseCollector {
    /**
     * Start a given collector
     */
    fun start()

    /**
     * Stop a given collector
     */
    fun stop()

    /**
     * Check whether a given collector can operate (e.g., permissions)
     */
    fun checkAvailability() : Boolean

    /**
     * List of permissions (Manifest.permissions.XXX) for this collector.
     */
    fun getRequiredPermissions() : List<String>

    /**
     * Intent to make this collector available;
     * for example, to collect notifications, ABC Logger needs a user's manual setting.
     * This function is used to start an activity for the setting.
     */
    fun newIntentForSetup(): Intent?
}