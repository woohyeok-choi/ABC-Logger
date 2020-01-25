package kaist.iclab.abclogger.collector.survey

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateUtils

import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.ui.question.SurveyResponseActivity
import kotlinx.coroutines.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SurveyCollector(val context: Context) : BaseCollector {
    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != ACTION_SURVEY_TRIGGER) return
                val box = ObjBox.boxFor<SurveySettingEntity>() ?: return
                GlobalScope.launch(Dispatchers.IO) {
                    intent.getStringExtra(EXTRA_SURVEY_UUID)?.let { uuid ->
                        box.query().equal(SurveySettingEntity_.uuid, uuid).build().findFirst()
                    }?.let { setting ->
                        handleTrigger(setting)
                    }
                    cancelSurvey(context)
                    scheduleSurvey(context)
                }
            }
        }
    }

    private val filter = IntentFilter().apply { addAction(ACTION_SURVEY_TRIGGER) }

    private fun handleTrigger(setting: SurveySettingEntity) {
        val survey = Survey.fromJson(setting.json) ?: return
        val curTime = System.currentTimeMillis()
        val id = SurveyEntity(
                title = survey.title,
                message = survey.message,
                timeoutPolicy = survey.timeoutPolicy,
                timeoutSec = survey.timeoutSec,
                deliveredTime = curTime,
                json = setting.json
        ).fill(timeMillis = curTime).run { ObjBox.putSync(this) }
        if (id < 0) return

        val surveyIntent = context.intentFor<SurveyResponseActivity>(
                SurveyResponseActivity.EXTRA_ENTITY_ID to id,
                SurveyResponseActivity.EXTRA_SHOW_FROM_LIST to false,
                SurveyResponseActivity.EXTRA_SURVEY_TITLE to survey.title,
                SurveyResponseActivity.EXTRA_SURVEY_MESSAGE to survey.message,
                SurveyResponseActivity.EXTRA_SURVEY_DELIVERED_TIME to curTime
        )
        val pendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(surveyIntent)
                .getPendingIntent(REQUEST_CODE_SURVEY_OPEN, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = Notifications.build(
                context = context,
                channelId = Notifications.CHANNEL_ID_SURVEY,
                title = survey.title,
                text = survey.message,
                subText = DateUtils.formatDateTime(
                        context, curTime,
                        DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
                ),
                intent = pendingIntent
        )

        NotificationManagerCompat.from(context).notify(Notifications.ID_SURVEY_DELIVERED, notification)
    }

    private fun scheduleSurvey(context: Context, event: ABCEvent? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val box = ObjBox.boxFor<SurveySettingEntity>() ?: return
        val settings = box.all.mapNotNull { setting ->
            Survey.fromJson(jsonString = setting.json)?.let { survey -> Pair(survey, setting) }
        }.mapNotNull { (survey, setting) ->
            when (survey) {
                is IntervalBasedSurvey -> updateIntervalBasedSurvey(survey, setting)
                is ScheduleBasedSurvey -> updateScheduleBasedSurvey(survey, setting)
                is EventBasedSurvey -> updateEventBasedSurvey(survey, setting, event)
                else -> null
            }
        }
        box.put(settings)

        settings.forEach { setting ->
            val pendingIntent = getPendingIntent(setting.id, setting.uuid)
            if (setting.nextTimeTriggered < 0) {
                alarmManager.cancel(pendingIntent)
            } else {
                val curTime = System.currentTimeMillis()
                val triggerTime = if (setting.nextTimeTriggered < curTime + TimeUnit.SECONDS.toMillis(5)) {
                    curTime + TimeUnit.SECONDS.toMillis(10)
                } else {
                    setting.nextTimeTriggered
                }
                AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    private fun cancelSurvey(context: Context) {
        val box = ObjBox.boxFor<SurveySettingEntity>() ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        box.all?.forEach { entity ->
            alarmManager.cancel(getPendingIntent(id = entity.id, uuid = entity.uuid))
        }
    }

    private fun getPendingIntent(id: Long, uuid: String) = PendingIntent.getBroadcast(
            context, id.toInt(),
            Intent(ACTION_SURVEY_TRIGGER).putExtra(EXTRA_SURVEY_UUID, uuid),
            PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun updateIntervalBasedSurvey(survey: IntervalBasedSurvey, setting: SurveySettingEntity): SurveySettingEntity {
        val curTime = System.currentTimeMillis()

        val initDelayMs = if (survey.initialDelaySec > 0) {
            TimeUnit.SECONDS.toMillis(survey.initialDelaySec)
        } else {
            0
        }
        val intervalMs = if (survey.intervalSec > 0) {
            TimeUnit.SECONDS.toMillis(survey.intervalSec)
        } else {
            0
        }
        val flexMs = if (survey.flexIntervalSec > 0) {
            TimeUnit.SECONDS.toMillis(Random.nextLong(survey.flexIntervalSec))
        } else {
            0
        }

        return when {
            curTime < GeneralPrefs.participationTime + initDelayMs -> setting.copy(
                    nextTimeTriggered = GeneralPrefs.participationTime + initDelayMs
            )
            curTime <= setting.nextTimeTriggered -> setting
            else -> setting.copy(
                    nextTimeTriggered = setting.lastTimeTriggered + intervalMs + flexMs
            )
        }
    }

    private fun updateEventBasedSurvey(survey: EventBasedSurvey,
                                       setting: SurveySettingEntity,
                                       event: ABCEvent? = null): SurveySettingEntity {
        val intervalMs = if (survey.delayAfterTriggerEventSec > 0) {
            TimeUnit.SECONDS.toMillis(survey.delayAfterTriggerEventSec)
        } else {
            0
        }
        val flexMs = if (survey.flexDelayAfterTriggerEventSec > 0) {
            TimeUnit.SECONDS.toMillis(Random.nextLong(survey.flexDelayAfterTriggerEventSec))
        } else {
            0
        }

        return when (event?.eventType) {
            in survey.triggerEvents -> setting.copy(
                    nextTimeTriggered = event?.timestamp?.plus(intervalMs + flexMs) ?: -1
            )
            in survey.cancelEvents -> setting.copy(
                    nextTimeTriggered = -1
            )
            else -> setting
        }
    }

    private fun updateScheduleBasedSurvey(survey: ScheduleBasedSurvey, setting: SurveySettingEntity): SurveySettingEntity {
        val curTime = System.currentTimeMillis()
        val calendar = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = curTime
        }
        val curDate = calendar.time

        val nextTimeTriggered = survey.schedules.mapNotNull { schedule ->
            (0..Int.MAX_VALUE step 7).firstNotNullResult { day ->
                val triggerCalendar = calendar.apply {
                    set(GregorianCalendar.DAY_OF_WEEK, schedule.dayOfWeek.id)
                    set(GregorianCalendar.HOUR_OF_DAY, schedule.hour)
                    set(GregorianCalendar.MINUTE, schedule.minute)
                    set(GregorianCalendar.SECOND, 0)
                    set(GregorianCalendar.MILLISECOND, 0)

                    add(GregorianCalendar.DAY_OF_YEAR, day)
                }
                val triggerDate = triggerCalendar.time

                return@firstNotNullResult if (triggerDate.after(curDate)) {
                    triggerDate.time
                } else {
                    null
                }
            }
        }.min()

        return if (nextTimeTriggered != null) {
            setting.copy(nextTimeTriggered = nextTimeTriggered)
        } else {
            setting.copy(nextTimeTriggered = -1)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEvent(event: ABCEvent) {
        GlobalScope.launch(Dispatchers.IO) { scheduleSurvey(context, event) }
    }

    override suspend fun onStart() {
        ABCEvent.register(this)
        cancelSurvey(context)
        scheduleSurvey(context)
        context.safeRegisterReceiver(receiver, filter)
    }

    override suspend fun onStop() {
        ABCEvent.unregister(this)
        cancelSurvey(context)
        context.safeUnregisterReceiver(receiver)
    }

    override fun checkAvailability(): Boolean  {
        val box = ObjBox.boxFor<SurveySettingEntity>() ?: return false
        return box.count() > 0L && context.checkPermission(requiredPermissions)
    }

    override val requiredPermissions: List<String>
        get() = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    override val newIntentForSetUp: Intent?
        get() = Intent(context, SurveySettingActivity::class.java)

    companion object {
        private const val EXTRA_SURVEY_UUID = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_UUID"
        private const val ACTION_SURVEY_TRIGGER = "${BuildConfig.APPLICATION_ID}.ACTION_SURVEY_TRIGGER"
        private const val REQUEST_CODE_SURVEY_OPEN = 0xdd
    }
}