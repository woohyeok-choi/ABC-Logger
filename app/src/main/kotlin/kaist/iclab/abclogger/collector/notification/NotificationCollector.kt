package kaist.iclab.abclogger.collector.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kaist.iclab.abclogger.AbcEvent
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.*
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.reflect.KClass

class NotificationCollector(private val context: Context) : BaseCollector<NotificationCollector.Status>(context) {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null) : BaseStatus() {
        override fun info(): Map<String, Any> = mapOf()
    }

    override val clazz: KClass<Status> = Status::class

    override val name: String = context.getString(R.string.data_name_notification)

    override val description: String = context.getString(R.string.data_desc_notification)

    override val requiredPermissions: List<String> = listOf()

    override val newIntentForSetUp: Intent? = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    override suspend fun checkAvailability(): Boolean =
            Settings.Secure.getString(
                    context.contentResolver, "enabled_notification_listeners"
            )?.contains(context.packageName) == true

    override suspend fun onStart() {}

    override suspend fun onStop() {}

    class NotificationCollectorService : NotificationListenerService() {
        private val collector: NotificationCollector by inject()

        override fun onNotificationPosted(sbn: StatusBarNotification?) {
            super.onNotificationPosted(sbn)
            collector.launch {
                if (collector.getStatus()?.hasStarted == true && sbn != null) {
                    store(sbn, true)
                    AbcEvent.post(sbn.postTime, AbcEvent.NOTIFICATION_POSTED)
                }
            }
        }

        override fun onNotificationRemoved(sbn: StatusBarNotification?) {
            super.onNotificationRemoved(sbn)

            collector.launch {
                if (collector.getStatus()?.hasStarted == true && sbn != null) {
                    store(sbn, false)
                    AbcEvent.post(sbn.postTime, AbcEvent.NOTIFICATION_REMOVED)
                }
            }
        }

        private fun visibilityToString(typeInt: Int) = when (typeInt) {
            Notification.VISIBILITY_PRIVATE -> "PRIVATE"
            Notification.VISIBILITY_PUBLIC -> "PUBLIC"
            Notification.VISIBILITY_SECRET -> "SECRET"
            else -> "UNKNOWN"
        }

        private suspend fun store(sbn: StatusBarNotification, isPosted: Boolean) {
            val notification = sbn.notification
            val postTime = sbn.postTime
            val packageName = sbn.packageName
            val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?: ""
            val visibility = visibilityToString(notification.visibility)
            val category = notification.category ?: ""

            val vibrate: String
            val sound: String
            val lightColor: String

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                vibrate = notification?.vibrate?.joinToString(",") ?: ""
                sound = notification?.sound?.toString() ?: ""
                lightColor = notification?.ledARGB?.toString() ?: ""
            } else {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = notification?.channelId?.let { channelId ->
                    notificationManager.getNotificationChannel(channelId)
                }

                vibrate = channel?.vibrationPattern?.joinToString(",") ?: ""
                sound = channel?.sound?.toString() ?: ""
                lightColor = channel?.lightColor.toString()
            }

            NotificationEntity(
                    name = getApplicationName(packageManager = packageManager, packageName = packageName)
                            ?: "",
                    packageName = packageName ?: "",
                    isSystemApp = isSystemApp(packageManager = packageManager, packageName = packageName),
                    isUpdatedSystemApp = isUpdatedSystemApp(packageManager = packageManager, packageName = packageName),
                    title = title,
                    visibility = visibility,
                    category = category,
                    vibrate = vibrate,
                    sound = sound,
                    lightColor = lightColor,
                    isPosted = isPosted
            ).fill(timeMillis = postTime).also { entity ->
                ObjBox.put(entity)
                collector.setStatus(Status(lastTime = postTime))
            }
        }
    }
}
