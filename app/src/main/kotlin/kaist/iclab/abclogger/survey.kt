package kaist.iclab.abclogger

import android.text.TextUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

import java.lang.IllegalArgumentException
import java.util.*
import kotlin.reflect.KClass

enum class DayOfWeek(val id: Int) {
    NONE(-1),
    SUNDAY(GregorianCalendar.SUNDAY),
    MONDAY(GregorianCalendar.MONDAY),
    TUESDAY(GregorianCalendar.TUESDAY),
    WEDNESDAY(GregorianCalendar.WEDNESDAY),
    THURSDAY(GregorianCalendar.THURSDAY),
    FRIDAY(GregorianCalendar.FRIDAY),
    SATURDAY(GregorianCalendar.SATURDAY);

    fun isWeekend(): Boolean {
        return this == SUNDAY || this == SATURDAY
    }

    fun isWeekDay(): Boolean {
        return !isWeekend()
    }

    fun next(): DayOfWeek {
        return valueMap[id % 7 + 1] ?: NONE
    }

    companion object {
        private val valueMap = enumValues<DayOfWeek>().associateBy { it.id }

        fun now(): DayOfWeek {
            val dayOfWeek = GregorianCalendar.getInstance(TimeZone.getDefault()).get(GregorianCalendar.DAY_OF_WEEK)
            return valueMap[dayOfWeek] ?: NONE
        }
    }
}

data class Schedule(val dayOfWeek: DayOfWeek, val hour: Int, val minute: Int)

data class SurveyQuestion(
        val type: String,
        val shouldAnswers: Boolean = true,
        val showEtc: Boolean = false,
        val text: String,
        val options: Array<Any>? = arrayOf(),
        val altText: String? = "",
        var response: Array<String> = arrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SurveyQuestion

        if (type != other.type) return false
        if (shouldAnswers != other.shouldAnswers) return false
        if (text != other.text) return false
        if (options != null) {
            if (other.options == null) return false
            if (!options.contentEquals(other.options)) return false
        } else if (other.options != null) return false
        if (altText != other.altText) return false
        if (!response.contentEquals(other.response)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + shouldAnswers.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + (options?.contentHashCode() ?: 0)
        result = 31 * result + (altText?.hashCode() ?: 0)
        result = 31 * result + response.contentHashCode()
        return result
    }
}

open class Survey(
        val type: String,
        open val title: String = "",
        open val message: String = "",
        open val instruction: String = "",
        open val timeoutSec: Long = -1,
        open val timeoutPolicy: String = "",
        open val questions: Array<SurveyQuestion> = arrayOf()
) {

    companion object {
        const val TIMEOUT_ALT_TEXT = "TIMEOUT_ALT_TEXT"
        const val TIMEOUT_DISABLED = "TIMEOUT_DISABLED"
        const val TIMEOUT_NONE = "TIMEOUT_NONE"

        const val QUESTION_FREE_TEXT = "QUESTION_FREE_TEXT"
        const val QUESTION_RADIO_BUTTON = "QUESTION_RADIO_BUTTON"
        const val QUESTION_CHECK_BOX = "QUESTION_CHECK_BOX"
        const val QUESTION_RADIO_BUTTON_HORIZONTAL = "QUESTION_RADIO_BUTTON_HORIZONTAL"
        const val QUESTION_SLIDER = "QUESTION_SLIDER"

        private val moshi by lazy {
            Moshi.Builder().add(
                PolymorphicJsonAdapterFactory.of(Survey::class.java, "type")
                        .withSubtype(IntervalBasedSurvey::class.java, "interval")
                        .withSubtype(EventBasedSurvey::class.java, "event")
                        .withSubtype(ScheduleBasedSurvey::class.java, "schedule")
            ).add(KotlinJsonAdapterFactory()).build()
        }

        fun <T : Survey> fromJson(jsonString: String) = moshi.adapter(Survey::class.java).fromJson(jsonString)
    }

    fun toJson() = moshi.adapter(Survey::class.java).toJson(this)
}

