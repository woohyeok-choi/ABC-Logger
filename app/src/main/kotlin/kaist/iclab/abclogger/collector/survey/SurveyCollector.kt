package kaist.iclab.abclogger.collector.survey

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import io.objectbox.kotlin.inValues
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.core.Event
import kaist.iclab.abclogger.core.EventBus
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.*
import kaist.iclab.abclogger.core.collector.*
import kaist.iclab.abclogger.structure.survey.*
import kaist.iclab.abclogger.ui.settings.survey.SurveySettingActivity
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SurveyCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<SurveyEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    var configurations by ReadWriteStatusJson(
        default = listOf(),
        adapter = SurveyConfiguration.ListAdapter
    )

    var baseScheduleDate by ReadWriteStatusLong(Long.MIN_VALUE)

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val timestamp = System.currentTimeMillis()

            if (intent?.action == ACTION_SCHEDULE) {
                handleSchedule(intent.getLongExtra(EXTRA_SURVEY_ID, 0), timestamp, null)
            }
        }
    }

    override fun isAvailable(): Boolean = configurations.isNotEmpty()

    override fun getDescription(): Array<Description> = arrayOf(
        R.string.collector_survey_info_base_date with formatDateTime(context, baseScheduleDate)
    )

    override val permissions: List<String> = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override val setupIntent: Intent? = Intent(context, SurveySettingActivity::class.java)

    override suspend fun onStart() {
        if (baseScheduleDate < 0) {
            baseScheduleDate = System.currentTimeMillis()
        }

        EventBus.register(this)
        context.safeRegisterReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_SCHEDULE)
            addAction(ACTION_EMPTY)
        })
    }

    override suspend fun onStop() {
        EventBus.unregister(this)
        context.safeUnregisterReceiver(receiver)
    }

    override suspend fun count(): Long = dataRepository.count<SurveyEntity>()

    override suspend fun flush(entities: Collection<SurveyEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<SurveyEntity> = dataRepository.find(0, limit)

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEvent(event: Event) {
        val events = configurations.flatMap { setting ->
            (setting.survey.intraDaySchedule as? EventSchedule)?.let {
                it.eventsTrigger + it.eventsCancel
            } ?: listOf()
        }.toSet()

        if (event.type in events) handleSchedule(
            id = 0,
            timestamp = event.timestamp,
            event = event.type
        )
    }

    private fun handleSchedule(id: Long, timestamp: Long, event: String? = null) = launch {
        if (id > 0) {
            trigger(id, timestamp)
        }

        configurations.forEach { setting ->
            schedule(baseScheduleDate.takeIf { it > 0 } ?: timestamp, timestamp, setting, event)
        }

        timer(timestamp)
    }

    private suspend fun schedule(
        baseScheduleTime: Long,
        timestamp: Long,
        setting: SurveyConfiguration,
        event: String? = null
    ) {
        val baseTime = baseScheduleTime.takeIf { it >= 0 } ?: return

        val dateBase = LocalDate.fromMillis(baseTime)
        val dateCurrent = LocalDate.fromMillis(timestamp)

        val uuid = setting.uuid
        val survey = setting.survey
        val intraDaySchedule = survey.intraDaySchedule
        val interDaySchedule = survey.interDaySchedule

        val latestSchedule = dataRepository.findFirst<InternalSurveyEntity> {
            equal(InternalSurveyEntity_.uuid, uuid)
            orderDesc(InternalSurveyEntity_.intendedTriggerTime)
        }

        val dateFrom = if (latestSchedule == null) {
            dateBase + (survey.timeFrom.takeIf { !it.isNone() } ?: Duration.MIN)
        } else {
            LocalDate.fromMillis(latestSchedule.intendedTriggerTime) + 1
        }

        if (dateCurrent + DAYS_MARGIN_FOR_SCHEDULE < dateFrom) return

        val dateTo = if (survey.timeTo.isNone()) {
            dateFrom + DAYS_SCHEDULE
        } else {
            dateBase + survey.timeTo
        }

        if (dateTo < dateCurrent) return

        val scheduledDates = scheduleInterDay(
            dateFrom, dateTo, interDaySchedule
        ).takeIf { it.isNotEmpty() } ?: return

        val scheduledTimes = when (intraDaySchedule) {
            is TimeSchedule -> scheduleIntraDay(intraDaySchedule)
            is IntervalSchedule -> scheduleIntraDay(intraDaySchedule)
            is EventSchedule -> scheduleIntraDay(
                intraDaySchedule,
                uuid,
                event,
                LocalTime.fromLocalDateTime(LocalDateTime.fromMillis(timestamp))
            )
            else -> listOf()
        }.takeIf { it.isNotEmpty() } ?: return

        val scheduledDateTimes = scheduledDates.combination(scheduledTimes) { date, time ->
            date + time
        }.filter { dateTime -> dateTime in (dateFrom..dateTo) }

        val eventsTrigger =
            (survey.intraDaySchedule as? EventSchedule)?.eventsTrigger ?: listOf()
        val eventName = if (event != null && event in eventsTrigger) event else ""

        val entities = scheduledDateTimes.map { dateTime ->
            InternalSurveyEntity(
                uuid = uuid,
                eventTime = if (eventName.isNotBlank()) timestamp else Long.MIN_VALUE,
                eventName = eventName,
                intendedTriggerTime = dateTime.toMillis(),
                url = setting.url,
                title = survey.title,
                message = survey.message,
                instruction = survey.instruction,
                timeoutUntil = if (survey.timeoutAction != Survey.TimeoutAction.NONE) {
                    (dateTime + survey.timeout).toMillis()
                } else {
                    Long.MIN_VALUE
                },
                timeoutAction = survey.timeoutAction
            )
        }
        putAll(entities, isStatUpdates = false)
    }

    private suspend fun timer(timestamp: Long) {
        val schedulesNotTriggered = dataRepository.find<InternalSurveyEntity> {
            inValues(InternalSurveyEntity_.uuid, configurations.map { it.uuid }.toTypedArray())
                .and()
                .less(InternalSurveyEntity_.actualTriggerTime, 0)
                .and()
                .less(InternalSurveyEntity_.intendedTriggerTime, timestamp)
                .or()
                .equal(InternalSurveyEntity_.intendedTriggerTime, timestamp)

            order(InternalSurveyEntity_.intendedTriggerTime)
        }.sortedBy { entity -> entity.intendedTriggerTime }

        schedulesNotTriggered.forEach { entity ->
            trigger(entity.id, timestamp)
        }

        val latestSchedule = dataRepository.findFirst<InternalSurveyEntity> {
            inValues(InternalSurveyEntity_.uuid, configurations.map { it.uuid }.toTypedArray())
            greater(InternalSurveyEntity_.intendedTriggerTime, timestamp)
            order(InternalSurveyEntity_.intendedTriggerTime)
        }

        val alarmManager = context.getSystemService<AlarmManager>() ?: return

        if (latestSchedule == null) {
            val triggerIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE_ACTION_SCHEDULE,
                Intent(ACTION_SCHEDULE),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                timestamp + INTERVAL_CHECK_SCHEDULE,
                triggerIntent
            )
        } else {
            val triggerIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE_ACTION_SCHEDULE,
                Intent(ACTION_SCHEDULE).putExtra(
                    EXTRA_SURVEY_ID, latestSchedule.id
                ), PendingIntent.FLAG_UPDATE_CURRENT
            )

            val showIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ACTION_EMPTY,
                Intent(ACTION_EMPTY),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            AlarmManagerCompat.setAlarmClock(
                alarmManager,
                latestSchedule.intendedTriggerTime,
                showIntent,
                triggerIntent
            )
        }
    }

    private suspend fun trigger(id: Long, timestamp: Long) {
        val entity = dataRepository.get<InternalSurveyEntity>(id) ?: return
        val uuid = entity.uuid
        val setting = configurations.firstOrNull { it.uuid == uuid } ?: return
        val survey = setting.survey
        val isAltTextShown = entity.isAltTextShown(timestamp)

        val responses = survey.question.mapIndexed { idx, question ->
            InternalResponseEntity(
                surveyId = id,
                index = idx,
                question = question,
                answer = InternalAnswer(question.option.type != Option.Type.CHECK_BOX)
            )
        }

        put(entity.copy(actualTriggerTime = timestamp))
        putAll(responses, isStatUpdates = false)

        NotificationRepository.notifySurvey(
            context = context,
            timestamp = timestamp,
            entityId = id,
            title = entity.title.text(isAltTextShown),
            message = entity.message.text(isAltTextShown)
        )
    }

    private fun scheduleInterDay(
        dateFrom: LocalDate,
        dateTo: LocalDate,
        schedule: InterDaySchedule
    ): List<LocalDate> {
        val localDates = when (schedule) {
            is DateSchedule -> schedule.dates
            is DayOfWeekSchedule -> (dateFrom..dateTo).filter {
                it.toDayOfWeek() in schedule.daysOfWeek
            }
            is DailySchedule -> (dateFrom..dateTo).filter {
                it !in schedule.exceptDates
            }
        }

        return localDates.filter { it in dateFrom..dateTo }
    }

    private fun scheduleIntraDay(schedule: TimeSchedule): List<LocalTime> = schedule.times

    private fun scheduleIntraDay(schedule: IntervalSchedule): List<LocalTime> {
        val times = mutableListOf<LocalTime>()

        val intervalDefault = schedule.intervalDefault.coerceAtLeast(Duration.MIN).toMillis()
        val intervalFlex = schedule.intervalFlex.coerceAtLeast(Duration.MIN).toMillis()

        val from = schedule.timeFrom.coerceIn(LocalTime.MIN, LocalTime.MAX)
        val to = schedule.timeTo.coerceIn(LocalTime.MIN, LocalTime.MAX)

        var next = from - (intervalDefault + intervalFlex)

        while (next <= to) {
            val interval = intervalDefault + Random.nextLong(intervalFlex)
            val time = next + interval
            if (time in from..to) times.add(time)

            next = time
        }
        return times
    }

    private suspend fun scheduleIntraDay(
        schedule: EventSchedule,
        uuid: String,
        event: String?,
        eventTime: LocalTime
    ): List<LocalTime> {
        if (event in schedule.eventsCancel) {
            dataRepository.remove<InternalSurveyEntity> {
                equal(InternalSurveyEntity_.uuid, uuid)
                less(InternalSurveyEntity_.actualTriggerTime, 0)
            }
        }

        if (event in schedule.eventsTrigger) {
            val fromTime = schedule.timeFrom.coerceIn(LocalTime.MIN, LocalTime.MAX)
            val toTime = schedule.timeTo.coerceIn(LocalTime.MIN, LocalTime.MAX)

            val delayDefault = schedule.delayDefault.coerceAtLeast(Duration.MIN).toMillis()
            val delayFlex = schedule.delayFlex.coerceAtLeast(Duration.MIN).toMillis()
            val delay = delayDefault + Random.nextLong(delayFlex)

            val time = eventTime + delay

            if (time in (fromTime..toTime)) {
                return listOf(time)
            }
        }

        return listOf()
    }

    companion object {
        private const val DAYS_SCHEDULE = 7
        private const val DAYS_MARGIN_FOR_SCHEDULE = 3

        private const val ACTION_EMPTY =
            "${BuildConfig.APPLICATION_ID}.collector.survey.SurveyCollector.ACTION_EMPTY"
        private const val REQUEST_CODE_ACTION_EMPTY = 0x01

        private const val ACTION_SCHEDULE =
            "${BuildConfig.APPLICATION_ID}.collector.survey.SurveyCollector.ACTION_SCHEDULE"
        private const val REQUEST_CODE_ACTION_SCHEDULE = 0x02
        private const val EXTRA_SURVEY_ID =
            "${BuildConfig.APPLICATION_ID}.collector.survey.SurveyCollector.EXTRA_SURVEY_ID"

        private val INTERVAL_CHECK_SCHEDULE = TimeUnit.MINUTES.toMillis(30)
    }
}
