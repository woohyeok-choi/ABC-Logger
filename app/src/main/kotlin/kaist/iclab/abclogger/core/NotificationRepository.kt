package kaist.iclab.abclogger.core

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.AbcError
import kaist.iclab.abclogger.commons.Formatter
import kaist.iclab.abclogger.commons.getColorFromAttr
import kaist.iclab.abclogger.ui.config.ConfigCollectorFragment
import kaist.iclab.abclogger.ui.config.ConfigGeneralFragment
import kaist.iclab.abclogger.ui.survey.response.SurveyResponseFragment

object NotificationRepository {
    private data class Setting(
        @StringRes val nameRes: Int,
        val category: String,
        val priority: Int,
        val visibility: Int,
        val importance: Int,
        val hasSound: Boolean,
        val hasVibration: Boolean
    )

    private const val CHANNEL_ID_SURVEY_TRIGGERED =
        "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_SURVEY_TRIGGERED"
    private const val CHANNEL_ID_FOREGROUND = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_FOREGROUND"
    private const val CHANNEL_ID_SYNC = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_SYNC"
    private const val CHANNEL_ID_ERROR = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID_ERROR"

    private const val GROUP_ID_SURVEY = "${BuildConfig.APPLICATION_ID}.GROUP_ID_SURVEY"

    private const val ID_SURVEY_TRIGGERED = 0x01
    private const val ID_FOREGROUND = 0x02
    private const val ID_SYNC = 0x03
    private const val ID_ERROR = 0x04
    private const val ID_COLLECTOR_ERROR = 0x04

    private val DEFAULT_VIBRATION_PATTERN =
        longArrayOf(0, 300, 50, 750, 1000, 300, 50, 750, 1000, 300, 50, 750)

    private val DEFAULT_RINGTONE_URI =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    /**
     * All kinds of notifications from this app are managed by here.
     * Please study here:   https://developer.android.com/training/notify-user/time-sensitive
     * and this discussion: https://stackoverflow.com/a/45920861
     */
    private val NOTIFICATION_SETTINGS = mapOf(
        CHANNEL_ID_SURVEY_TRIGGERED to Setting(
            nameRes = R.string.ntf_channel_survey,
            category = NotificationCompat.CATEGORY_REMINDER,
            priority = NotificationCompat.PRIORITY_DEFAULT,             // do not replace this with the higher priority. it may ignore the silent mode.
            visibility = NotificationCompat.VISIBILITY_PUBLIC,
            importance = NotificationManagerCompat.IMPORTANCE_DEFAULT,  // do not replace this with the higher importance. it may ignore the silent mode.
            hasSound = true,
            hasVibration = true,
        ),
        CHANNEL_ID_FOREGROUND to Setting(
            nameRes = R.string.ntf_channel_foreground,
            category = NotificationCompat.CATEGORY_STATUS,
            priority = NotificationCompat.PRIORITY_LOW,
            visibility = NotificationCompat.VISIBILITY_SECRET,
            importance = NotificationManagerCompat.IMPORTANCE_MIN,
            hasSound = false,
            hasVibration = false,
        ),
        CHANNEL_ID_SYNC to Setting(
            nameRes = R.string.ntf_channel_sync,
            category = NotificationCompat.CATEGORY_PROGRESS,
            priority = NotificationCompat.PRIORITY_LOW,
            visibility = NotificationCompat.VISIBILITY_PRIVATE,
            importance = NotificationManagerCompat.IMPORTANCE_MIN,
            hasSound = false,
            hasVibration = false,
        ),
        CHANNEL_ID_ERROR to Setting(
            nameRes = R.string.ntf_channel_error,
            category = NotificationCompat.CATEGORY_ERROR,
            priority = NotificationCompat.PRIORITY_HIGH,
            visibility = NotificationCompat.VISIBILITY_PUBLIC,
            importance = NotificationManagerCompat.IMPORTANCE_HIGH,
            hasSound = true,
            hasVibration = true,
        )
    )

