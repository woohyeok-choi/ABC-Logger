package kaist.iclab.abclogger.background

//import kaist.iclab.abclogger.data.FirestoreAccessor
import android.app.AlarmManager
import android.app.IntentService
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.annotation.WorkerThread
import androidx.core.app.AlarmManagerCompat
import android.util.Log
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.background.collector.LocationAndActivityCollector
import kaist.iclab.abclogger.common.ABCException
import kaist.iclab.abclogger.common.NoParticipatedExperimentException
import kaist.iclab.abclogger.common.util.FormatUtils
import kaist.iclab.abclogger.common.util.NotificationUtils
import kaist.iclab.abclogger.common.util.Utils
import kaist.iclab.abclogger.data.PreferenceAccessor
import kaist.iclab.abclogger.data.entities.LogEntity
import kaist.iclab.abclogger.data.entities.ParticipationEntity
import kaist.iclab.abclogger.data.entities.SurveyEntity
import kaist.iclab.abclogger.data.types.PhysicalActivityTransitionType
import kaist.iclab.abclogger.prefs
import kaist.iclab.abclogger.survey.Survey
import kaist.iclab.abclogger.survey.SurveyEventType
import kaist.iclab.abclogger.survey.SurveyPolicy
import kaist.iclab.abclogger.survey.SurveyTime
import java.util.*

object SurveyManager {
    private val TAG = SurveyManager::class.java.simpleName

    private const val MIN_DELAY_IN_MS : Long = 1000 * 60 * 15
    private const val MAX_DELAY_IN_MS : Long = 1000 * 60 * 60

    private const val REQUEST_CODE_ALARM = 0x00ff
    private const val PERIOD_SURVEY_CHECK_IN_MS = 1000 * 60 * 60 * 6

    private val EXTRA_SURVEY_EVENT_TYPE = "$TAG.EXTRA_SURVEY_EVENT_TYPE"

    class SurveyTriggerService : IntentService(SurveyTriggerService::class.java.name) {
        override fun onHandleIntent(intent: Intent?) {
            try {
                val entity = ParticipationEntity.getParticipationFromLocal()
                val survey = Survey.parse(entity.survey)    // PARSING TEST
                val pref = PreferenceAccessor.getInstance(this)
                val now = System.currentTimeMillis()

                if (pref.lastTimeSurveyChecked < 0) pref.lastTimeSurveyChecked = System.currentTimeMillis()

                if (now - pref.lastTimeSurveyChecked >= PERIOD_SURVEY_CHECK_IN_MS) {
                    notifyNotRespondedSurvey(this)
                    pref.lastTimeSurveyChecked = now
                }

                if (pref.nextSurveyTriggeredAt in 1..now) {
                    notifySurvey(this, entity, survey)
                    /*
                    Tasks.await(
                        FirestoreAccessor.setOrUpdate(
                            subjectEmail = entity.subjectEmail,
                            experimentUuid = entity.experimentUuid,
                            data = FirestoreAccessor.ExperimentData(lastTimeSurveyTriggered = now)
                        )
                    )*/
                    prefs.lastTimeSurveyTriggered = now
                }
                scheduleOnBackground(this, entity, survey)
            } catch (e: NoParticipatedExperimentException) {
                cancel(this)
            } catch (e: ABCException) {
                cancel(this)
            } catch (e: Exception) {
                scheduleWithNothing(this)
            }
        }
    }

    class SurveyScheduleService : IntentService(SurveyScheduleService::class.java.name) {
        override fun onHandleIntent(intent: Intent?) {
            val eventTypes = intent?.getStringArrayExtra(EXTRA_SURVEY_EVENT_TYPE)?.mapNotNull {
                try { SurveyEventType.valueOf(it) } catch (e: Exception) { null }
            }
            scheduleOnBackground(this, eventTypes = eventTypes)
        }
    }

