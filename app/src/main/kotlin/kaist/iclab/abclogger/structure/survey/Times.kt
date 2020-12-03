package kaist.iclab.abclogger.structure.survey


import java.util.*
import java.util.concurrent.TimeUnit

data class Duration(
    val amount: Long,
    val unit: Unit
) : Comparable<Duration> {
    enum class Unit {
        MILLISECOND,
        SECOND,
        MINUTE,
        HOUR,
        DAY
    }

    override fun compareTo(other: Duration): Int = toMillis().compareTo(other.toMillis())

    fun isNone() = amount < 0

    companion object {
        val NONE = Duration(-1, Unit.MILLISECOND)
        val MIN = Duration(0, Unit.MILLISECOND)

        fun milliseconds(amount: Int) = Duration(amount.toLong(), Unit.MILLISECOND)
        fun seconds(amount: Int) = Duration(amount.toLong(), Unit.SECOND)
        fun minutes(amount: Int) = Duration(amount.toLong(), Unit.MINUTE)
        fun hours(amount: Int) = Duration(amount.toLong(), Unit.HOUR)
        fun days(amount: Int) = Duration(amount.toLong(), Unit.DAY)
    }
}

/**
 * Class for representing local date time.
 * Here, month is not-zero based; starting with 1 = January
 */
data class LocalDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int
) : Comparable<LocalDateTime> {


    override fun compareTo(other: LocalDateTime): Int =
        toMillis().compareTo(other.toMillis())


    companion object {
        val NONE = LocalDateTime(-1, -1, -1, -1, -1, -1)

        fun fromMillis(millis: Long) =
            fromCalendar(GregorianCalendar.getInstance().apply { timeInMillis = millis })

        fun fromCalendar(calendar: Calendar): LocalDateTime = calendar.let {
            LocalDateTime(
                it.get(Calendar.YEAR),
                it.get(Calendar.MONTH) + 1,
                it.get(Calendar.DAY_OF_MONTH),
                it.get(Calendar.HOUR_OF_DAY),
                it.get(Calendar.MINUTE),
                it.get(Calendar.SECOND)
            )
        }
    }
}

data class LocalTime(
    val hour: Int,
    val minute: Int,
    val second: Int
) : Comparable<LocalTime> {


    override fun compareTo(other: LocalTime): Int = toMillis().compareTo(other.toMillis())


    companion object {
        val MIN = LocalTime(0, 0, 0)
        val MAX = LocalTime(23, 59, 59)

        fun fromMillis(millis: Long): LocalTime {
            var value = millis
            val hour = value / TimeUnit.HOURS.toMillis(1)
            value -= hour * TimeUnit.HOURS.toMillis(1)
            val minute = value / TimeUnit.MINUTES.toMillis(1)
            value -= minute * TimeUnit.MINUTES.toMillis(1)
            val second = value / TimeUnit.SECONDS.toMillis(1)
            return LocalTime(hour.toInt(), minute.toInt(), second.toInt())
        }

        fun fromLocalDateTime(dateTime: LocalDateTime): LocalTime = GregorianCalendar.getInstance().apply {
            timeInMillis = dateTime.toMillis()
        }.let { calendar ->
            LocalTime(
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE),
                second = calendar.get(Calendar.SECOND),
            )
        }
    }
}

data class LocalDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<LocalDate> {
    override fun compareTo(other: LocalDate): Int = toMillis().compareTo(other.toMillis())


