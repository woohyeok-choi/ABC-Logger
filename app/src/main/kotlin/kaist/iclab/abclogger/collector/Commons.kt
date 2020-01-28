package kaist.iclab.abclogger.collector

import android.content.ContentResolver
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.activity.ActivityCollector
import kaist.iclab.abclogger.collector.appusage.AppUsageCollector
import kaist.iclab.abclogger.collector.battery.BatteryCollector
import kaist.iclab.abclogger.collector.bluetooth.BluetoothCollector
import kaist.iclab.abclogger.collector.call.CallLogCollector
import kaist.iclab.abclogger.collector.call.CallLogEntity
import kaist.iclab.abclogger.collector.event.DeviceEventCollector
import kaist.iclab.abclogger.collector.externalsensor.polar.PolarH10Collector
import kaist.iclab.abclogger.collector.install.InstalledAppCollector
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.collector.location.LocationCollector
import kaist.iclab.abclogger.collector.media.MediaCollector
import kaist.iclab.abclogger.collector.message.MessageCollector
import kaist.iclab.abclogger.collector.message.MessageEntity
import kaist.iclab.abclogger.collector.notification.NotificationCollector
import kaist.iclab.abclogger.collector.physicalstatus.PhysicalStatCollector
import kaist.iclab.abclogger.collector.sensor.SensorCollector
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.traffic.DataTrafficCollector
import kaist.iclab.abclogger.collector.wifi.WifiCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.*
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

fun <T> getRecentContents(contentResolver: ContentResolver,
                          uri: Uri, timeColumn: String, columns: Array<String>,
                          lastTime: Long = -1, transform: (cursor: Cursor) -> T): List<T>? {
    return contentResolver.query(
            uri, columns, "$timeColumn > ?", arrayOf(lastTime.toString()), "$timeColumn ASC"
    ).use { cursor ->
        val entities = mutableListOf<T>()
        while (cursor?.moveToNext() == true) {
            entities.add(transform(cursor))
        }
        return@use if (entities.isEmpty()) null else entities
    }
}

fun getRecentContents(contentResolver: ContentResolver,
                      uri: Uri, timeColumn: String, columns: Array<String>,
                      lastTime: Long = -1): Cursor? {
    return contentResolver.query(
            uri, columns, "$timeColumn > ?", arrayOf(lastTime.toString()), "$timeColumn ASC"
    )
}

fun contactTypeToString(typeInt: Int?): String? = when (typeInt) {
    ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT -> "ASSISTANT"
    ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK -> "CALLBACK"
    ContactsContract.CommonDataKinds.Phone.TYPE_CAR -> "CAR"
    ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN -> "COMPANY_MAIN"
    ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "FAX_HOME"
    ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "FAX_WORK"
    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "HOME"
    ContactsContract.CommonDataKinds.Phone.TYPE_ISDN -> "ISDN"
    ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "MAIN"
    ContactsContract.CommonDataKinds.Phone.TYPE_MMS -> "MMS"
    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "MOBILE"
    ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "OTHER"
    ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX -> "OTHER_FAX"
    ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "PAGER"
    ContactsContract.CommonDataKinds.Phone.TYPE_RADIO -> "RADIO"
    ContactsContract.CommonDataKinds.Phone.TYPE_TELEX -> "TELEX"
    ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD -> "TTY_TDD"
    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "WORK"
    ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "WORK_MOBILE"
    ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER -> "WORK_PAGER"
    else -> null
}

fun toHash(input: String, start: Int = 0, end: Int = input.length, algorithm: String = "MD5"): String {
    if (input.isEmpty()) return input

    val safeStart = max(0, start)
    val safeEnd = min(input.length, end)

    val subString = input.substring(safeEnd, input.length - 1).toByteArray()
    val bytes = MessageDigest.getInstance(algorithm).digest(subString)
    println(input.substring(0, if (safeStart < 1) 0 else safeStart - 1))

    return input.substring(safeStart, safeEnd) + "\$" +
            bytes.joinToString(separator = "", transform = {
                it.toInt().and(0xFF).toString(16).padStart(2, '0')
            })
}


fun <T : Base> T.fillContact(number: String?, contentResolver: ContentResolver): T {
    if (number == null) return this

    contentResolver.query(
            Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(number)),
            arrayOf(ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL,
                    ContactsContract.CommonDataKinds.Phone.STARRED,
                    ContactsContract.CommonDataKinds.Phone.PINNED
            ),
            null, null, null
    ).use { cursor ->
        if (cursor?.moveToFirst() == true) {
            when (this) {
                is MessageEntity -> this.apply {
                    contactType = contactTypeToString(cursor.getIntOrNull(0)) ?: cursor.getStringOrNull(1) ?: "UNKNOWN"
                    isStarred = cursor.getInt(2) == 1
                    isPinned = cursor.getInt(3) == 1
                }
                is CallLogEntity -> this.apply {
                    contactType = contactTypeToString(cursor.getIntOrNull(0)) ?: cursor.getStringOrNull(1)  ?: "UNKNOWN"
                    isStarred = cursor.getInt(2) == 1
                    isPinned = cursor.getInt(3) == 1
                }
            }
        }
        return this
    }
}

