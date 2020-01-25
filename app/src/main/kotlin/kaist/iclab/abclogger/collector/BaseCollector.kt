package kaist.iclab.abclogger.collector

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import kaist.iclab.abclogger.ABCException
import kaist.iclab.abclogger.CollectorPrefs
import kaist.iclab.abclogger.R
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
import kaist.iclab.abclogger.collector.physicalstatus.PhysicalStatusCollector
import kaist.iclab.abclogger.collector.sensor.SensorCollector
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.traffic.DataTrafficCollector
import kaist.iclab.abclogger.collector.wifi.WifiCollector


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
}

fun <T : BaseCollector> T.hasStarted() =
        when (this) {
            is ActivityCollector -> CollectorPrefs.hasStartedActivity
            is AppUsageCollector -> CollectorPrefs.hasStartedAppUsage
            is BatteryCollector -> CollectorPrefs.hasStartedBattery
            is BluetoothCollector -> CollectorPrefs.hasStartedBluetooth
            is CallLogCollector -> CollectorPrefs.hasStartedCallLog
            is DataTrafficCollector -> CollectorPrefs.hasStartedDataTraffic
            is DeviceEventCollector -> CollectorPrefs.hasStartedDeviceEvent
            is InstalledAppCollector -> CollectorPrefs.hasStartedInstallApp
            is KeyLogCollector -> CollectorPrefs.hasStartedKeyStrokes
            is LocationCollector -> CollectorPrefs.hasStartedLocation
            is MediaCollector -> CollectorPrefs.hasStartedMediaGeneration
            is MessageCollector -> CollectorPrefs.hasStartedMessage
            is NotificationCollector -> CollectorPrefs.hasStartedNotification
            is PhysicalStatusCollector -> CollectorPrefs.hasStartedPhysicalStatus
            is PolarH10Collector -> CollectorPrefs.hasStartedPolarH10
            is SurveyCollector -> CollectorPrefs.hasStartedSurvey
            is WifiCollector -> CollectorPrefs.hasStartedWiFi
            is SensorCollector -> CollectorPrefs.hasStartedSensor
            else -> false
        }

fun <T : BaseCollector> T.info() =
        when (this) {
            is ActivityCollector -> CollectorPrefs.infoActivity
            is AppUsageCollector -> CollectorPrefs.infoAppUsage
            is BatteryCollector -> CollectorPrefs.infoBattery
            is BluetoothCollector -> CollectorPrefs.infoBluetooth
            is CallLogCollector -> CollectorPrefs.infoCallLog
            is DataTrafficCollector -> CollectorPrefs.infoDataTraffic
            is DeviceEventCollector -> CollectorPrefs.infoDeviceEvent
            is InstalledAppCollector -> CollectorPrefs.infoInstallApp
            is KeyLogCollector -> CollectorPrefs.infoKeyStrokes
            is LocationCollector -> CollectorPrefs.infoLocation
            is MediaCollector -> CollectorPrefs.infoMediaGeneration
            is MessageCollector -> CollectorPrefs.infoMessage
            is NotificationCollector -> CollectorPrefs.infoNotification
            is PhysicalStatusCollector -> CollectorPrefs.infoPhysicalStatus
            is PolarH10Collector -> "Id: ${CollectorPrefs.polarH10DeviceId}; Connected: ${CollectorPrefs.polarH10Connection == PolarH10Collector.POLAR_CONNECTED}; Battery: ${CollectorPrefs.polarH10BatteryLevel}"
            is SurveyCollector -> CollectorPrefs.infoSurvey
            is WifiCollector -> CollectorPrefs.infoWiFi
            is SensorCollector -> {
                val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val hasLightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null
                val hasProximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
                "Light Sensor: $hasLightSensor; Proximity: $hasProximity"
            }
            else -> ""
        }

