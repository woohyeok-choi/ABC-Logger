package kaist.iclab.abclogger.survey
import kaist.iclab.abclogger.common.type.DayOfWeek
import kaist.iclab.abclogger.common.type.HourMin
import kaist.iclab.abclogger.common.type.HourMinRange
import java.util.*
import java.util.concurrent.TimeUnit

data class SurveyPolicy (
    val initialDelay: SurveyTime = SurveyTime(0, TimeUnit.MILLISECONDS),
    val daysOfWeek: List<DayOfWeek> = DayOfWeek.values().toList(),
    val dailySurveyTime: HourMinRange = HourMinRange(HourMin(0, 0), HourMin(24, 0)),
    val minInterval: SurveyTime = SurveyTime(1, TimeUnit.MINUTES),
    val flexInterval: SurveyTime = SurveyTime(0, TimeUnit.MILLISECONDS),
    val triggerEvents : List<SurveyEventType> = listOf(),
    val cancelEvents: List<SurveyEventType> = listOf(),
    val timeout: SurveyTime = SurveyTime(Long.MIN_VALUE, TimeUnit.MILLISECONDS),
    val timeoutPolicyType: SurveyTimeoutPolicyType = SurveyTimeoutPolicyType.NONE
) {
    fun isEventBased() : Boolean {
        return triggerEvents.isNotEmpty() &&
            cancelEvents.isNotEmpty() &&
            SurveyEventType.NONE !in triggerEvents &&
            SurveyEventType.NONE !in cancelEvents &&
            triggerEvents.intersect(cancelEvents).isEmpty()
    }

    fun getMostRecentTriggerTime(triggeredAtMillis: Long) : Long {
        var millis = triggeredAtMillis

        for (i in 0..6) {
            val isValid =
                millis >= triggeredAtMillis &&
                (DayOfWeek.fromMillis(millis) in daysOfWeek || daysOfWeek.isEmpty()) &&
                dailySurveyTime.isInRange(HourMin.fromMillis(millis))

            if(isValid) break

            millis = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                timeInMillis = triggeredAtMillis + TimeUnit.DAYS.toMillis(i.toLong())
                set(GregorianCalendar.HOUR_OF_DAY, dailySurveyTime.from.hour)
                set(GregorianCalendar.MINUTE, dailySurveyTime.from.minute)
                set(GregorianCalendar.SECOND, 0)
                set(GregorianCalendar.MILLISECOND, 0)
            }.timeInMillis
        }

        return millis
    }
}