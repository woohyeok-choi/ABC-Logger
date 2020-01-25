package kaist.iclab.abclogger.collector.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.collector.getApplicationName
import kaist.iclab.abclogger.collector.isSystemApp
import kaist.iclab.abclogger.collector.isUpdatedSystemApp

class NotificationCollector(val context: Context) : BaseCollector {
    class NotificationCollectorService: NotificationListenerService() {
        override fun onNotificationPosted(sbn: StatusBarNotification?) {
            super.onNotificationPosted(sbn)
            if (!CollectorPrefs.hasStartedNotification) return

            try {
                sbn?.run {
                    store(this, true)
                    ABCEvent.post(postTime, ABCEvent.NOTIFICATION_POSTED)
                }

            } catch (e: Exception) { }
        }

        override fun onNotificationRemoved(sbn: StatusBarNotification?) {
            super.onNotificationRemoved(sbn)
            if (!CollectorPrefs.hasStartedNotification) return

            try {
                sbn?.run {
                    store(this, false)
                    ABCEvent.post(postTime, ABCEvent.NOTIFICATION_REMOVED)
                }
            } catch (e: Exception) {

            }
        }

        private fun visibilityToString (typeInt: Int) = when(typeInt) {
            Notification.VISIBILITY_PRIVATE -> "PRIVATE"
            Notification.VISIBILITY_PUBLIC -> "PUBLIC"
            Notification.VISIBILITY_SECRET -> "SECRET"
            else -> "UNKNOWN"
        }

        private fun store(sbn: StatusBarNotification, isPosted: Boolean) {
            val notification = sbn.notification
            val postTime = sbn.postTime
            val packageName = sbn.packageName
            val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val visibility = visibilityToString(notification.visibility)
            val category = notification.category ?: ""

            val vibrate: String
            val sound: String
            val lightColor: String

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                vibrate = notification.vibrate?.joinToString(",") ?: ""
                sound = notification.sound?.toString() ?: ""
                lightColor = notification.ledARGB.toString()
            } else {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = notification.channelId?.let { channelId ->
                    notificationManager.getNotificationChannel(channelId)
                }

                vibrate = channel?.vibrationPattern?.joinToString(",") ?: ""
                sound = channel?.sound?.toString() ?: ""
                lightColor = channel?.lightColor.toString()
            }

            NotificationEntity(
                    name = getApplicationName(packageManager = packageManager, packageName = packageName)
                            ?: "",
                    packageName = packageName,
                    isSystemApp = isSystemApp(packageManager = packageManager, packageName = packageName),
                    isUpdatedSystemApp = isUpdatedSystemApp(packageManager = packageManager, packageName = packageName),
                    title = title,
                    visibility = visibility,
                    category = category,
                    vibrate = vibrate,
                    sound = sound,
                    lightColor = lightColor,
                    isRemoved = isPosted
            ).fill(timeMillis = postTime).run { ObjBox.put(this) }
        }
    }

    override suspend fun onStart() { }

    override suspend fun onStop() { }

    override fun checkAvailability(): Boolean =
            Settings.Secure.getString(
                    context.contentResolver, "enabled_notification_listeners"
            )?.contains(context.packageName) == true

    override val requiredPermissions: List<String>
        get() = listOf()

    override val newIntentForSetUp: Intent?
        get() = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}
