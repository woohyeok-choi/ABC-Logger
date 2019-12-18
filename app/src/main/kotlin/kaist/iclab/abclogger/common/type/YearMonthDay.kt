package kaist.iclab.abclogger.common.type

import java.util.*

data class YearMonthDay(val year: Int, val month: Int, val day: Int) {
    fun isEmpty() : Boolean {
        return year == 0 && month == 0 && day == 0
    }

    companion object {
        fun fromString(str: String?) : YearMonthDay? {
            if(str == null) return null
            return try {
                YearMonthDay(
                    str.split(". ")[0].toInt(),
                    str.split(". ")[1].toInt(),
                    str.split(". ")[2].toInt()
                )
            } catch (e: Exception) {
                null
            }
        }

        fun empty() : YearMonthDay {
            return YearMonthDay(0, 0, 0)
        }

        fun now() : YearMonthDay {
            return GregorianCalendar.getInstance(TimeZone.getDefault()).let {
                YearMonthDay(
                    it.get(Calendar.YEAR),
                    it.get(Calendar.MONTH) + 1,
                    it.get(Calendar.DAY_OF_MONTH)
                )
            }
        }

        fun fromMillis(millis: Long) : YearMonthDay {
            return GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                timeInMillis = millis
            }.let {
                YearMonthDay(
                    it.get(Calendar.YEAR),
                    it.get(Calendar.MONTH) + 1,
                    it.get(Calendar.DAY_OF_MONTH)
                )
            }
        }
    }

    override fun toString(): String {
        return String.format("%s. %s. %s", year, month, day)
    }

}