package kaist.iclab.abclogger.collector

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

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
    val requiredPermissions : List<String>

    /**
     * Intent to make this collector available;
     * for example, to collect notifications, ABC Logger needs a user's manual setting.
     * This function is used to start an activity for the setting.
     */
    val newIntentForSetUp: Intent?

    val nameRes: Int?

    val descriptionRes: Int?

    val statusString : LiveData<String>
}