fun <T : BaseCollector> T.nameRes() = when (this) {
    is ActivityCollector -> R.string.data_name_physical_activity
    is AppUsageCollector -> R.string.data_name_app_usage
    is BatteryCollector -> R.string.data_name_battery
    is BluetoothCollector -> R.string.data_name_bluetooth
    is CallLogCollector -> R.string.data_name_call_log
    is DataTrafficCollector -> R.string.data_name_traffic
    is DeviceEventCollector -> R.string.data_name_device_event
    is InstalledAppCollector -> R.string.data_name_installed_app
    is KeyLogCollector -> R.string.data_name_key_log
    is LocationCollector -> R.string.data_name_location
    is MediaCollector -> R.string.data_name_media
    is MessageCollector -> R.string.data_name_message
    is NotificationCollector -> R.string.data_name_notification
    is PhysicalStatusCollector -> R.string.data_name_physical_status
    is PolarH10Collector -> R.string.data_name_polar_h10
    is SurveyCollector -> R.string.data_name_survey
    is WifiCollector -> R.string.data_name_wifi
    is SensorCollector -> R.string.data_name_sensor
    else -> null
}

fun <T : BaseCollector> T.descriptionRes() = when (this) {
    is ActivityCollector -> R.string.data_desc_physical_activity
    is AppUsageCollector -> R.string.data_desc_app_usage
    is BatteryCollector -> R.string.data_desc_battery
    is BluetoothCollector -> R.string.data_desc_bluetooth
    is CallLogCollector -> R.string.data_desc_call_log
    is DataTrafficCollector -> R.string.data_desc_traffic
    is DeviceEventCollector -> R.string.data_desc_device_event
    is InstalledAppCollector -> R.string.data_desc_installed_app
    is KeyLogCollector -> R.string.data_desc_key_log
    is LocationCollector -> R.string.data_desc_location
    is MediaCollector -> R.string.data_desc_media
    is MessageCollector -> R.string.data_desc_message
    is NotificationCollector -> R.string.data_desc_notification
    is PhysicalStatusCollector -> R.string.data_desc_physical_status
    is PolarH10Collector -> R.string.data_desc_polar_h10
    is SurveyCollector -> R.string.data_desc_survey
    is WifiCollector -> R.string.data_desc_wifi
    is SensorCollector -> R.string.data_desc_sensor
    else -> null
}


suspend fun <T : BaseCollector> T.start(onComplete: ((collector: T, throwable: Throwable?) -> Unit)? = null) = handleState(true, onComplete)

suspend fun <T : BaseCollector> T.stop(onComplete: ((collector: T, throwable: Throwable?) -> Unit)? = null) = handleState(false, onComplete)

private suspend fun <T : BaseCollector> T.handleState(
        state: Boolean,
        onComplete: ((collector: T, throwable: Throwable?) -> Unit)? = null
) {
    var throwable: Throwable? = null

    try {
        if (state) onStart() else onStop()
    } catch (e: Exception) {
        if (state) throwable = e
    }

    if (throwable != null) {
        onComplete?.invoke(this, ABCException.wrap(throwable))
        return
    }

    when (this) {
        is ActivityCollector -> CollectorPrefs.hasStartedActivity = state
        is AppUsageCollector -> CollectorPrefs.hasStartedAppUsage = state
        is BatteryCollector -> CollectorPrefs.hasStartedBattery = state
        is BluetoothCollector -> CollectorPrefs.hasStartedBluetooth = state
        is CallLogCollector -> CollectorPrefs.hasStartedCallLog = state
        is DataTrafficCollector -> CollectorPrefs.hasStartedDataTraffic = state
        is DeviceEventCollector -> CollectorPrefs.hasStartedDeviceEvent = state
        is InstalledAppCollector -> CollectorPrefs.hasStartedInstallApp = state
        is KeyLogCollector -> CollectorPrefs.hasStartedKeyStrokes = state
        is LocationCollector -> CollectorPrefs.hasStartedLocation = state
        is MediaCollector -> CollectorPrefs.hasStartedMediaGeneration = state
        is MessageCollector -> CollectorPrefs.hasStartedMessage = state
        is NotificationCollector -> CollectorPrefs.hasStartedNotification = state
        is PhysicalStatusCollector -> CollectorPrefs.hasStartedPhysicalStatus = state
        is PolarH10Collector -> CollectorPrefs.hasStartedPolarH10 = state
        is SurveyCollector -> CollectorPrefs.hasStartedSurvey = state
        is WifiCollector -> CollectorPrefs.hasStartedWiFi = state
        is SensorCollector -> CollectorPrefs.hasStartedSensor = state
    }

    onComplete?.invoke(this, throwable)
}
