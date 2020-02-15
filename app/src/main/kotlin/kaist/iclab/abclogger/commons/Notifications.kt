package kaist.iclab.abclogger.commons

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R

object Notifications {
    const val CHANNEL_ID_SURVEY = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_SURVEY"
    const val CHANNEL_ID_FOREGROUND = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_IN_PROGRESS"
    const val CHANNEL_ID_PROGRESS = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_UPLOAD"
    const val CHANNEL_ID_REQUIRE_SETTING = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_REQUIRE_SETTING"

    const val ID_SURVEY_DELIVERED = 0x01
    const val ID_FOREGROUND = 0x02
    const val ID_SYNC_PROGRESS = 0x03
    const val ID_FLUSH_PROGRESS = 0x04
    const val ID_REQUIRE_SETTING = 0x05

    private val DEFAULT_VIBRATION_PATTERN = longArrayOf(1000, 10, 1000, 10)

    private val DEFAULT_RINGTONE_URI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private val NOTIFICATION_SETTINGS = mapOf(
            CHANNEL_ID_SURVEY to NotificationSetting(
                    name = "Survey",
                    priority = NotificationCompat.PRIORITY_MAX,
                    visibility = NotificationCompat.VISIBILITY_PUBLIC,
                    importance = NotificationManagerCompat.IMPORTANCE_MAX,
                    ongoing = false,
                    showBadge = true,
                    showWhen = true,
                    autoCancel = true,
                    hasSound = true,
                    hasVibration = true,
                    alertOnce = false
            ),
            CHANNEL_ID_FOREGROUND to NotificationSetting(
                    name = "Experiment in progress",
                    priority = NotificationCompat.PRIORITY_MIN,
                    visibility = NotificationCompat.VISIBILITY_SECRET,
                    importance = NotificationManagerCompat.IMPORTANCE_MIN,
                    ongoing = true,
                    showBadge = false,
                    showWhen = false,
                    autoCancel = false,
                    hasSound = false,
                    hasVibration = false,
                    alertOnce = true
            ),
            CHANNEL_ID_PROGRESS to NotificationSetting(
                    name = "Upload",
                    priority = NotificationCompat.PRIORITY_HIGH,
                    visibility = NotificationCompat.VISIBILITY_PRIVATE,
                    importance = NotificationManagerCompat.IMPORTANCE_HIGH,
                    ongoing = false,
                    showBadge = false,
                    autoCancel = false,
                    showWhen = false,
                    hasSound = false,
                    hasVibration = false,
                    alertOnce = true
            ),
            CHANNEL_ID_REQUIRE_SETTING to NotificationSetting(
                    name = "Require setting",
                    priority = NotificationCompat.PRIORITY_MAX,
                    visibility = NotificationCompat.VISIBILITY_PRIVATE,
                    importance = NotificationManagerCompat.IMPORTANCE_MAX,
                    ongoing = false,
                    showBadge = false,
                    autoCancel = true,
                    showWhen = false,
                    hasSound = true,
                    hasVibration = true,
                    alertOnce = true
            )
    )


    private data class NotificationSetting(
            val name: String,
            val priority: Int,
            val visibility: Int,
            val importance: Int,
            val autoCancel: Boolean,
            val ongoing: Boolean,
            val showBadge: Boolean,
            val showWhen: Boolean,
            val alertOnce: Boolean,
            val hasSound: Boolean,
            val hasVibration: Boolean
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildChannel(notificationManager: NotificationManagerCompat,
                             channelId: String,
                             name: String,
                             importance: Int,
                             visibility: Int,
                             showBadge: Boolean = true,
                             vibrationPattern: LongArray? = null,
                             soundUri: Uri? = null) {
        val channel = notificationManager.getNotificationChannel(channelId)
        if (channel != null) return

        val newChannel = NotificationChannel(channelId, name, importance).apply {
            lockscreenVisibility = visibility
            setShowBadge(showBadge)
            this.importance = importance

            if (vibrationPattern != null) {
                enableVibration(true)
                setVibrationPattern(vibrationPattern)
            } else {
                enableVibration(false)
            }
            if (soundUri != null) {
                setSound(soundUri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            } else {
                setSound(null, null)
            }
        }
        notificationManager.createNotificationChannel(newChannel)
    }

    private fun buildNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = (NotificationManagerCompat.from(context))
            NOTIFICATION_SETTINGS.forEach { (channelId, setting) ->
                buildChannel(
                        notificationManager = manager,
                        channelId = channelId,
                        name = setting.name,
                        showBadge = setting.showBadge,
                        importance = setting.importance,
                        visibility = setting.visibility,
                        vibrationPattern = if (setting.hasVibration) DEFAULT_VIBRATION_PATTERN else null,
                        soundUri = if (setting.hasSound) DEFAULT_RINGTONE_URI else null
                )
            }
        }
    }

    fun bind(context: Context) {
        buildNotificationChannels(context)
    }

    fun notify(context: Context, id: Int, ntf: Notification) {
        NotificationManagerCompat.from(context).notify(id, ntf)
    }

    fun cancel(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun build(context: Context,
              channelId: String,
              title: String? = null,
              text: String? = null,
              bigText: String? = null,
              subText: String? = null,
              intent: PendingIntent? = null,
              @IntRange(from = 0, to = 100) progress: Int? = null,
              indeterminate: Boolean = false,
              timeoutMs: Long? = null,
              removeViews: RemoteViews? = null,
              actions: Collection<NotificationCompat.Action>? = null): Notification {
        val setting = NOTIFICATION_SETTINGS[channelId]
                ?: throw IllegalArgumentException("Invalid channel ID.")
        val timestamp = System.currentTimeMillis()

        return NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_abc_notification)
                .setColor(ContextCompat.getColor(context, R.color.color_primary))
                .setPriority(setting.priority)
                .setVisibility(setting.visibility)
                .setOngoing(setting.ongoing)
                .setOnlyAlertOnce(setting.alertOnce)
                .setShowWhen(setting.showWhen)
                .setWhen(timestamp)
                .setAutoCancel(setting.autoCancel)
                .apply {
                    if (setting.hasSound) setSound(DEFAULT_RINGTONE_URI)
                    if (setting.hasVibration) setVibrate(DEFAULT_VIBRATION_PATTERN)
                    title?.let { setContentTitle(it) }
                    text?.let { setContentText(it) }
                    subText?.let { setSubText(it) }
                    bigText?.let { setStyle(NotificationCompat.BigTextStyle().bigText(it)) }
                    intent?.let { setContentIntent(it) }
                    timeoutMs?.let { setTimeoutAfter(it) }
                    progress?.let { setProgress(100, it, indeterminate) }
                    if (removeViews != null) {
                        setCustomContentView(removeViews)
                        setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    }
                    actions?.forEach { addAction(it) }
                }.build()
    }
}
