package kaist.iclab.abclogger.structure.survey


import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

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
            .withDefaultValue(NoneIntraDaySchedule())
    }
}

class NoneIntraDaySchedule: IntraDaySchedule(Type.NONE)

data class TimeSchedule(
    val times: List<LocalTime> = listOf()
): IntraDaySchedule(Type.TIME)

data class IntervalSchedule(
    val timeFrom: LocalTime = LocalTime.MIN,
    val timeTo: LocalTime = LocalTime.MAX,
    val intervalDefault: Duration = Duration.hours(1),
    val intervalFlex: Duration = Duration.MIN
) : IntraDaySchedule(Type.INTERVAL)

data class EventSchedule(
    val timeFrom: LocalTime = LocalTime.MIN,
    val timeTo: LocalTime = LocalTime.MAX,
    val delayDefault: Duration = Duration.MIN,
    val delayFlex: Duration = Duration.MIN,
    val eventsTrigger: List<String> = listOf(),
    val eventsCancel: List<String> = listOf()
) : IntraDaySchedule(Type.EVENT)


