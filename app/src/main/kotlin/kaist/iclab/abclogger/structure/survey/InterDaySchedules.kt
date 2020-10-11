package kaist.iclab.abclogger.structure.survey

import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

sealed class InterDaySchedule(val type: Type){
    enum class Type {
        DATE,
        DAY_OF_WEEK,
        DAILY
    }

    companion object {
        val Factory: PolymorphicJsonAdapterFactory<InterDaySchedule> = PolymorphicJsonAdapterFactory.of(InterDaySchedule::class.java, "type")
            .withSubtype(DateSchedule::class.java, Type.DATE.name)
            .withSubtype(DayOfWeekSchedule::class.java, Type.DAY_OF_WEEK.name)
            .withSubtype(DailySchedule::class.java, Type.DAILY.name)
            .withDefaultValue(DailySchedule())
    }
}

data class DateSchedule(
    val dates: List<LocalDate> = listOf()
) : InterDaySchedule(Type.DATE)

data class DayOfWeekSchedule(
    val daysOfWeek: List<DayOfWeek> = listOf()
) : InterDaySchedule(Type.DAY_OF_WEEK)

class DailySchedule(
    val exceptDates: List<LocalDate> = listOf()
) : InterDaySchedule(Type.DAILY)