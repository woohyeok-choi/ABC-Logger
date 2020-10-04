package kaist.iclab.abclogger.survey

import androidx.databinding.BaseObservable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.*
import kotlin.Exception

data class Survey(
    val title: AltText = AltText(),
    val message: AltText = AltText(),
    val instruction: AltText = AltText(),
    val question: Array<Question> = arrayOf(),
    val timeout: Duration = Duration(0, Duration.Unit.MILLISECOND),
    val timeoutAction: TimeoutAction = TimeoutAction.NONE,
    val timeFrom: Duration = Duration.NONE,
    val timeTo: Duration = Duration.NONE,
    val schedule: Schedule = Schedule()
) {
    enum class TimeoutAction {
        NONE,
        ALT_TEXT,
        DISABLED;
    }

    fun toJson(): String = try {
        Adapter.toJson(this) ?: ""
    } catch (e: Exception) {
        ""
    }

    fun isEmpty() = this == Empty

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Survey

        if (title != other.title) return false
        if (message != other.message) return false
        if (instruction != other.instruction) return false
        if (!question.contentEquals(other.question)) return false
        if (timeout != other.timeout) return false
        if (timeFrom != other.timeFrom) return false
        if (timeTo != other.timeTo) return false
        if (schedule != other.schedule) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + instruction.hashCode()
        result = 31 * result + question.contentHashCode()
        result = 31 * result + timeout.hashCode()
        result = 31 * result + timeFrom.hashCode()
        result = 31 * result + timeTo.hashCode()
        result = 31 * result + schedule.hashCode()
        return result
    }

    companion object {
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
            null
        }

        private val Empty = Survey()
    }
}

data class SurveyConfiguration(
    val uuid: String = UUID.randomUUID().toString(),
    var downloadUrl: String = ""
) : BaseObservable() {
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

    @Transient
    var json: String = ""

    var lastAccessTime: Long = Long.MIN_VALUE
        set(value) {
            if (field == value) return
            field = value
            notifyChange()
        }

    var survey: Survey = Survey()
        set(value) {
            if (field == value) return
            field = value
            notifyChange()
        }

    fun getTitle() = survey.title

    fun getMessage() = survey.message
}