data class IntervalBasedSurvey(
        override val title: String,
        override val message: String,
        override val instruction: String,
        override val timeoutSec: Long,
        override val timeoutPolicy: String,
        override val questions: Array<SurveyQuestion>,
        val initialDelaySec: Long = -1,
        val intervalSec: Long = -1,
        val flexIntervalSec: Long = -1,
        val dailyStartTimeHour: Int = -1,
        val dailyStartTimeMinute: Int = -1,
        val dailyEndTimeHour: Int = -1,
        val dailyEndTimeMinute: Int = -1,
        val daysOfWeek: List<DayOfWeek> = DayOfWeek.values().toList()
) : Survey("interval", title, message, instruction, timeoutSec, timeoutPolicy, questions) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntervalBasedSurvey

        if (title != other.title) return false
        if (message != other.message) return false
        if (instruction != other.instruction) return false
        if (timeoutSec != other.timeoutSec) return false
        if (timeoutPolicy != other.timeoutPolicy) return false
        if (!questions.contentEquals(other.questions)) return false
        if (initialDelaySec != other.initialDelaySec) return false
        if (intervalSec != other.intervalSec) return false
        if (flexIntervalSec != other.flexIntervalSec) return false
        if (dailyStartTimeHour != other.dailyStartTimeHour) return false
        if (dailyStartTimeMinute != other.dailyStartTimeMinute) return false
        if (dailyEndTimeHour != other.dailyEndTimeHour) return false
        if (dailyEndTimeMinute != other.dailyEndTimeMinute) return false
        if (daysOfWeek != other.daysOfWeek) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + instruction.hashCode()
        result = 31 * result + timeoutSec.hashCode()
        result = 31 * result + timeoutPolicy.hashCode()
        result = 31 * result + questions.contentHashCode()
        result = 31 * result + initialDelaySec.hashCode()
        result = 31 * result + intervalSec.hashCode()
        result = 31 * result + flexIntervalSec.hashCode()
        result = 31 * result + dailyStartTimeHour
        result = 31 * result + dailyStartTimeMinute
        result = 31 * result + dailyEndTimeHour
        result = 31 * result + dailyEndTimeMinute
        result = 31 * result + daysOfWeek.hashCode()
        return result
    }
}

data class EventBasedSurvey(
        override val title: String,
        override val message: String,
        override val instruction: String,
        override val timeoutSec: Long,
        override val timeoutPolicy: String,
        override val questions: Array<SurveyQuestion>,
        val triggerEvents: Set<String> = setOf(),
        val cancelEvents: Set<String> = setOf(),
        val delayAfterTriggerEventSec: Long = -1,
        val flexDelayAfterTriggerEventSec: Long = -1,
        val dailyStartTimeHour: Int = -1,
        val dailyStartTimeMinute: Int = -1,
        val dailyEndTimeHour: Int = -1,
        val dailyEndTimeMinute: Int = -1,
        val daysOfWeek: List<DayOfWeek> = DayOfWeek.values().toList()
) : Survey("event", title, message, instruction, timeoutSec, timeoutPolicy, questions) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventBasedSurvey

        if (title != other.title) return false
        if (message != other.message) return false
        if (instruction != other.instruction) return false
        if (timeoutSec != other.timeoutSec) return false
        if (timeoutPolicy != other.timeoutPolicy) return false
        if (!questions.contentEquals(other.questions)) return false
        if (triggerEvents != other.triggerEvents) return false
        if (cancelEvents != other.cancelEvents) return false
        if (delayAfterTriggerEventSec != other.delayAfterTriggerEventSec) return false
        if (flexDelayAfterTriggerEventSec != other.flexDelayAfterTriggerEventSec) return false
        if (dailyStartTimeHour != other.dailyStartTimeHour) return false
        if (dailyStartTimeMinute != other.dailyStartTimeMinute) return false
        if (dailyEndTimeHour != other.dailyEndTimeHour) return false
        if (dailyEndTimeMinute != other.dailyEndTimeMinute) return false
        if (daysOfWeek != other.daysOfWeek) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + instruction.hashCode()
        result = 31 * result + timeoutSec.hashCode()
        result = 31 * result + timeoutPolicy.hashCode()
        result = 31 * result + questions.contentHashCode()
        result = 31 * result + triggerEvents.hashCode()
        result = 31 * result + cancelEvents.hashCode()
        result = 31 * result + delayAfterTriggerEventSec.hashCode()
        result = 31 * result + flexDelayAfterTriggerEventSec.hashCode()
        result = 31 * result + dailyStartTimeHour
        result = 31 * result + dailyStartTimeMinute
        result = 31 * result + dailyEndTimeHour
        result = 31 * result + dailyEndTimeMinute
        result = 31 * result + daysOfWeek.hashCode()
        return result
    }
}


data class ScheduleBasedSurvey(
        override val title: String,
        override val message: String,
        override val instruction: String,
        override val timeoutSec: Long,
        override val timeoutPolicy: String,
        override val questions: Array<SurveyQuestion>,
        val schedules: List<Schedule> = listOf()
) : Survey("schedule", title, message, instruction, timeoutSec, timeoutPolicy, questions) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScheduleBasedSurvey

        if (title != other.title) return false
        if (message != other.message) return false
        if (instruction != other.instruction) return false
        if (timeoutSec != other.timeoutSec) return false
        if (timeoutPolicy != other.timeoutPolicy) return false
        if (!questions.contentEquals(other.questions)) return false
        if (schedules != other.schedules) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + instruction.hashCode()
        result = 31 * result + timeoutSec.hashCode()
        result = 31 * result + timeoutPolicy.hashCode()
        result = 31 * result + questions.contentHashCode()
        result = 31 * result + schedules.hashCode()
        return result
    }
}
