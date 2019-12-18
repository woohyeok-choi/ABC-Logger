package kaist.iclab.abclogger.common.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.ABCException
import kaist.iclab.abclogger.data.entities.SurveyEntity
import kaist.iclab.abclogger.foreground.activity.RootActivity
import kaist.iclab.abclogger.foreground.activity.SurveyQuestionActivity
import java.lang.Exception

object NotificationUtils {
    const val CHANNEL_ID_SURVEY_DELIVERED = "CHANNEL_ID_SURVEY_DELIVERED"
    const val CHANNEL_ID_SURVEY_REMAINED = "CHANNEL_ID_SURVEY_REMAINED"
    const val CHANNEL_ID_UPLOAD_ON_FOREGROUND = "CHANNEL_ID_UPLOAD_ON_FOREGROUND"
    const val CHANNEL_ID_EXPERIMENT_IN_PROGRESS = "CHANNEL_ID_EXPERIMENT_IN_PROGRESS"
    const val CHANNEL_ID_UPDATE_AVAILABLE = "CHANNEL_ID_UPDATE_AVAILABLE"

    const val NOTIFICATION_ID_SURVEY_DELIVERED = 0x00000001
    const val NOTIFICATION_ID_SURVEY_REMAINED = 0x00000002
    const val NOTIFICATION_ID_UPLOAD_ON_FOREGROUND = 0x00000003
    const val NOTIFICATION_ID_EXPERIMENT_IN_PROGRESS = 0x00000004
    const val NOTIFICATION_ID_UPDATE_AVAILABLE = 0x00000005

    private const val REQUEST_CODE_SURVEY_DELIVERED = 0x00000100
    private const val REQUEST_CODE_SURVEY_REMAINED = 0x00000110
    private const val REQUEST_CODE_UPDATE_AVAILABLE = 0x00000111

    private val VIBRATE_PATTERN: LongArray = longArrayOf(1000, 10, 1000, 10)
    private val SOUND_URI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

