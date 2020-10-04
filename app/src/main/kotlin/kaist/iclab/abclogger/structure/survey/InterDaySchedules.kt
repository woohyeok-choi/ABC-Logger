package kaist.iclab.abclogger.structure.survey

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

sealed class InterDaySchedule(val type: Type) : Parcelable {
    enum class Type {
        DATE,
        DAY_OF_WEEK,
        DAILY
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type.name)
    }

    override fun describeContents(): Int = 0

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
) : InterDaySchedule(Type.DATE) {
    constructor(parcel: Parcel) : this(parcel.createTypedArrayList(LocalDate) ?: listOf())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeTypedList(dates)
    }

    companion object CREATOR : Parcelable.Creator<DateSchedule> {
        override fun createFromParcel(parcel: Parcel): DateSchedule {
            return DateSchedule(parcel)
        }

        override fun newArray(size: Int): Array<DateSchedule?> {
            return arrayOfNulls(size)
        }
    }
}

data class DayOfWeekSchedule(
    val daysOfWeek: List<DayOfWeek> = listOf()
) : InterDaySchedule(Type.DAY_OF_WEEK) {
    constructor(parcel: Parcel) : this(parcel.createTypedArrayList(DayOfWeek) ?: listOf())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeTypedList(daysOfWeek)
    }

    companion object CREATOR : Parcelable.Creator<DayOfWeekSchedule> {
        override fun createFromParcel(parcel: Parcel): DayOfWeekSchedule {
            return DayOfWeekSchedule(parcel)
        }

        override fun newArray(size: Int): Array<DayOfWeekSchedule?> {
            return arrayOfNulls(size)
        }
    }
}

class DailySchedule : InterDaySchedule(Type.DAILY) {
    companion object CREATOR : Parcelable.Creator<DailySchedule> {
        override fun createFromParcel(parcel: Parcel): DailySchedule {
            return DailySchedule()
        }

        override fun newArray(size: Int): Array<DailySchedule?> {
            return arrayOfNulls(size)
        }
    }
}
