package kaist.iclab.abclogger.collector.notification

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.collector.stringifyNotificationCategory
import kaist.iclab.abclogger.collector.stringifyNotificationPriority
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.collector.stringifyNotificationVisibility
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.NotificationRepository
import kaist.iclab.abclogger.core.collector.Description
import org.koin.android.ext.android.inject

class NotificationCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<NotificationEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    override val permissions: List<String> = listOf()

    override val setupIntent: Intent? = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    override fun isAvailable(): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    override fun getDescription(): Array<Description> = arrayOf()

    override suspend fun onStart() {    }

    override suspend fun onStop() {    }

    override suspend fun count(): Long = dataRepository.count<NotificationEntity>()

    override suspend fun flush(entities: Collection<NotificationEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<NotificationEntity> =
        dataRepository.find(0, limit)

    class NotificationCollectorService : NotificationListenerService() {
        private val collector: NotificationCollector by inject()

        override fun onNotificationPosted(sbn: StatusBarNotification?) {
            super.onNotificationPosted(sbn)
            sbn ?: return
            if (!collector.isEnabled) return

            handleNotificationReceived(sbn, System.currentTimeMillis(), true)
        }

        override fun onNotificationRemoved(sbn: StatusBarNotification?) {
            super.onNotificationRemoved(sbn)
            sbn ?: return
            if (!collector.isEnabled) return

            handleNotificationReceived(sbn, System.currentTimeMillis(), false)
        }

        private fun handleNotificationReceived(
            sbn: StatusBarNotification,
            timestamp: Long,
            isPosted: Boolean
        ) = collector.launch {
            val notification = sbn.notification ?: return@launch
            if (NotificationRepository.isSync(sbn.id) && sbn.packageName == packageName) return@launch

            val key = sbn.key ?: ""
            val groupKey = sbn.groupKey ?: ""
            val notificationId = sbn.id
            val tag = sbn.tag ?: ""
            val isClearable = sbn.isClearable
            val isOngoing = sbn.isOngoing

            val postTime = sbn.postTime
            val packageName = sbn.packageName ?: ""
            val extras = notification.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val bigTitle = extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString() ?: ""
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val summaryText =
                extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString() ?: ""
            val infoText = extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""
            val vibrate: String
            val sound: String
            val lightColor: String
            val priority: String

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = NotificationManagerCompat.from(this)
                val channelId = notification.channelId ?: ""
                val channel = manager.getNotificationChannel(channelId)

                vibrate = channel?.vibrationPattern?.joinToString(",") ?: ""
                sound = channel?.sound?.toString() ?: ""
                lightColor = channel?.lightColor.toString()
                priority = stringifyNotificationPriority(channel?.importance)
            } else {
                vibrate = notification.vibrate?.joinToString(",") ?: ""
                sound = notification.sound?.toString() ?: ""
                lightColor = notification.ledARGB.toString()
                priority = stringifyNotificationPriority(notification.priority)
            }

            val entity = NotificationEntity(
                key = key,
                groupKey = groupKey,
                notificationId = notificationId,
                tag = tag,
                isClearable = isClearable,
                isOngoing = isOngoing,
                name = getApplicationName(
                    packageManager = packageManager,
                    packageName = packageName
                )
                    ?: "",
                packageName = packageName,
                postTime = postTime,
                isSystemApp = isSystemApp(
                    packageManager = packageManager,
                    packageName = packageName
                ),
                isUpdatedSystemApp = isUpdatedSystemApp(
                    packageManager = packageManager,
                    packageName = packageName
                ),
                    /*
                title = title,
                bigTitle = bigTitle,
                text = text,
                subText = subText,
                bigText = bigText,
                summaryText = summaryText,
                infoText = infoText,
                     */ // delete due to privacy concerns.
                visibility = stringifyNotificationVisibility(notification.visibility),
                category = stringifyNotificationCategory(notification.category),
                priority = priority,
                vibrate = vibrate,
                sound = sound,
                lightColor = lightColor,
                isPosted = isPosted
            )

            collector.put(
                entity.apply { this.timestamp = timestamp }
            )
        }
    }
}
