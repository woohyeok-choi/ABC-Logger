package kaist.iclab.abclogger.common.type

import java.util.*

enum class DayOfWeek(override val id: Int): HasId {
    NONE(-1),
    SUNDAY(GregorianCalendar.SUNDAY),
    MONDAY(GregorianCalendar.MONDAY),
    TUESDAY(GregorianCalendar.TUESDAY),
    WEDNESDAY(GregorianCalendar.WEDNESDAY),
    THURSDAY(GregorianCalendar.THURSDAY),
    FRIDAY(GregorianCalendar.FRIDAY),
    SATURDAY(GregorianCalendar.SATURDAY);

    companion object : EnumMap<DayOfWeek>(buildValueMap()) {
        fun now() : DayOfWeek {
            return DayOfWeek.fromValue(GregorianCalendar.getInstance(TimeZone.getDefault()).get(GregorianCalendar.DAY_OF_WEEK), NONE)
        }

        fun fromString(str: String): DayOfWeek {
            return try {
                DayOfWeek.valueOf(str.toUpperCase())
            } catch (e: IllegalArgumentException) {
                DayOfWeek.NONE
            }
        }

        fun fromMillis(millis: Long) : DayOfWeek {
            return DayOfWeek.fromValue(
                GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                    timeInMillis = millis
                }.get(GregorianCalendar.DAY_OF_WEEK),
                NONE
            )
        }
    }

    fun isWeekend() : Boolean {
        return this == SUNDAY || this == SATURDAY
    }

    fun isWeekDay() : Boolean {
        return !isWeekend()
    }

    fun next() : DayOfWeek {
        return DayOfWeek.fromValue(id % 7 + 1, NONE)
    }

    fun toMillis(anchorTime: Long) : Long {
        val anchorDay = DayOfWeek.fromMillis(anchorTime)
        var diff = id - anchorDay.id
        if(diff < 0) diff += 7
        return anchorTime + diff * 1000 * 60 * 60 * 24
    }
}