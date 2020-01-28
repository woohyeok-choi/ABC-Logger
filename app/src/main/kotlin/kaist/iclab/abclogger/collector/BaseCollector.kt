package kaist.iclab.abclogger.collector

import android.content.Intent
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.activity.ActivityCollector
import kaist.iclab.abclogger.collector.appusage.AppUsageCollector
import kaist.iclab.abclogger.collector.battery.BatteryCollector
import kaist.iclab.abclogger.collector.bluetooth.BluetoothCollector
import kaist.iclab.abclogger.collector.call.CallLogCollector
import kaist.iclab.abclogger.collector.event.DeviceEventCollector
import kaist.iclab.abclogger.collector.externalsensor.polar.PolarH10Collector
import kaist.iclab.abclogger.collector.install.InstalledAppCollector
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.collector.location.LocationCollector
import kaist.iclab.abclogger.collector.media.MediaCollector
import kaist.iclab.abclogger.collector.message.MessageCollector
import kaist.iclab.abclogger.collector.notification.NotificationCollector
import kaist.iclab.abclogger.collector.physicalstatus.PhysicalStatCollector
import kaist.iclab.abclogger.collector.sensor.SensorCollector
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.traffic.DataTrafficCollector
import kaist.iclab.abclogger.collector.wifi.WifiCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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
