package kaist.iclab.abclogger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat

object NotificationUtils {
    private const val CHANNEL_ID_SURVEY = "kaist.iclab.abc.logger.CHANNEL_ID_SURVEY"
    private const val CHANNEL_ID_IN_PROGRESS = "kaist.iclab.abc.logger.CHANNEL_ID_IN_PROGRESS"
    private const val CHANNEL_ID_UPLOAD = "kaist.iclab.abc.logger.CHANNEL_ID_UPLOAD"

    fun bind (context: Context) {
        (NotificationManagerCompat.from(context)).let { manager ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                buildChannel(
                        notificationManager = manager,
                        channelId = CHANNEL_ID_SURVEY,
                        name = "Survey",
                        showBadge = true,
                        importance = NotificationManagerCompat.IMPORTANCE_HIGH,
                        visibility = Notification.VISIBILITY_PUBLIC,
                        vibrationPattern = longArrayOf(1000, 10, 1000, 10),
                        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                )
                buildChannel(
                        notificationManager = manager,
                        channelId = CHANNEL_ID_IN_PROGRESS,
                        name = "Experiment in progress",
                        showBadge = false,
                        importance = NotificationManagerCompat.IMPORTANCE_LOW,
                        visibility = Notification.VISIBILITY_SECRET
                )
                buildChannel(
                        notificationManager = manager,
                        channelId = CHANNEL_ID_UPLOAD,
                        name = "Upload progress",
                        showBadge = false,
                        importance = NotificationManagerCompat.IMPORTANCE_HIGH,
                        visibility = Notification.VISIBILITY_SECRET
                )
            }
        }
    }

    fun notifySurvey() {

    }

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
            if (vibrationPattern != null) {
                enableVibration(true)
                setVibrationPattern(vibrationPattern)
            } else {
                enableVibration(false)
            }
            if (soundUri != null) {
                setSound(soundUri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
        }
        notificationManager.createNotificationChannel(newChannel)
    }
}