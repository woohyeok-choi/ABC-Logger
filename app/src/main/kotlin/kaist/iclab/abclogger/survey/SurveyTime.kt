package kaist.iclab.abclogger.survey

import java.util.*
import java.util.concurrent.TimeUnit

data class SurveyTime(val value: Long, val unit: TimeUnit) {
    fun toMillis() : Long {
        return unit.toMillis(value)
    }

    /**
     * @param anchorTime
     *
     * This function is used to get next time after this time from anchor time.
     * If unit is TimeUnit.DAYS, it returns timestamp that all fields except for DAY are cleared.
     */
    fun getTimeInMillisAfterInterval(anchorTime: Long) : Long {
        if(value <= 0) return anchorTime

        return if(unit == TimeUnit.DAYS) {
            GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                timeInMillis = anchorTime
                add(GregorianCalendar.DAY_OF_YEAR, value.toInt())
                set(GregorianCalendar.HOUR_OF_DAY, 0)
                set(GregorianCalendar.MINUTE, 0)
                set(GregorianCalendar.MILLISECOND, 0)
                set(GregorianCalendar.SECOND, 0)
            }.timeInMillis
        } else {
            anchorTime + toMillis()
        }
    }
}