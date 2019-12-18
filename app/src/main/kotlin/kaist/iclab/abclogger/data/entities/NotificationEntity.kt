package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.data.types.NotificationVisibilityType
import kaist.iclab.abclogger.data.types.NotificationVisibilityTypeConverter

/**
 * It represents a notification info. that a user receives.
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property packageName package name identifier that a user currently interacts with
 * @property isSystemApp indicates that the app is installed by a system (i.e., default app)
 * @property isUpdatedSystemApp indicates that the app is installed by a system and has been updated
 * @property title a title of the notification
 * @property visibility a visibility in the lock screen
 * @property category a category information (see https://developer.android.com/reference/android/app/Notification#category)
 * @property hasVibration indicates that a phone is vibrated or not when the notification is received
 * @property lightColor LED light colours
 * @property hasSound indicates that a phone plays some sound when the notification is received
 */

@Entity
data class NotificationEntity (
    var name: String = "",
    var packageName: String = "",
    var isSystemApp: Boolean = false,
    var isUpdatedSystemApp: Boolean = false,
    var title: String = "",
    @Convert(converter = NotificationVisibilityTypeConverter::class, dbType = String::class)
    var visibility: NotificationVisibilityType = NotificationVisibilityType.UNDEFINED,
    var category: String = "",
    var hasVibration: Boolean = false,
    var hasSound: Boolean = false,
    var lightColor: String = "",
    var key: String = "",
    var isPosted: Boolean = false
) : BaseEntity()