fun countNumDigits(num: Long): Int {
    return when (num) {
        in Long.MIN_VALUE until 0 -> -1
        0L -> 1
        else -> (log10(num.toDouble()) + 1).toInt()
    }
}

fun toMillis(timestamp: Long): Long {
    val curMillis = System.currentTimeMillis()
    return if (countNumDigits(timestamp) == countNumDigits(curMillis)) {
        timestamp
    } else {
        timestamp * 1000
    }
}

fun getApplicationName(packageManager: PackageManager, packageName: String?): String? {
    packageName ?: return null

    return try {
        packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
        ).let {
            packageManager.getApplicationLabel(it).toString()
        }
    } catch (e: Exception) {
        null
    }
}

fun <T : Base> T.fill(timeMillis: Long): T {
    timestamp = timeMillis
    utcOffset = TimeZone.getDefault().rawOffset.toFloat() / (1000 * 60 * 60)
    subjectEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    deviceInfo = "${Build.MANUFACTURER}-${Build.MODEL}-${Build.VERSION.RELEASE}"
    isUploaded = false

    return this
}

fun isSystemApp(packageManager: PackageManager, packageName: String?): Boolean {
    packageName ?: return false
    return try {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).let {
            it.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM
        }
    } catch (e: Exception) {
        false
    }
}


fun isUpdatedSystemApp(packageManager: PackageManager, packageName: String?): Boolean {
    packageName ?: return false
    return try {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).let {
            it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        }
    } catch (e: Exception) {
        false
    }
}

@Suppress("UNCHECKED_CAST")
suspend inline fun <T : BaseCollector, reified V : BaseStatus> T.setStatus(newStatus: V) {
    val collector = this
    val oldStatus = getStatus() as? V
    val mergedStatus = oldStatus merge newStatus

    withContext(Dispatchers.IO) {
        when (collector) {
            is ActivityCollector -> Prefs.statusActivity = mergedStatus as? ActivityCollector.Status
            is AppUsageCollector -> Prefs.statusAppUsage = mergedStatus as? AppUsageCollector.Status
            is BatteryCollector -> Prefs.statusBattery = mergedStatus as? BatteryCollector.Status
            is BluetoothCollector -> Prefs.statusBluetooth = mergedStatus as? BluetoothCollector.Status
            is CallLogCollector -> Prefs.statusCallLog = mergedStatus as? CallLogCollector.Status
            is DataTrafficCollector -> Prefs.statusDataTraffic = mergedStatus as? DataTrafficCollector.Status
            is DeviceEventCollector -> Prefs.statusDeviceEvent = mergedStatus as? DeviceEventCollector.Status
            is InstalledAppCollector -> Prefs.statusInstallApp = mergedStatus as? InstalledAppCollector.Status
            is KeyLogCollector -> Prefs.statusKeyLog = mergedStatus as? KeyLogCollector.Status
            is LocationCollector -> Prefs.statusLocation = mergedStatus as? LocationCollector.Status
            is MediaCollector -> Prefs.statusMedia = mergedStatus as? MediaCollector.Status
            is MessageCollector -> Prefs.statusMessage = mergedStatus as? MessageCollector.Status
            is NotificationCollector -> Prefs.statusNotification = mergedStatus as? NotificationCollector.Status
            is PhysicalStatCollector -> Prefs.statusPhysicalStat = mergedStatus as? PhysicalStatCollector.Status
            is PolarH10Collector -> Prefs.statusPolarH10 = mergedStatus as? PolarH10Collector.Status
            is SurveyCollector -> Prefs.statusSurvey = mergedStatus as? SurveyCollector.Status
            is WifiCollector -> Prefs.statusWiFi = mergedStatus as? WifiCollector.Status
            is SensorCollector -> Prefs.statusSensor = mergedStatus as? SensorCollector.Status
        }
    }
}

