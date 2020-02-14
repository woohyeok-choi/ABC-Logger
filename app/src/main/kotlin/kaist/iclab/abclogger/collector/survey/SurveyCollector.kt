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
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.collector.survey.setting.SurveySettingActivity
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.ui.question.SurveyResponseActivity
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.reflect.KClass

class SurveyCollector(private val context: Context) : BaseCollector<SurveyCollector.Status>(context) {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null,
                      val startTime: Long? = null,
                      val nReceived: Int? = null,
                      val nAnswered: Int? = null,
                      val settings: List<Setting>? = null) : BaseStatus() {
        override fun info(): String = ""

        data class Setting(
                val id: Int = 0,
                val uuid: String? = null,
                var url: String? = null,
                val json: String? = null,
                val lastTimeTriggered: Long? = null,
                val nextTimeTriggered: Long? = null
        )
    }

    override val clazz: KClass<Status> = Status::class

    override val name: String = context.getString(R.string.data_name_survey)

    override val description: String = context.getString(R.string.data_desc_survey)

    override val requiredPermissions: List<String> = listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override val newIntentForSetUp: Intent? = Intent(context, SurveySettingActivity::class.java)

    override suspend fun checkAvailability(): Boolean =
            !getStatus()?.settings.isNullOrEmpty() && context.checkPermission(requiredPermissions)

    override suspend fun onStart() {
        AbcEvent.register(this)

        val prevStartTime = getStatus()?.startTime ?: 0
        if (prevStartTime <= 0) setStatus(Status(startTime = System.currentTimeMillis()))

        cancelAll()
        scheduleAll()

        context.safeRegisterReceiver(receiver, filter)
    }

    override suspend fun onStop() {
        AbcEvent.unregister(this)
        cancelAll()

        context.safeUnregisterReceiver(receiver)
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != ACTION_SURVEY_TRIGGER) return

                handleSurveyTrigger(intent.getStringExtra(EXTRA_SURVEY_UUID))
            }
        }
    }

    private val filter = IntentFilter().apply { addAction(ACTION_SURVEY_TRIGGER) }

    private fun handleSurveyTrigger(uuid: String?) = launch {
        val curTime = System.currentTimeMillis()
        val settings = getStatus()?.settings ?: return@launch

        val updatedSettings = settings.map { setting ->
            if (setting.uuid == uuid) setting.copy(lastTimeTriggered = curTime) else setting.copy()
        }

        setStatus(Status(lastTime = curTime, settings = updatedSettings))

        cancelAll()
        scheduleAll()

        val curSetting = settings.find { setting -> setting.uuid == uuid } ?: return@launch
        val json = curSetting.json ?: return@launch
        val survey = json.let { Survey.fromJson(it) } ?: return@launch

        val shouldNotify = when (survey) {
            is IntervalBasedSurvey -> checkTriggerCondition(
                    curTime = curTime,
                    dailyStartTimeHour = survey.dailyStartTimeHour,
                    dailyStartTimeMinute = survey.dailyStartTimeMinute,
                    dailyEndTimeHour = survey.dailyEndTimeHour,
                    dailyEndTimeMinute = survey.dailyEndTimeMinute,
                    daysOfWeek = survey.daysOfWeek
            )
            is EventBasedSurvey -> checkTriggerCondition(
                    curTime = curTime,
                    dailyStartTimeHour = survey.dailyStartTimeHour,
                    dailyStartTimeMinute = survey.dailyStartTimeMinute,
                    dailyEndTimeHour = survey.dailyEndTimeHour,
                    dailyEndTimeMinute = survey.dailyEndTimeMinute,
                    daysOfWeek = survey.daysOfWeek
            )
            else -> true
        }

        if (shouldNotify) {
            notify(curTime, survey, json)

            val nDelivered = getStatus()?.nReceived ?: 0

            setStatus(Status(nReceived = nDelivered + 1))
        }
    }

    private suspend fun scheduleAll(event: AbcEvent? = null) {
        val settings = updateSettings(event)
        setStatus(Status(settings = settings))

        settings?.forEach { setting -> scheduleSurvey(setting) }
    }

    private suspend fun cancelAll() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        getStatus()?.settings?.forEach { setting ->
            alarmManager.cancel(getPendingIntent(id = setting.id, uuid = setting.uuid ?: ""))
        }
    }

    private fun checkTriggerCondition(curTime: Long,
                                      dailyStartTimeHour: Int,
                                      dailyStartTimeMinute: Int,
                                      dailyEndTimeHour: Int,
                                      dailyEndTimeMinute: Int,
                                      daysOfWeek: Array<DayOfWeek>) : Boolean {
        val curCalendar = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = curTime
        }
        val curDayOfWeek = DayOfWeek.from(curCalendar.get(Calendar.DAY_OF_WEEK))
        val curHour = curCalendar.get(Calendar.HOUR_OF_DAY)
        val curMinute = curCalendar.get(Calendar.MINUTE)

        val dailyStartTimeAsMinute = TimeUnit.HOURS.toMinutes(dailyStartTimeHour.toLong()) + dailyStartTimeMinute
        val dailyEndTimeAsMinute = TimeUnit.HOURS.toMinutes(dailyEndTimeHour.toLong()) + dailyEndTimeMinute
        val curTimeAsMinute = TimeUnit.HOURS.toMinutes(curHour.toLong()) + curMinute

        if (curDayOfWeek !in daysOfWeek) return false
        if (curTimeAsMinute !in (dailyStartTimeAsMinute..dailyEndTimeAsMinute)) return false

        return true
    }

    private fun notify(curTime: Long, survey: Survey, json: String) {
        val id = SurveyEntity(
                title = survey.title,
                message = survey.message,
                timeoutPolicy = survey.timeoutPolicy,
                timeoutSec = survey.timeoutSec,
                deliveredTime = curTime,
                json = json
        ).fill(timeMillis = curTime).let { entity ->
            ObjBox.put(entity)
        }

        if (id < 0) return

        val surveyIntent = Intent(context, SurveyResponseActivity::class.java).fillExtras(
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
                intent = pendingIntent,
                timeoutMs = if (survey.timeoutPolicy == Survey.TIMEOUT_DISABLED) TimeUnit.SECONDS.toMillis(survey.timeoutSec) else null
        )

        NotificationManagerCompat.from(context).notify(Notifications.ID_SURVEY_DELIVERED, notification)
    }

    private fun getPendingIntent(id: Int, uuid: String) = PendingIntent.getBroadcast(
            context, id,
            Intent(ACTION_SURVEY_TRIGGER).putExtra(EXTRA_SURVEY_UUID, uuid),
            PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun scheduleSurvey(setting: Status.Setting) {
        val nextTimeTriggered = setting.nextTimeTriggered ?: 0
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = getPendingIntent(setting.id, setting.uuid ?: "")

        if (nextTimeTriggered > 0) {
            val curTime = System.currentTimeMillis()
            val triggerTime = max(curTime + TimeUnit.SECONDS.toMillis(10), nextTimeTriggered)
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.cancel(pendingIntent)
        }
    }

    private suspend fun updateSettings(event: AbcEvent? = null) : List<Status.Setting>? {
        val startTime = getStatus()?.startTime ?: return null

        return getStatus()?.settings?.mapNotNull { setting ->
            setting.json?.let { json -> Survey.fromJson(json) }?.let { survey ->
                when (survey) {
                    is IntervalBasedSurvey -> updateIntervalBasedSurvey(survey = survey, setting = setting, startTime = startTime)
                    is ScheduleBasedSurvey -> updateScheduleBasedSurvey(survey = survey, setting = setting)
                    is EventBasedSurvey -> updateEventBasedSurvey(survey = survey, setting = setting, event = event)
                    else -> null
                }
            }
        }
    }

    private fun updateIntervalBasedSurvey(survey: IntervalBasedSurvey,
                                          setting: Status.Setting,
                                          startTime: Long): Status.Setting {
        val curTime = System.currentTimeMillis()

        val initDelayMs = TimeUnit.SECONDS.toMillis(max(survey.initialDelaySec, 0))
        val intervalMs = TimeUnit.SECONDS.toMillis(max(survey.intervalSec, 0))
        val flexMs = TimeUnit.SECONDS.toMillis(max(safeRandom(survey.flexIntervalSec), 0))

        val initialTriggerAt: Long = startTime + initDelayMs
        val lastTimeTriggered: Long = setting.lastTimeTriggered ?: initialTriggerAt
        val nextTimeTriggered: Long = setting.nextTimeTriggered ?: 0

        /**
         * Case 1: curTime < initialTriggerAt
         * - nextTriggeredTime = initialTriggerAt
         *
         * Case 2: curTime >= initial
         * * Case 2.1: lastTimeTriggered == null && nextTimeTriggered == null
         *   - lastTimeTriggered = initialTriggerAt
         *   - nextTimeTriggered = initialTriggerAt + intervals
         *
         * * Case 2.2: lastTimeTriggered != null && nextTimeTriggered == null
         *   - nextTimeTriggered = lastTimeTriggered + intervals
         *
         * * Case 2.3: lastTimeTriggered == null && nextTimeTriggered != null
         *   * Case 2.3.1: nextTimeTriggered >= curTime
         *     - Just copy the previous setting
         *   * Case 2.3.2: nextTimeTriggered < curTime
         *     - nextTimeTriggered = initialTriggerAt + intervals
         *
         * * Case 2.4: lastTimeTriggered != null && nextTimeTriggered != null
         *   * Case 2.3.1: nextTimeTriggered >= curTime
         *     - Just copy the previous setting
         *   * Case 2.3.2: nextTimeTriggered < curTime
         *     - nextTimeTriggered = lastTimeTriggered + intervals
         */

        return if (curTime < initialTriggerAt) {
            setting.copy(
                    nextTimeTriggered = initialTriggerAt,
                    lastTimeTriggered = null
            )
        } else {
            if (curTime <= nextTimeTriggered) {
                setting.copy()
            } else {
                setting.copy(
                        nextTimeTriggered = lastTimeTriggered + intervalMs + flexMs
                )
            }
        }
    }

    private fun updateEventBasedSurvey(survey: EventBasedSurvey,
                                       setting: Status.Setting,
                                       event: AbcEvent? = null): Status.Setting {
        val intervalMs = max(TimeUnit.SECONDS.toMillis(survey.delayAfterTriggerEventSec), 0)
        val flexMs = TimeUnit.SECONDS.toMillis(max(safeRandom(survey.flexDelayAfterTriggerEventSec), 0))

        return when (event?.eventType) {
            in survey.triggerEvents -> setting.copy(
                    nextTimeTriggered = event?.timestamp?.plus(intervalMs + flexMs) ?: 0
            )
            in survey.cancelEvents -> setting.copy(
                    nextTimeTriggered = 0
            )
            else -> setting.copy()
        }
    }

    private fun updateScheduleBasedSurvey(survey: ScheduleBasedSurvey, setting: Status.Setting): Status.Setting {
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
        }.min() ?: 0

        return setting.copy(nextTimeTriggered = nextTimeTriggered)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEvent(event: AbcEvent) {
        launch { scheduleAll(event) }
    }

    companion object {
        private const val EXTRA_SURVEY_UUID = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_UUID"
        private const val ACTION_SURVEY_TRIGGER = "${BuildConfig.APPLICATION_ID}.ACTION_SURVEY_TRIGGER"
        private const val REQUEST_CODE_SURVEY_OPEN = 0xdd
    }
}