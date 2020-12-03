package kaist.iclab.abclogger.structure.survey

import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.BaseObservable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.*
import kotlin.Exception

data class Survey(
    val title: AltText = AltText(),
    val message: AltText = AltText(),
    val instruction: AltText = AltText(),
    val question: Array<Question> = arrayOf(),
    val timeout: Duration = Duration.MIN,
    val timeoutAction: TimeoutAction = TimeoutAction.NONE,
    val timeFrom: Duration = Duration.NONE,
    val timeTo: Duration = Duration.NONE,
    val interDaySchedule: InterDaySchedule = DailySchedule(),
    val intraDaySchedule: IntraDaySchedule = NoneIntraDaySchedule()
) {
    enum class TimeoutAction {
        NONE,
        ALT_TEXT,
        DISABLED;
    }
    fun toJson(prettify: Boolean = false): String = try {
        if (prettify) {
            Adapter.indent("\t").toJson(this) ?: ""
        } else {
            Adapter.toJson(this) ?: ""
        }
    } catch (e: Exception) {
        ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Survey

        if (title != other.title) return false
        if (message != other.message) return false
        if (instruction != other.instruction) return false
        if (!question.contentEquals(other.question)) return false
        if (timeout != other.timeout) return false
        if (timeoutAction != other.timeoutAction) return false
        if (timeFrom != other.timeFrom) return false
        if (timeTo != other.timeTo) return false
        if (interDaySchedule != other.interDaySchedule) return false
        if (intraDaySchedule != other.intraDaySchedule) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + instruction.hashCode()
        result = 31 * result + question.contentHashCode()
        result = 31 * result + timeout.hashCode()
        result = 31 * result + timeoutAction.hashCode()
        result = 31 * result + timeFrom.hashCode()
        result = 31 * result + timeTo.hashCode()
        result = 31 * result + interDaySchedule.hashCode()
        result = 31 * result + intraDaySchedule.hashCode()
        return result
    }

    companion object {
        val Empty = Survey()

        val Adapter: JsonAdapter<Survey> = Moshi.Builder()
            .add(Question.Factory)
            .add(IntraDaySchedule.Factory)
            .add(InterDaySchedule.Factory)
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter(Survey::class.java)

        fun fromJson(json: String): Survey? = try {
            Adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class SurveyConfiguration(
    val uuid: String = UUID.randomUUID().toString(),
    val url: String = "",
    val lastAccessTime: Long = Long.MIN_VALUE,
    val survey: Survey = Survey.Empty
) : BaseObservable(), Parcelable {
    @Transient
    var isLoading: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyChange()
        }

    @Transient
    var error: String? = null
        set(value) {
            if (field == value) return
            field = value
            notifyChange()
        }


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uuid)
        parcel.writeString(url)
        parcel.writeLong(lastAccessTime)
        parcel.writeString(survey.toJson())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SurveyConfiguration> {
        override fun createFromParcel(parcel: Parcel): SurveyConfiguration = SurveyConfiguration(
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readLong(),
            Survey.fromJson(parcel.readString() ?: "") ?: Survey.Empty
        )

        override fun newArray(size: Int): Array<SurveyConfiguration?> {
            return arrayOfNulls(size)
        }

        private val moshi = Moshi.Builder()
            .add(Survey::class.java, Survey.Adapter)
            .add(KotlinJsonAdapterFactory())
            .build()

        val Adapter: JsonAdapter<SurveyConfiguration> =
            moshi.adapter(SurveyConfiguration::class.java)

        val ListAdapter: JsonAdapter<List<SurveyConfiguration>> = moshi.adapter(
            Types.newParameterizedType(
                List::class.java,
                SurveyConfiguration::class.java
            )
        )
    }
}