suspend fun <T : BaseCollector> T.getStatus() : BaseStatus? {
    val collector = this

    return withContext(Dispatchers.IO) {
        when (collector) {
            is ActivityCollector -> Prefs.statusActivity
            is AppUsageCollector -> Prefs.statusAppUsage
            is BatteryCollector -> Prefs.statusBattery
            is BluetoothCollector -> Prefs.statusBluetooth
            is CallLogCollector -> Prefs.statusCallLog
            is DataTrafficCollector -> Prefs.statusDataTraffic
            is DeviceEventCollector -> Prefs.statusDeviceEvent
            is InstalledAppCollector -> Prefs.statusInstallApp
            is KeyLogCollector -> Prefs.statusKeyLog
            is LocationCollector -> Prefs.statusLocation
            is MediaCollector -> Prefs.statusMedia
            is MessageCollector -> Prefs.statusMessage
            is NotificationCollector -> Prefs.statusNotification
            is PhysicalStatCollector -> Prefs.statusPhysicalStat
            is PolarH10Collector -> Prefs.statusPolarH10
            is SurveyCollector -> Prefs.statusSurvey
            is WifiCollector -> Prefs.statusWiFi
            is SensorCollector -> Prefs.statusSensor
            else -> null
        }
    }
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
    is PhysicalStatCollector -> R.string.data_name_physical_status
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
    is PhysicalStatCollector -> R.string.data_desc_physical_status
    is PolarH10Collector -> R.string.data_desc_polar_h10
    is SurveyCollector -> R.string.data_desc_survey
    is WifiCollector -> R.string.data_desc_wifi
    is SensorCollector -> R.string.data_desc_sensor
    else -> null
}

fun <T : BaseCollector> T.prefKey() = when (this) {
    is ActivityCollector -> PrefKeys.STATUS_ACTIVITY
    is AppUsageCollector -> PrefKeys.STATUS_APP_USAGE
    is BatteryCollector -> PrefKeys.STATUS_BATTERY
    is BluetoothCollector -> PrefKeys.STATUS_BLUETOOTH
    is CallLogCollector -> PrefKeys.STATUS_CALL_LOG
    is DataTrafficCollector -> PrefKeys.STATUS_DATA_TRAFFIC
    is DeviceEventCollector -> PrefKeys.STATUS_DEVICE_EVENT
    is InstalledAppCollector -> PrefKeys.STATUS_INSTALLED_APP
    is KeyLogCollector -> PrefKeys.STATUS_KEY_LOG
    is LocationCollector -> PrefKeys.STATUS_LOCATION
    is MediaCollector -> PrefKeys.STATUS_MEDIA
    is MessageCollector -> PrefKeys.STATUS_MESSAGE
    is NotificationCollector -> PrefKeys.STATUS_NOTIFICATION
    is PhysicalStatCollector -> PrefKeys.STATUS_PHYSICAL_STAT
    is PolarH10Collector -> PrefKeys.STATUS_POLAR_H10
    is SurveyCollector -> PrefKeys.STATUS_SURVEY
    is WifiCollector -> PrefKeys.STATUS_WIFI
    is SensorCollector -> PrefKeys.STATUS_SENSOR
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
        if (state) {
            onStart()
        } else {
            onStop()
        }
    } catch (e: Exception) {
        if (state) throwable = e
    }

    if (throwable != null) {
        onComplete?.invoke(this, ABCException.wrap(throwable))
        return
    }

    when (this) {
        is ActivityCollector -> setStatus(ActivityCollector.Status(hasStarted = state))
        is AppUsageCollector -> setStatus(AppUsageCollector.Status(hasStarted = state))
        is BatteryCollector -> setStatus(BatteryCollector.Status(hasStarted = state))
        is BluetoothCollector -> setStatus(BluetoothCollector.Status(hasStarted = state))
        is CallLogCollector -> setStatus(CallLogCollector.Status(hasStarted = state))
        is DataTrafficCollector -> setStatus(DataTrafficCollector.Status(hasStarted = state))
        is DeviceEventCollector -> setStatus(DeviceEventCollector.Status(hasStarted = state))
        is InstalledAppCollector -> setStatus(InstalledAppCollector.Status(hasStarted = state))
        is KeyLogCollector -> setStatus(KeyLogCollector.Status(hasStarted = state))
        is LocationCollector -> setStatus(LocationCollector.Status(hasStarted = state))
        is MediaCollector -> setStatus(MediaCollector.Status(hasStarted = state))
        is MessageCollector -> setStatus(MessageCollector.Status(hasStarted = state))
        is NotificationCollector -> setStatus(NotificationCollector.Status(hasStarted = state))
        is PhysicalStatCollector -> setStatus(PhysicalStatCollector.Status(hasStarted = state))
        is PolarH10Collector -> setStatus(PolarH10Collector.Status(hasStarted = state))
        is SurveyCollector -> setStatus(SurveyCollector.Status(hasStarted = state))
        is WifiCollector -> setStatus(WifiCollector.Status(hasStarted = state))
        is SensorCollector -> setStatus(SensorCollector.Status(hasStarted = state))
    }
    AppLog.d(this::class.java.name, if (state) "start" else "stop")
    onComplete?.invoke(this, null)
}
