package kaist.iclab.abclogger.collector.survey

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

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

        fun from(dayOfWeekInt: Int) = valueMap[dayOfWeekInt]
    }
}

data class Schedule(val dayOfWeek: DayOfWeek, val hour: Int, val minute: Int)

open class Survey(
        val type: String,
        open val title: String = "",
        open val message: String = "",
        open val instruction: String = "",
        open val timeoutSec: Long = -1,
        open val timeoutPolicy: String = "",
        open var questions: Array<Question> = arrayOf()
) {
    data class Question(
            val type: String,
            val shouldAnswer: Boolean = true,
            val showEtc: Boolean = false,
            val text: String,
            val altText: String = "",
            val options: Array<String> = arrayOf(),
            var responses: Array<String> = arrayOf()
    ) {
        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + shouldAnswer.hashCode()
            result = 31 * result + text.hashCode()
            result = 31 * result + (options.contentHashCode())
            result = 31 * result + (responses.contentHashCode())
            result = 31 * result + (altText.hashCode())
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Question

            if (type != other.type) return false
            if (shouldAnswer != other.shouldAnswer) return false
            if (showEtc != other.showEtc) return false
            if (text != other.text) return false
            if (altText != other.altText) return false
            if (!options.contentEquals(other.options)) return false
            if (!responses.contentEquals(other.responses)) return false

            return true
        }

        fun isCorrectlyAnswered(): Boolean = !shouldAnswer || responses.any { !it.isBlank() }
    }

    companion object {
        const val TIMEOUT_ALT_TEXT = "ALT_TEXT"
        const val TIMEOUT_DISABLED = "DISABLED"
        const val TIMEOUT_NONE = "NONE"

        const val QUESTION_FREE_TEXT = "FREE_TEXT"
        const val QUESTION_HORIZONTAL_RADIO_BUTTON = "HORIZONTAL_RADIO_BUTTON"
        const val QUESTION_RADIO_BUTTON = "RADIO_BUTTON"
        const val QUESTION_CHECK_BOX = "CHECK_BOX"
        const val QUESTION_SLIDER = "SLIDER"

        const val TYPE_INTERVAL = "INTERVAL"
        const val TYPE_EVENT = "EVENT"
        const val TYPE_SCHEDULE = "SCHEDULE"

        private val moshi by lazy {
            Moshi.Builder().add(
                    PolymorphicJsonAdapterFactory.of(Survey::class.java, "type")
                            .withSubtype(IntervalBasedSurvey::class.java, TYPE_INTERVAL)
                            .withSubtype(EventBasedSurvey::class.java, TYPE_EVENT)
                            .withSubtype(ScheduleBasedSurvey::class.java, TYPE_SCHEDULE)
            ).add(KotlinJsonAdapterFactory()).build()
        }

        suspend fun fromJson(jsonString: String) = withContext(Dispatchers.IO) { moshi.adapter(Survey::class.java).fromJson(jsonString) }
    }

    suspend fun toJson(): String {
        val survey = this
        return withContext(Dispatchers.IO) { moshi.adapter(Survey::class.java).toJson(survey) }
    }
}

data class IntervalBasedSurvey(
        override val title: String,
        override val message: String,
        override val instruction: String,
        override val timeoutSec: Long,
        override val timeoutPolicy: String,
        override var questions: Array<Question>,
        val initialDelaySec: Long = -1,
        val intervalSec: Long = -1,
        val flexIntervalSec: Long = -1,
        val dailyStartTimeHour: Int = -1,
        val dailyStartTimeMinute: Int = -1,
        val dailyEndTimeHour: Int = -1,
        val dailyEndTimeMinute: Int = -1,
        val daysOfWeek: Array<DayOfWeek> = DayOfWeek.values()
) : Survey(TYPE_INTERVAL, title, message, instruction, timeoutSec, timeoutPolicy, questions) {

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
        if (!daysOfWeek.contentEquals(other.daysOfWeek)) return false

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
        result = 31 * result + daysOfWeek.contentHashCode()
        return result
    }
}

data class EventBasedSurvey(
        override val title: String,
        override val message: String,
        override val instruction: String,
        override val timeoutSec: Long,
        override val timeoutPolicy: String,
        override var questions: Array<Question>,
        val triggerEvents: Set<String> = setOf(),
        val cancelEvents: Set<String> = setOf(),
        val delayAfterTriggerEventSec: Long = -1,
        val flexDelayAfterTriggerEventSec: Long = -1,
        val dailyStartTimeHour: Int = -1,
        val dailyStartTimeMinute: Int = -1,
        val dailyEndTimeHour: Int = -1,
        val dailyEndTimeMinute: Int = -1,
        val daysOfWeek: Array<DayOfWeek> = DayOfWeek.values()
) : Survey(TYPE_EVENT, title, message, instruction, timeoutSec, timeoutPolicy, questions) {

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
        if (!daysOfWeek.contentEquals(other.daysOfWeek)) return false

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
        result = 31 * result + daysOfWeek.contentHashCode()
        return result
    }
}


data class ScheduleBasedSurvey(
        override val title: String,
        override val message: String,
        override val instruction: String,
        override val timeoutSec: Long,
        override val timeoutPolicy: String,
        override var questions: Array<Question>,
        val schedules: List<Schedule> = listOf()
) : Survey(TYPE_SCHEDULE, title, message, instruction, timeoutSec, timeoutPolicy, questions) {

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
