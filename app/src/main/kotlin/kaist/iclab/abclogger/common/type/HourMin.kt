package kaist.iclab.abclogger.common.type

import android.os.Parcel
import android.os.Parcelable
import java.util.*
import kotlin.math.min

class HourMin(val hour : Int , val minute : Int) : Comparable<HourMin>, Parcelable {
    constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readInt())

    override fun compareTo(other: HourMin): Int {
        var cmp = Integer.compare(hour, other.hour)
        if (cmp == 0) {
            cmp = Integer.compare(minute, other.minute)
        }
        return cmp
    }

    fun toMillis(anchorTime: Long) : Long {
        val anchor = HourMin.fromMillis(anchorTime)
        var modifiedTime = anchorTime

        if(anchor > this) {
            modifiedTime += 1000 * 60 * 60 * 24
        }

        return GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = modifiedTime
            set(GregorianCalendar.HOUR_OF_DAY, hour)
            set(GregorianCalendar.MINUTE, minute)
            set(GregorianCalendar.SECOND, 0)
            set(GregorianCalendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun toMillis() : Long {
        return 1000 * 60 * (hour * 60 + minute).toLong()
    }

    override fun toString(): String {
        return String.format("%02d:%02d", hour, minute)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(hour)
        parcel.writeInt(minute)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<HourMin> {
        override fun createFromParcel(parcel: Parcel): HourMin {
            return HourMin(parcel)
        }

        override fun newArray(size: Int): Array<HourMin?> {
            return arrayOfNulls(size)
        }

        fun now() : HourMin {
            return GregorianCalendar.getInstance(TimeZone.getDefault()).let {
                HourMin(
                    hour = it.get(Calendar.HOUR_OF_DAY),
                    minute = it.get(Calendar.MINUTE)
                )
            }
        }

        fun fromMillis(millis: Long) : HourMin {
            return GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
                timeInMillis = millis
            }.let {
                HourMin(
                    hour = it.get(Calendar.HOUR_OF_DAY),
                    minute = it.get(Calendar.MINUTE)
                )
            }
        }

        fun fromString(str: String) : HourMin {
            return HourMin(
                str.split(":")[0].toInt(),
                str.split(":")[1].toInt()
            )
        }
    }
}