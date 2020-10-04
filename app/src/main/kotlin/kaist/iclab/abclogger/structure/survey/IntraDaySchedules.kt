package kaist.iclab.abclogger.structure.survey

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

sealed class IntraDaySchedule(val type: Type): Parcelable {
    enum class Type {
        NONE,
        INTERVAL,
        EVENT,
        TIME
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type.name)
    }

    override fun describeContents(): Int = 0

    companion object {
        val Factory: PolymorphicJsonAdapterFactory<IntraDaySchedule> = PolymorphicJsonAdapterFactory.of(IntraDaySchedule::class.java, "type")
            .withSubtype(TimeSchedule::class.java, Type.TIME.name)
            .withSubtype(IntervalSchedule::class.java, Type.INTERVAL.name)
            .withSubtype(EventSchedule::class.java, Type.EVENT.name)
            .withDefaultValue(NoneIntraDaySchedule())
    }
}

class NoneIntraDaySchedule: IntraDaySchedule(Type.NONE) {
    companion object CREATOR : Parcelable.Creator<NoneIntraDaySchedule> {
        override fun createFromParcel(parcel: Parcel): NoneIntraDaySchedule {
            return NoneIntraDaySchedule()
        }

        override fun newArray(size: Int): Array<NoneIntraDaySchedule?> {
            return arrayOfNulls(size)
        }
    }
}

data class TimeSchedule(
    val times: List<LocalTime> = listOf()
): IntraDaySchedule(Type.TIME) {
    constructor(parcel: Parcel) : this(parcel.createTypedArrayList(LocalTime) ?: listOf())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeTypedList(times)
    }

    companion object CREATOR : Parcelable.Creator<TimeSchedule> {
        override fun createFromParcel(parcel: Parcel): TimeSchedule {
            return TimeSchedule(parcel)
        }

        override fun newArray(size: Int): Array<TimeSchedule?> {
            return arrayOfNulls(size)
        }
    }
}

data class IntervalSchedule(
    val timeFrom: LocalTime = LocalTime.MIN,
    val timeTo: LocalTime = LocalTime.MAX,
    val intervalDefault: Duration = Duration.hours(1),
    val intervalFlex: Duration = Duration.MIN
) : IntraDaySchedule(Type.INTERVAL) {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(LocalTime::class.java.classLoader) ?: LocalTime.MIN,
        parcel.readParcelable(LocalTime::class.java.classLoader) ?: LocalTime.MAX,
        parcel.readParcelable(Duration::class.java.classLoader) ?: Duration.hours(1),
        parcel.readParcelable(Duration::class.java.classLoader) ?: Duration.MIN
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeParcelable(timeFrom, flags)
        parcel.writeParcelable(timeTo, flags)
        parcel.writeParcelable(intervalDefault, flags)
        parcel.writeParcelable(intervalFlex, flags)
    }

    companion object CREATOR : Parcelable.Creator<IntervalSchedule> {
        override fun createFromParcel(parcel: Parcel): IntervalSchedule {
            return IntervalSchedule(parcel)
        }

        override fun newArray(size: Int): Array<IntervalSchedule?> {
            return arrayOfNulls(size)
        }
    }
}

data class EventSchedule(
    val timeFrom: LocalTime = LocalTime.MIN,
    val timeTo: LocalTime = LocalTime.MAX,
    val delayDefault: Duration = Duration.MIN,
    val delayFlex: Duration = Duration.MIN,
    val eventsTrigger: List<String> = listOf(),
    val eventsCancel: List<String> = listOf()
) : IntraDaySchedule(Type.EVENT) {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(LocalTime::class.java.classLoader) ?: LocalTime.MIN,
        parcel.readParcelable(LocalTime::class.java.classLoader) ?: LocalTime.MAX,
        parcel.readParcelable(Duration::class.java.classLoader) ?: Duration.MIN,
        parcel.readParcelable(Duration::class.java.classLoader) ?: Duration.MIN,
        parcel.createStringArrayList() ?: listOf(),
        parcel.createStringArrayList() ?: listOf()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeParcelable(timeFrom, flags)
        parcel.writeParcelable(timeTo, flags)
        parcel.writeParcelable(delayDefault, flags)
        parcel.writeParcelable(delayFlex, flags)
        parcel.writeStringList(eventsTrigger)
        parcel.writeStringList(eventsCancel)
    }

    companion object CREATOR : Parcelable.Creator<EventSchedule> {
        override fun createFromParcel(parcel: Parcel): EventSchedule {
            return EventSchedule(parcel)
        }

        override fun newArray(size: Int): Array<EventSchedule?> {
            return arrayOfNulls(size)
        }
    }
}



