package kaist.iclab.abclogger.structure.survey

import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

data class Schedule(
    val interDaySchedule: InterDaySchedule = DailySchedule,
    val intraDaySchedule: IntraDaySchedule = NoneIntraDaySchedule
)

sealed class InterDaySchedule(val type: Type) {
    enum class Type {
        DATE,
        DAY_OF_WEEK,
        DAILY
    }

    companion object {
        val Factory: PolymorphicJsonAdapterFactory<InterDaySchedule> = PolymorphicJsonAdapterFactory.of(InterDaySchedule::class.java, "type")
            .withSubtype(DateSchedule::class.java, Type.DATE.name)
            .withSubtype(DayOfWeekSchedule::class.java, Type.DAY_OF_WEEK.name)
            .withDefaultValue(DailySchedule)
    }
}

data class DateSchedule(
    val dates: List<LocalDate> = listOf()
) : InterDaySchedule(Type.DATE)

data class DayOfWeekSchedule(
    val daysOfWeek: List<DayOfWeek> = listOf()
) : InterDaySchedule(Type.DAY_OF_WEEK)

object DailySchedule: InterDaySchedule(Type.DAILY)

sealed class IntraDaySchedule(val type: Type) {
    enum class Type {
        NONE,
        INTERVAL,
        EVENT,
        TIME
    }

    companion object {
        val Factory: PolymorphicJsonAdapterFactory<IntraDaySchedule> = PolymorphicJsonAdapterFactory.of(IntraDaySchedule::class.java, "type")
            .withSubtype(TimeSchedule::class.java, Type.TIME.name)
            .withSubtype(IntervalSchedule::class.java, Type.INTERVAL.name)
            .withSubtype(EventSchedule::class.java, Type.EVENT.name)
            .withDefaultValue(NoneIntraDaySchedule)
    }
}

object NoneIntraDaySchedule: IntraDaySchedule(Type.NONE)

data class TimeSchedule(
    val times: List<LocalTime> = listOf()
): IntraDaySchedule(Type.TIME)

data class IntervalSchedule(
    val timeFrom: LocalTime = LocalTime.MIN,
    val timeTo: LocalTime = LocalTime.MAX,
    val intervalDefault: Duration,
    val intervalFlex: Duration
) : IntraDaySchedule(Type.INTERVAL)

data class EventSchedule(
    val timeFrom: LocalTime,
    val timeTo: LocalTime,
    val delayDefault: Duration,
    val delayFlex: Duration,
    val eventsTrigger: List<String> = listOf(),
    val eventsCancel: List<String> = listOf()
) : IntraDaySchedule(Type.EVENT)