   @RequiresApi(Build.VERSION_CODES.O)
    private fun buildChannelForSurveyDelivered (context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).let { manager ->
            manager.getNotificationChannel(CHANNEL_ID_SURVEY_DELIVERED) ?:
            NotificationChannel(CHANNEL_ID_SURVEY_DELIVERED, "Survey is delivered", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                importance = NotificationManager.IMPORTANCE_HIGH
                setShowBadge(true)
            }.let { manager.createNotificationChannel(it) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildChannelForSurveyRemained (context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).let { manager ->
            manager.getNotificationChannel(CHANNEL_ID_SURVEY_REMAINED) ?:
            NotificationChannel(CHANNEL_ID_SURVEY_REMAINED, "Survey is remained not responded", NotificationManager.IMPORTANCE_DEFAULT).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                importance = NotificationManager.IMPORTANCE_DEFAULT
                setShowBadge(false)
            }.let { manager.createNotificationChannel(it) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildChannelForUploadOnForeground (context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).let { manager ->
            manager.getNotificationChannel(CHANNEL_ID_UPLOAD_ON_FOREGROUND) ?:
            NotificationChannel(CHANNEL_ID_UPLOAD_ON_FOREGROUND, "Upload...", NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = Notification.VISIBILITY_SECRET
                importance = NotificationManager.IMPORTANCE_LOW
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }.let { manager.createNotificationChannel(it) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildChannelForExperimentInProgress (context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).let { manager ->
            manager.getNotificationChannel(CHANNEL_ID_EXPERIMENT_IN_PROGRESS) ?:
            NotificationChannel(CHANNEL_ID_EXPERIMENT_IN_PROGRESS, "Experiment in progress...", NotificationManager.IMPORTANCE_MIN).apply {
                lockscreenVisibility = Notification.VISIBILITY_SECRET
                importance = NotificationManager.IMPORTANCE_MIN
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }.let { manager.createNotificationChannel(it) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildChannelForUpdateAvailable(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).let { manager ->
            manager.getNotificationChannel(CHANNEL_ID_UPDATE_AVAILABLE) ?:
            NotificationChannel(CHANNEL_ID_UPDATE_AVAILABLE, "New update available", NotificationManager.IMPORTANCE_MIN).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                importance = NotificationManager.IMPORTANCE_MIN
                setShowBadge(false)
              }.let { manager.createNotificationChannel(it) }
        }
    }

    fun buildNotificationForSurveyDelivered(context: Context, entity: SurveyEntity, number: Int) : Notification {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) buildChannelForSurveyDelivered(context)

        return NotificationCompat.Builder(context, CHANNEL_ID_SURVEY_DELIVERED)
            .setContentTitle(entity.title)
            .setSmallIcon(R.mipmap.ic_abc_notification)
            .setContentText(entity.message)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setLights(ContextCompat.getColor(context, R.color.colorPrimary), 1000, 500)
            .setSound(SOUND_URI)
            .setVibrate(VIBRATE_PATTERN)
            .setNumber(number)
            .setSubText(FormatUtils.formatSameYear(context, entity.deliveredTime, entity.deliveredTime))
            .setShowWhen(false)
            .setContentIntent(
                TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(SurveyQuestionActivity.newIntent(context, entity, false))
                    .getPendingIntent(REQUEST_CODE_SURVEY_DELIVERED, PendingIntent.FLAG_UPDATE_CURRENT)
            ).build()
    }

    fun buildNotificationForSurveyRemained(context: Context, numNotResponded: Int) : Notification {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) buildChannelForSurveyRemained(context)
        val now = System.currentTimeMillis()

        return NotificationCompat.Builder(context, CHANNEL_ID_SURVEY_REMAINED)
            .setContentTitle(context.getString(R.string.noti_title_survey_check))
            .setSmallIcon(R.mipmap.ic_abc_notification)
            .setContentText(String.format("%s %s", numNotResponded, context.getString(R.string.noti_text_survey_check)))
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setLights(ContextCompat.getColor(context, R.color.colorPrimary), 1000, 500)
            .setSound(SOUND_URI)
            .setVibrate(VIBRATE_PATTERN)
            .setSubText(FormatUtils.formatSameYear(context, now, now))
            .setShowWhen(false)
            .setContentIntent(
                TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(
                        Intent(context, RootActivity::class.java)
                    ).getPendingIntent(REQUEST_CODE_SURVEY_REMAINED, PendingIntent.FLAG_UPDATE_CURRENT)
            ).build()
    }

    fun buildNotificationForUploadOnForeground(context: Context, curProgress: Int, maxProgress: Int, exception: Exception? = null) : Notification {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) buildChannelForUploadOnForeground(context)

        var builder = NotificationCompat.Builder(context, CHANNEL_ID_UPLOAD_ON_FOREGROUND)
            .setContentTitle(context.getString(R.string.noti_title_sync))
            .setSmallIcon(R.mipmap.ic_abc_notification)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(false)

        builder = when {
            exception != null ->  builder.setProgress(0, 0, false)
                .setContentText(String.format("%s: %s", context.getText(R.string.noti_text_sync_failed), context.getText(if(exception is ABCException) exception.getErrorStringRes() else R.string.error_general_error)))
                .setTimeoutAfter(5000)
            maxProgress > curProgress -> builder.setProgress(maxProgress, curProgress, false)
                .setContentText(context.getString(R.string.noti_text_sync))
            else -> builder.setProgress(0, 0, false)
                .setContentText(context.getText(R.string.noti_text_sync_complete))
                .setTimeoutAfter(5000)
        }

        return builder.build()
    }

    fun buildNotificationForExperimentInProgress(context: Context) : Notification {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) buildChannelForExperimentInProgress(context)

        return NotificationCompat.Builder(context, CHANNEL_ID_EXPERIMENT_IN_PROGRESS)
            .setContentTitle(context.getString(R.string.noti_title_service_running))
            .setSmallIcon(R.mipmap.ic_abc_notification)
            .setContentText(context.getString(R.string.noti_text_service_running))
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            .build()
    }

    fun buildNotificationForUpdateAvailable(context: Context, title: String, text: String) : Notification {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) buildChannelForUpdateAvailable(context)

        return NotificationCompat.Builder(context, CHANNEL_ID_UPDATE_AVAILABLE)
            .setSmallIcon(R.mipmap.ic_abc_notification)
            .setContentText(title)
            .setContentTitle(text)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setContentIntent(
                PendingIntent.getActivity(context, REQUEST_CODE_UPDATE_AVAILABLE,
                    Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse("https://play.google.com/store/apps/details?id=kaist.iclab.abc"))
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_ONE_SHOT
                )
            )
            .build()
    }

    fun notifySurveyDelivered(context: Context, entity: SurveyEntity, number: Int) {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SURVEY_DELIVERED, buildNotificationForSurveyDelivered(context, entity, number))
    }

    fun cancelSurveyDelivered(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SURVEY_DELIVERED)
    }

    fun notifySurveyRemained(context: Context, numNotResponded: Int) {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SURVEY_REMAINED, buildNotificationForSurveyRemained(context, numNotResponded))
    }

    fun cancelSurveyRemained(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SURVEY_REMAINED)
    }

    fun notifyUploadProgress(context: Context, curProgress: Int, maxProgress: Int, exception: Exception? = null) {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UPLOAD_ON_FOREGROUND, buildNotificationForUploadOnForeground(context, curProgress, maxProgress, exception))
    }

    fun notifyUpdateAvailable(context: Context, title: String, text: String) {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UPDATE_AVAILABLE, buildNotificationForUpdateAvailable(context, title, text))
    }
}