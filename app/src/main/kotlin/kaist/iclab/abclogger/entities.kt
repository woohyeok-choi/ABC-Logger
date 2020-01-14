package kaist.iclab.abclogger

import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Transient
import java.util.*
import java.util.concurrent.TimeUnit

@BaseEntity
abstract class Base(
        @Id var id: Long = 0,
        var timestamp: Long = -1,
        var utcOffset: Float = Float.MIN_VALUE,
        var subjectEmail: String = "",
        var participationTime: Long = -1,
        var deviceInfo: String = "",
        @Index var isUploaded: Boolean = false
)

fun <T : Base> T.fillBaseInfo(timeMillis: Long): T {
    timestamp = timeMillis
    utcOffset = TimeZone.getDefault().rawOffset.toFloat() / (1000 * 60 * 60)
    subjectEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    participationTime = SharedPrefs.participationTime
    deviceInfo = "${Build.MANUFACTURER}-${Build.MODEL}-${Build.VERSION.RELEASE}"
    isUploaded = false

    return this
}

@Entity
data class AppUsageEventEntity(
        var name: String = "",
        var packageName: String = "",
        var type: String = "",
        var isSystemApp: Boolean = false,
        var isUpdatedSystemApp: Boolean = false
) : Base()

@Entity
data class BatteryEntity(
        var level: Int = Int.MIN_VALUE,
        var scale: Int = Int.MIN_VALUE,
        var temperature: Int = Int.MIN_VALUE,
        var voltage: Int = Int.MIN_VALUE,
        var health: String = "",
        var pluggedType: String = "",
        var status: String = ""
) : Base()

@Entity
data class BluetoothEntity(
        var deviceName: String = "",
        var address: String = "",
        var rssi: Int = Int.MIN_VALUE
) : Base()

@Entity
data class CallLogEntity(
        var duration: Long = Long.MIN_VALUE,
        var number: String = "",
        var type: String = "",
        var presentation: String = "",
        var dataUsage: Long = Long.MIN_VALUE,
        var contactType: String = "",
        var isStarred: Boolean = false,
        var isPinned: Boolean = false
) : Base()

@Entity
data class DataTrafficEntity(
        var fromTime: Long = Long.MIN_VALUE,
        var rxBytes: Long = Long.MIN_VALUE,
        var txBytes: Long = Long.MIN_VALUE,
        var mobileRxBytes: Long = Long.MIN_VALUE,
        var mobileTxBytes: Long = Long.MIN_VALUE
) : Base()


@Entity
data class DeviceEventEntity(
        var type: String = ""
) : Base()

@Entity
data class InstalledAppEntity(
        var name: String = "",
        var packageName: String = "",
        var isSystemApp: Boolean = false,
        var isUpdatedSystemApp: Boolean = false,
        var firstInstallTime: Long = Long.MIN_VALUE,
        var lastUpdateTime: Long = Long.MIN_VALUE
) : Base()

@Entity
data class KeyTrackEntity(
        var name: String = "",
        var packageName: String = "",
        var isSystemApp: Boolean = false,
        var isUpdatedSystemApp: Boolean = false,
        var distance : Float = 0.0F,
        var timeTaken : Long = 0
) : Base()

@Entity
data class LocationEntity(
        var latitude: Double = Double.MIN_VALUE,
        var longitude: Double = Double.MIN_VALUE,
        var altitude: Double = Double.MIN_VALUE,
        var accuracy: Float = Float.MIN_VALUE,
        var speed: Float = Float.MIN_VALUE
) : Base()

@Entity
data class MediaEntity(
        var mimeType: String = ""
) : Base()

@Entity
data class NotificationEntity(
        var name: String = "",
        var packageName: String = "",
        var isSystemApp: Boolean = false,
        var isUpdatedSystemApp: Boolean = false,
        var title: String = "",
        var visibility: String = "",
        var category: String = "",
        var vibrate: String = "",
        var sound: String = "",
        var lightColor: String = "",
        var isRemoved: Boolean = false
) : Base()

@Entity
data class PhysicalActivityTransitionEntity(
        var type: String = "",
        var isEntered: Boolean = false
) : Base()

@Entity
data class PhysicalActivityEntity(
        var type: String = "",
        var confidence: Int = -1
) : Base()

@Entity
data class PhysicalStatusEntity(
        var type: String = "",
        var startTime: Long = 0,
        var endTime: Long = 0,
        var value: Float = Float.MIN_VALUE
) : Base()

@Entity
data class SurveyEntity(
        var title: String = "",
        var message: String = "",
        var timeoutPolicy: String = "",
        var timeoutSec: Long = 0,
        var deliveredTime: Long = 0,
        var reactionTime: Long = 0,
        var responseTime: Long = 0,
        var json: String = "",
        @Transient var isResponded: Boolean = responseTime > 0
) : Base() {
    fun isAnswered() : Boolean = responseTime > 0

    fun isExpired() : Boolean = timeoutPolicy == Survey.TIMEOUT_DISABLED &&
            System.currentTimeMillis() > deliveredTime + TimeUnit.SECONDS.toMillis(timeoutSec)

    fun isAvailable() : Boolean = !isAnswered() && !isExpired()

    fun showAltText() : Boolean = timeoutPolicy == Survey.TIMEOUT_ALT_TEXT &&
            System.currentTimeMillis() > deliveredTime + TimeUnit.SECONDS.toMillis(timeoutSec)
}

@Entity
data class WifiEntity(
        var bssid: String = "",
        var ssid: String = "",
        var frequency: Int = Int.MIN_VALUE,
        var rssi: Int = Int.MIN_VALUE
) : Base()


@Entity
data class MessageEntity (
        var number: String = "",
        var messageClass: String = "",
        var messageBox: String = "",
        var contactType: String = "",
        var isStarred: Boolean = false,
        var isPinned: Boolean = false
) : Base()

@Entity
data class SensorEntity(
        var sensorId: String = "",
        var sensorName: String = "",
        var valueDescription: String = "",
        var valueType: String = "",
        var firstValue: Float = Float.MIN_VALUE,
        var secondValue: Float = Float.MIN_VALUE,
        var thirdValue: Float = Float.MIN_VALUE,
        var fourthValue: Float = Float.MIN_VALUE
) : Base()

@Entity
data class SurveySettingEntity(
        @Id var id: Long = 0,
        @Index var uuid: String = "",
        var url: String = "",
        var json: String = "",
        var lastTimeTriggered: Long = -1,
        var nextTimeTriggered: Long = -1
)