    companion object {
        val NONE = LocalDate(-1, -1, -1)

        fun fromCalendar(calendar: Calendar): LocalDate = calendar.let {
            LocalDate(
                it.get(Calendar.YEAR),
                it.get(Calendar.MONTH) + 1,
                it.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun fromMillis(millis: Long): LocalDate =
            fromCalendar(GregorianCalendar.getInstance().apply { timeInMillis = millis })
    }
}

enum class DayOfWeek(val id: Int) {
    NONE(-1),
    SUNDAY(GregorianCalendar.SUNDAY),
    MONDAY(GregorianCalendar.MONDAY),
    TUESDAY(GregorianCalendar.TUESDAY),
    WEDNESDAY(GregorianCalendar.WEDNESDAY),
    THURSDAY(GregorianCalendar.THURSDAY),
    FRIDAY(GregorianCalendar.FRIDAY),
    SATURDAY(GregorianCalendar.SATURDAY);


    companion object {
        fun fromMillis(timestamp: Long) = GregorianCalendar.getInstance().apply {
            timeInMillis = timestamp
        }.get(Calendar.DAY_OF_WEEK).let { dayOfWeek ->
            values().find { it.id == dayOfWeek } ?: NONE
        }
    }
}

data class LocalDateTimeRange(
    val timeFrom: LocalDateTime,
    val timeTo: LocalDateTime,
    val isInclusive: Boolean,
    val millis: Long = TimeUnit.SECONDS.toMillis(1)
) : Iterable<LocalDateTime> {
    override fun iterator(): Iterator<LocalDateTime> = object : Iterator<LocalDateTime> {
        private var hasNext: Boolean =
            millis > 0 && timeFrom <= timeTo

        private var next = if (hasNext) timeFrom else timeTo

        override fun hasNext(): Boolean = hasNext

        override fun next(): LocalDateTime {
            val value = next
            next += millis

            hasNext = if (isInclusive) {
                next <= timeTo
            } else {
                next < timeTo
            }
            return value
        }
    }
}

data class LocalDateRange(
    val timeFrom: LocalDate,
    val timeTo: LocalDate,
    val isInclusive: Boolean,
    val millis: Long = TimeUnit.DAYS.toMillis(1)
) : Iterable<LocalDate> {
    override fun iterator(): Iterator<LocalDate> = object : Iterator<LocalDate> {
        //private var hasNext: Boolean = millis > 0 && timeFrom >= timeTo
        private var hasNext: Boolean = millis > 0 && timeFrom <= timeTo

        private var next = if (hasNext) timeFrom else timeTo

        override fun hasNext(): Boolean = hasNext

        override fun next(): LocalDate {
            val value = next
            next += millis

            hasNext = if (millis < Duration.days(1).toMillis()) {
                false
            } else {
                if (isInclusive) {
                    next <= timeTo
                } else {
                    next < timeTo
                }
            }
            return value
        }
    }
}

data class LocalTimeRange(
    val timeFrom: LocalTime,
    val timeTo: LocalTime,
    val isInclusive: Boolean,
    val millis: Long = TimeUnit.SECONDS.toMillis(1)
) : Iterable<LocalTime> {
    override fun iterator(): Iterator<LocalTime> = object : Iterator<LocalTime> {
        private var hasNext: Boolean = millis > 0 && timeFrom <= timeTo

        private var next = if (hasNext) timeFrom else timeTo

        override fun hasNext(): Boolean = hasNext

        override fun next(): LocalTime {
            val value = next
            next += millis

            hasNext = if (isInclusive) {
                next <= timeTo
            } else {
                next < timeTo
            }
            return value
        }
    }
}

fun DayOfWeek.isWeekend() = this == DayOfWeek.SUNDAY || this == DayOfWeek.SATURDAY
fun DayOfWeek.isWeekday() = !isWeekend()

fun LocalDateTime.toDayOfWeek(): DayOfWeek = DayOfWeek.fromMillis(toMillis())
fun LocalDate.toDayOfWeek(): DayOfWeek = DayOfWeek.fromMillis(toMillis())

fun LocalDateTime.toMillis(): Long = GregorianCalendar.getInstance().apply {
    set(year, month - 1, day, hour, minute, second)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun LocalDate.toMillis(): Long = GregorianCalendar.getInstance().apply {
    set(year, month - 1, day, 0, 0, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun LocalTime.toMillis(): Long = TimeUnit.HOURS.toMillis(hour.toLong()) +
        TimeUnit.MINUTES.toMillis(minute.toLong()) +
        TimeUnit.SECONDS.toMillis(second.toLong())

fun Duration.toMillis(): Long = when (unit) {
    Duration.Unit.MILLISECOND -> amount
    Duration.Unit.SECOND -> TimeUnit.SECONDS.toMillis(amount)
    Duration.Unit.MINUTE -> TimeUnit.MINUTES.toMillis(amount)
    Duration.Unit.HOUR -> TimeUnit.HOURS.toMillis(amount)
    Duration.Unit.DAY -> TimeUnit.DAYS.toMillis(amount)
}

/**
 * startOfDate and endOfDate
 */
fun LocalDateTime.startOfDate(): LocalDateTime = LocalDateTime.fromCalendar(
    GregorianCalendar.getInstance().apply {
        set(year, month - 1, day, 0, 0, 0)
    }
)

fun LocalDateTime.endOfDate(): LocalDateTime = LocalDateTime.fromCalendar(
    GregorianCalendar.getInstance().apply {
        set(year, month - 1, day, 23, 59, 59)
    }
)

fun LocalDate.startOfDate(): LocalDateTime = LocalDateTime.fromCalendar(
    GregorianCalendar.getInstance().apply {
        set(year, month - 1, day, 0, 0, 0)
    }
)

fun LocalDate.endOfDate(): LocalDateTime = LocalDateTime.fromCalendar(
    GregorianCalendar.getInstance().apply {
        set(year, month - 1, day, 23, 59, 59)
    }
)

operator fun LocalDateTime.plus(millis: Long): LocalDateTime =
    LocalDateTime.fromMillis(toMillis() + millis)
operator fun LocalDateTime.plus(localTime: LocalTime): LocalDateTime = this + localTime.toMillis()
operator fun LocalDateTime.plus(localDate: LocalDate): LocalDateTime = this + localDate.toMillis()
operator fun LocalDateTime.plus(localDateTime: LocalDateTime): LocalDateTime =
    this + localDateTime.toMillis()
operator fun LocalDateTime.plus(duration: Duration): LocalDateTime = this + duration.toMillis()
operator fun LocalDateTime.minus(millis: Long): LocalDateTime = LocalDateTime.fromMillis(toMillis() - millis)
operator fun LocalDateTime.minus(localTime: LocalTime): LocalDateTime = this - localTime.toMillis()
operator fun LocalDateTime.minus(localDate: LocalDate): LocalDateTime = this - localDate.toMillis()
operator fun LocalDateTime.minus(localDateTime: LocalDateTime): LocalDateTime =
    this - localDateTime.toMillis()

operator fun LocalDateTime.rangeTo(other: LocalDateTime) = LocalDateTimeRange(this, other, true)
infix fun LocalDateTime.until(other: LocalDateTime) = LocalDateTimeRange(this, other, false)

operator fun LocalTime.rangeTo(other: LocalTime) = LocalTimeRange(this, other, true)
operator fun LocalTime.plus(millis: Long) = LocalTime.fromMillis(toMillis() + millis)
operator fun LocalTime.plus(other: LocalTime) = this + other.toMillis()
operator fun LocalTime.minus(millis: Long) = LocalTime.fromMillis(toMillis() - millis)
operator fun LocalTime.minus(other: LocalTime) = this - other.toMillis()
infix fun LocalTime.until(other: LocalTime) = LocalTimeRange(this, other, true)

operator fun LocalDate.plus(millis: Long): LocalDate = LocalDate.fromMillis(toMillis() + millis)
operator fun LocalDate.plus(duration: Duration): LocalDate = this + duration.toMillis()
operator fun LocalDate.plus(day: Int): LocalDate = this + Duration.days(day)
operator fun LocalDate.plus(localDate: LocalDate): LocalDate = this + localDate.toMillis()
operator fun LocalDate.plus(localTime: LocalTime): LocalDateTime =
    LocalDateTime.fromMillis(toMillis() + localTime.toMillis())
operator fun LocalDate.minus(millis: Long): LocalDate = LocalDate.fromMillis(toMillis() - millis)
operator fun LocalDate.minus(duration: Duration): LocalDate = this - duration.toMillis()
operator fun LocalDate.minus(day: Int): LocalDate = this - Duration.days(day)
operator fun LocalDate.minus(localDate: LocalDate): LocalDate = this - localDate.toMillis()
operator fun LocalDate.minus(localTime: LocalTime): LocalDateTime =
    LocalDateTime.fromMillis(toMillis() - localTime.toMillis())

operator fun LocalDate.rangeTo(other: LocalDate) = LocalDateRange(this, other, true)
infix fun LocalDate.until(other: LocalDate) = LocalDateRange(this, other, false)

operator fun Duration.plus(other: Duration): Duration =
    Duration(toMillis() + other.toMillis(), Duration.Unit.MILLISECOND)
operator fun Duration.minus(other: Duration): Duration =
    Duration(toMillis() - other.toMillis(), Duration.Unit.MILLISECOND)

operator fun Duration.times(t: Int): Duration = Duration(toMillis() * t, Duration.Unit.MILLISECOND)
operator fun Duration.times(t: Long): Duration = Duration(toMillis() * t, Duration.Unit.MILLISECOND)

infix fun LocalDateTimeRange.step(millis: Long) = copy(millis = millis)
infix fun LocalDateTimeRange.step(duration: Duration) = copy(millis = duration.toMillis())
operator fun LocalDateTimeRange.contains(value: LocalDateTime): Boolean =
    if (isInclusive) {
        this.timeFrom <= value && value <= this.timeTo
    } else {
        timeFrom < value && value < timeTo
    }

operator fun LocalDateTimeRange.contains(value: LocalDate): Boolean =
    if (isInclusive) {
        this.timeFrom <= value.endOfDate() && value.startOfDate() <= this.timeTo
    } else {
        timeFrom < value.endOfDate() && value.startOfDate() < timeTo
    }

infix fun LocalTimeRange.step(millis: Long) = copy(millis = millis)
infix fun LocalTimeRange.step(duration: Duration) = copy(millis = duration.toMillis())
operator fun LocalTimeRange.contains(value: LocalTime): Boolean =
    if (isInclusive) {
        this.timeFrom <= value && value <= this.timeTo
    } else {
        timeFrom < value && value < timeTo
    }


infix fun LocalDateRange.step(millis: Long) = copy(millis = millis)
infix fun LocalDateRange.step(day: Int) = copy(millis = TimeUnit.DAYS.toMillis(day.toLong()))
operator fun LocalDateRange.contains(value: LocalDate): Boolean =
    if (isInclusive) {
        this.timeFrom <= value && value <= this.timeTo
    } else {
        timeFrom < value && value < timeTo
    }

operator fun LocalDateRange.contains(value: LocalDateTime): Boolean =
    if (isInclusive) {
        timeFrom.startOfDate() <= value && value <= timeTo.endOfDate()
    } else {
        timeFrom.startOfDate() < value && value < timeTo.startOfDate()
    }