    fun getSettingIntent(context: Context): Intent {
        val intent = Intent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.data = Uri.parse("package:" + context.packageName)
        }
        return intent
    }

    fun bind(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = NOTIFICATION_SETTINGS.map { (id, setting) ->
                NotificationChannel(id, context.getString(setting.nameRes), setting.importance).apply {
                    lockscreenVisibility = setting.visibility
                    if (setting.hasVibration) vibrationPattern = DEFAULT_VIBRATION_PATTERN
                    if (setting.hasSound) {
                        setSound(
                            DEFAULT_RINGTONE_URI,
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .build()
                        )
                    }
                }
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannels(channels)
        }
    }

    fun notifySurvey(
        context: Context,
        timestamp: Long,
        entityId: Long,
        title: String,
        message: String
    ) {
        val builder = getBuilder(context, CHANNEL_ID_SURVEY_TRIGGERED)
        val primaryColor = getColorFromAttr(context, R.attr.colorPrimary)

        val ntf = builder.apply {
            setOngoing(false)
            setAutoCancel(true)
            if (primaryColor != null) color = primaryColor
            setContentIntent(
                SurveyResponseFragment.pendingIntent(
                    context = context,
                    entityId = entityId,
                    title = title,
                    message = message,
                    triggerTime = timestamp
                )
            )
            setSmallIcon(R.drawable.baseline_article_24)
            setWhen(timestamp)
            setContentTitle(title)
            setStyle(NotificationCompat.BigTextStyle().bigText(message))
            setGroup(GROUP_ID_SURVEY)
            setTimeoutAfter(15*60*1000L)        // hard coded. need to fix this with expired time.
        }.build()

        NotificationManagerCompat.from(context).notify(entityIdToTag(entityId), ID_SURVEY_TRIGGERED, ntf)
    }

    fun cancelSurvey(context: Context, entityId: Long) {
        NotificationManagerCompat.from(context).cancel(entityIdToTag(entityId), ID_SURVEY_TRIGGERED)
    }

    fun notifyError(
        context: Context,
        timestamp: Long,
        message: String
    ) {
        val builder = getBuilder(context, CHANNEL_ID_ERROR)
        val errorColor = getColorFromAttr(context, R.attr.colorError)

        val ntf = builder.apply {
            setOngoing(false)
            setAutoCancel(true)
            if (errorColor != null) color = errorColor
            setContentIntent(ConfigGeneralFragment.pendingIntent(context))
            setSmallIcon(R.drawable.baseline_error_outline_24)
            setWhen(timestamp)
            setContentTitle(context.getString(R.string.ntf_error_title))
            setStyle(NotificationCompat.BigTextStyle().bigText(message))
        }.build()

        NotificationManagerCompat.from(context).notify(ID_ERROR, ntf)
    }

    fun notifyCollectorError(
        context: Context,
        timestamp: Long,
        message: String,
        name: String,
        qualifiedName: String,
        description: String
    ) {
        val builder = getBuilder(context, CHANNEL_ID_ERROR)
        val errorColor = getColorFromAttr(context, R.attr.colorError)

        val ntf = builder.apply {
            setOngoing(false)
            setAutoCancel(true)
            if (errorColor != null) color = errorColor
            setContentIntent(ConfigCollectorFragment.pendingIntent(context, name, qualifiedName, description))
            setSmallIcon(R.drawable.baseline_error_outline_24)
            setWhen(timestamp)
            setContentTitle(context.getString(R.string.ntf_error_title))
            setStyle(NotificationCompat.BigTextStyle().bigText("($name) $message"))
        }.build()

        NotificationManagerCompat.from(context).notify(ID_COLLECTOR_ERROR, ntf)
    }

    private fun foreground(context: Context, recordsUploaded: Long) : Notification {
        val builder = getBuilder(context, CHANNEL_ID_FOREGROUND)

        val primaryColor = getColorFromAttr(context, R.attr.colorPrimary)
        val compactFormatUploaded = Formatter.formatCompactNumber(recordsUploaded.coerceAtLeast(0))

        return builder.apply {
            setOngoing(true)
            setAutoCancel(false)
            if (primaryColor != null) color = primaryColor
            setSmallIcon(R.drawable.ic_ntf_abc)
            setShowWhen(false)
            setContentTitle(context.getString(R.string.ntf_foreground_title))
            /*
            setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(
                R.string.ntf_foreground_text,
                compactFormatUploaded
            )))
             */
        }.build()
    }

    fun notifyForeground(service: Service, recordsUploaded: Long) {
        service.startForeground(ID_FOREGROUND, foreground(service, recordsUploaded))
    }

    fun notifyForeground(context: Context, recordsUploaded: Long) {
        NotificationManagerCompat.from(context).notify(ID_FOREGROUND, foreground(context, recordsUploaded))
    }

    fun syncInitialize(context: Context, cancelIntent: PendingIntent) : ForegroundInfo {
        val builder = getBuilder(context, CHANNEL_ID_SYNC)
        val primaryColor = getColorFromAttr(context, R.attr.colorPrimary)
        val ntf = builder.apply {
            setOngoing(true)
            setAutoCancel(false)
            if (primaryColor != null) color = primaryColor
            setSmallIcon(R.drawable.baseline_sync_24)
            setShowWhen(true)
            setContentTitle(context.getString(R.string.ntf_sync_text_initialize))
            setProgress(0, 0, true)
            addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.baseline_clear_24, context.getString(R.string.ntf_sync_action_cancel), cancelIntent
                ).build()
            )
        }.build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(ID_SYNC, ntf, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(ID_SYNC, ntf)
        }
    }

    fun syncProgress(context: Context, max: Long, progress: Long, cancelIntent: PendingIntent) : ForegroundInfo {
        val builder = getBuilder(context, CHANNEL_ID_SYNC)
        val primaryColor = getColorFromAttr(context, R.attr.colorPrimary)
        val maxFormat = Formatter.formatCompactNumber(max)
        val progressFormat = Formatter.formatCompactNumber(progress)
        val normalizedProgress = (progress.toFloat() / max * 100).toInt()

        val ntf = builder.apply {
            setOngoing(true)
            setAutoCancel(false)
            setSmallIcon(R.drawable.baseline_sync_24)
            setShowWhen(false)
            if (primaryColor != null) color = primaryColor
            setContentTitle(context.getString(R.string.ntf_sync_title))
            setContentText(
                context.getString(
                    R.string.ntf_sync_text_progress,
                    progressFormat,
                    maxFormat
                )
            )
            addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.baseline_clear_24,
                    context.getString(R.string.ntf_sync_action_cancel),
                    cancelIntent
                ).build()
            )
            setProgress(100, normalizedProgress, false)
        }.build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(ID_SYNC, ntf, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(ID_SYNC, ntf)
        }
    }

    fun syncSuccess(context: Context) : ForegroundInfo {
        val builder = getBuilder(context, CHANNEL_ID_SYNC)
        val primaryColor = getColorFromAttr(context, R.attr.colorPrimary)

        val ntf = builder.apply {
            setOngoing(false)
            setAutoCancel(true)
            setSmallIcon(R.drawable.baseline_sync_24)
            setShowWhen(true)
            if (primaryColor != null) color = primaryColor
            setContentTitle(context.getString(R.string.ntf_sync_title))
            setContentText(context.getString(R.string.ntf_sync_text_complete))
        }.build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(ID_SYNC, ntf, 0)
        } else {
            ForegroundInfo(ID_SYNC, ntf)
        }
    }

    fun syncFailure(context: Context, throwable: Throwable?) : ForegroundInfo {
        val builder = getBuilder(context, CHANNEL_ID_SYNC)
        val errorColor = getColorFromAttr(context, R.attr.colorError)
        val message = AbcError.wrap(throwable).toSimpleString(context)

        val ntf = builder.apply {
            setOngoing(false)
            setAutoCancel(true)
            setSmallIcon(R.drawable.baseline_sync_24)
            setShowWhen(true)
            if (errorColor != null) color = errorColor
            if (errorColor != null) color = errorColor

            setContentTitle(context.getString(R.string.ntf_sync_title))
            setStyle(NotificationCompat.BigTextStyle().bigText(
                context.getString(R.string.ntf_sync_text_error, message)
            ))
        }.build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(ID_SYNC, ntf, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(ID_SYNC, ntf)
        }
    }


    private fun getBuilder(context: Context, channelId: String): NotificationCompat.Builder {
        val setting = NOTIFICATION_SETTINGS[channelId] ?: return NotificationCompat.Builder(
            context,
            channelId
        ).apply {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            priority = NotificationCompat.PRIORITY_DEFAULT
        }

        return NotificationCompat.Builder(context, channelId).apply {
            setVisibility(setting.visibility)
            priority = setting.priority
            setCategory(setting.category)

            if (setting.hasSound) {
                setSound(DEFAULT_RINGTONE_URI, AudioManager.STREAM_NOTIFICATION)
            }
            if (setting.hasVibration) {
                setVibrate(DEFAULT_VIBRATION_PATTERN)
            }
        }
    }

    fun isSync(id: Int) = id == ID_SYNC

    fun isForeground(id: Int) = id == ID_FOREGROUND

    fun isSurvey(id: Int) = id == ID_SYNC

    fun isError(id: Int) = id == ID_ERROR

    private fun entityIdToTag(entityId: Long) = "${BuildConfig.APPLICATION_ID}.SURVEY_ENTITY_ID.$entityId"
}