    val EventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == LocationAndActivityCollector.ACTION_ACTIVITY_TRANSITION_AVAILABLE) {
                val eventType = extractTriggerEvent(intent)

                if(eventType != null) {
                    schedule(context, eventType)
                }
            }
        }
    }

    fun schedule(context: Context, eventTypes: List<SurveyEventType>? = null) {
        if(Looper.myLooper() == Looper.getMainLooper()) {
            context.startService(
                Intent(context, SurveyScheduleService::class.java).apply {
                    if (eventTypes?.isNotEmpty() == true) putExtra(EXTRA_SURVEY_EVENT_TYPE, eventTypes.map { it.name }.toTypedArray())
                }
            )
        } else {
            scheduleOnBackground(context, eventTypes = eventTypes)
        }
    }

    fun scheduleWithNothing (context: Context) {
        val alarmManager =  context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)
        val pref = PreferenceAccessor.getInstance(context)
        alarmManager.cancel(pendingIntent)
        pref.nextSurveyTriggeredAt = Long.MIN_VALUE

        AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + MIN_DELAY_IN_MS, pendingIntent)
    }

    @WorkerThread
    private fun scheduleOnBackground(context: Context, experimentEntity: ParticipationEntity? = null, surveyData: Survey? = null, eventTypes: List<SurveyEventType>? = null) {
        val pref = PreferenceAccessor.getInstance(context)
        val now = System.currentTimeMillis()
        val alarmManager =  context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)

        try {
            pref.nextSurveyTriggeredAt = Long.MIN_VALUE

            val entity = experimentEntity ?: ParticipationEntity.getParticipationFromLocal()
            val policy = surveyData?.policy ?: Survey.parse(entity.survey).policy

            val participateTime = entity.participateTime
            Log.d(TAG, entity.survey)
            /*
            val lastSurveyTriggeredTime = Tasks.await(
                FirestoreAccessor.get(
                    subjectEmail = entity.subjectEmail,
                    experimentUuid = entity.experimentUuid)
            )?.lastTimeSurveyTriggered ?: Long.MIN_VALUE
            */
            val lastSurveyTriggeredTime = prefs.lastTimeSurveyTriggered

            if(lastSurveyTriggeredTime > 0 && policy.isEventBased()) {
                if(eventTypes?.intersect(policy.triggerEvents)?.isNotEmpty() == true) pref.lastTimeSurveyTriggerEventOccurs = now
                if(eventTypes?.intersect(policy.cancelEvents)?.isNotEmpty() == true) pref.lastTimeSurveyCancelEventOccurs = now
            }

            val triggeredAt = deriveTriggerTimeInMs(
                policy = policy,
                participateTime = participateTime,
                lastTriggerEventTime = pref.lastTimeSurveyTriggerEventOccurs,
                lastCancelEventTime = pref.lastTimeSurveyCancelEventOccurs,
                lastSurveyTriggeredTime = lastSurveyTriggeredTime
            )

            pref.nextSurveyTriggeredAt = triggeredAt ?: Long.MIN_VALUE

            val actualTriggeredAt = if(triggeredAt == null || triggeredAt - now > MAX_DELAY_IN_MS ) {
                now + MIN_DELAY_IN_MS
            } else {
                Math.max(triggeredAt, now + 5000)
            }
            alarmManager.cancel(pendingIntent)
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, actualTriggeredAt, pendingIntent)

            LogEntity.log(TAG, "scheduleOnBackground(): " +
                "participatedTime = ${FormatUtils.formatLogTime(entity.participateTime) ?: "None"}, " +
                "lastSurveyTriggerTime = ${FormatUtils.formatLogTime(lastSurveyTriggeredTime) ?: "None"}, " +
                "lastTriggerEventTime = ${FormatUtils.formatLogTime(pref.lastTimeSurveyTriggerEventOccurs) ?: "None"}, " +
                "lastCancelEventTime = ${FormatUtils.formatLogTime(pref.lastTimeSurveyCancelEventOccurs) ?: "None"}, " +
                "intendedTriggerAt = ${FormatUtils.formatLogTime(pref.nextSurveyTriggeredAt) ?: "None"}, " +
                "actualTriggerAt = ${FormatUtils.formatLogTime(actualTriggeredAt) ?: "None"}"
            )
        } catch (e: Exception) {
            alarmManager.cancel(pendingIntent)
            pref.nextSurveyTriggeredAt = Long.MIN_VALUE

            Log.d(TAG, e.toString())

            LogEntity.log(TAG, "scheduleOnBackground(): canceled because of ${e::class.java.name} = ${e.message}")
        }
    }

    fun cancel(context: Context) {
        val alarmManager =  context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)

        alarmManager.cancel(pendingIntent)

        LogEntity.log(TAG, "cancel()")
    }

    private fun deriveTriggerTimeInMs(policy: SurveyPolicy,
                                      participateTime: Long,
                                      lastSurveyTriggeredTime: Long = Long.MIN_VALUE,
                                      lastTriggerEventTime: Long = Long.MIN_VALUE,
                                      lastCancelEventTime: Long = Long.MIN_VALUE) : Long? {

        val isTriggerEventOccurs = lastCancelEventTime <= lastTriggerEventTime && lastTriggerEventTime > 0
        val isCancelEventOccurs = lastTriggerEventTime < lastCancelEventTime && lastCancelEventTime > 0

        Log.d(TAG, "deriveTriggerTimeInMs(lastSurveyTriggeredTime = $lastSurveyTriggeredTime, lastTriggerEventTime = $lastTriggerEventTime, lastCancelEventTime = $lastCancelEventTime)")

        val triggeredAt = when {
            lastSurveyTriggeredTime <= 0 -> policy.initialDelay.getTimeInMillisAfterInterval(participateTime)
            policy.isEventBased() && isTriggerEventOccurs -> policy.minInterval.getTimeInMillisAfterInterval(Math.max(lastTriggerEventTime, lastSurveyTriggeredTime)) +
                deriveFlexTimeInMillis(policy.flexInterval)
            policy.isEventBased() && isCancelEventOccurs -> null
            else -> policy.minInterval.getTimeInMillisAfterInterval(lastSurveyTriggeredTime) + deriveFlexTimeInMillis(policy.flexInterval)
        }

        return triggeredAt?.let { policy.getMostRecentTriggerTime(it) }
    }

    private fun buildPendingIntent(context: Context) = PendingIntent.getService(
        context,
        REQUEST_CODE_ALARM,
        Intent(context, SurveyTriggerService::class.java),
        PendingIntent.FLAG_CANCEL_CURRENT
    )

    private fun notifyNotRespondedSurvey(context: Context) {
        val now = System.currentTimeMillis()
        val participationEntity = try { ParticipationEntity.getParticipationFromLocal() } catch (e: Exception) { null } ?: return

        val numberNotRepliedEntities = SurveyEntity.numberNotRepliedEntities(participationEntity, now)
        if(numberNotRepliedEntities > 0) {
            NotificationUtils.notifySurveyRemained(context, numberNotRepliedEntities)
        }
    }

    private fun notifySurvey(context: Context, entity: ParticipationEntity, survey: Survey) {

        val box = App.boxFor<SurveyEntity>()
        var surveyEntity = SurveyEntity(
            title = survey.title,
            message = survey.message ?: "",
            timeoutPolicy = survey.policy.timeoutPolicyType,
            timeout = survey.policy.timeout,
            responses = entity.survey
        )
        val entityId = box.put(surveyEntity)

        surveyEntity = surveyEntity.copy(
            deliveredTime = System.currentTimeMillis()
        ).apply {
            id = entityId
            utcOffset = Utils.utcOffsetInHour()
            experimentUuid = entity.experimentUuid
            experimentGroup = entity.experimentGroup
            subjectEmail = entity.subjectEmail
            isUploaded = false
        }
        box.put(surveyEntity)
        Log.d(TAG, "Box.put(" +
            "timestamp = ${surveyEntity.timestamp}, subjectEmail = ${surveyEntity.subjectEmail}, experimentUuid = ${surveyEntity.experimentUuid}, " +
            "experimentGroup = ${surveyEntity.experimentGroup}, entity = $surveyEntity)")

        NotificationUtils.notifySurveyDelivered(
            context = context,
            entity = surveyEntity,
            number = SurveyEntity.numberNotRepliedEntities(entity, System.currentTimeMillis())
        )

        LogEntity.log(TAG, "notifySurvey()")
    }

    private fun extractTriggerEvent(intent: Intent) : List<SurveyEventType>? {
        return intent.getStringArrayExtra(LocationAndActivityCollector.EXTRA_ACTIVITY_TRANSITIONS).mapNotNull {
            try { PhysicalActivityTransitionType.valueOf(it) } catch (e: Exception) {null }
        }
    }

    private fun deriveFlexTimeInMillis(flex: SurveyTime) : Long {
        val random = Random(System.currentTimeMillis())
        val flexMillis = flex.toMillis()
        return if(flexMillis > 0) random.nextInt(flexMillis.toInt()).toLong() else 0
    }
}