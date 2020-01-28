package kaist.iclab.abclogger.collector

import android.content.Intent


/**
 * Default interface for data collector
 */
interface BaseCollector {
    /**
     * Define operations when a user requests for starting this collector.
     * This function should be called in non-UI thread.
     */
    suspend fun onStart()

    /**
     * Define operations when a user requests for stopping this collector.
     * This function should be called in non-UI thread.
     */
    suspend fun onStop()

    /**
     * Check whether a given collector can operate (e.g., permissions).
     * If not available (even after started), it will be stopped.
     */
    suspend fun checkAvailability() : Boolean

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
}
