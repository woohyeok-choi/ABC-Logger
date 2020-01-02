package kaist.iclab.abclogger.survey

import android.text.TextUtils
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import kaist.iclab.abclogger.EmptySurveyException
import kaist.iclab.abclogger.InvalidSurveyFormatException
import kaist.iclab.abclogger.common.type.DayOfWeek
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.reflect.KClass

data class Schedule(val dayOfWeek: DayOfWeek, val hour: Int, val minute: Int)

data class SurveyQuestion(
        val type: String,
        val shouldAnswers: Boolean = true,
        val text: String,
        val options: ArrayList<Any>? = arrayListOf(),
        val altText: String? = "",
        var response: ArrayList<String> = ArrayList(options?.size ?: 1)
)

@TypeFor(field = "type", adapter = SurveyTypeAdapter::class)
open class Survey(
        val type: String,
        open var uuid: String = "",
        open val title: String = "",
        open val message: String = "",
        open val instruction: String = "",
        open val timeoutSec: Long = -1,
        open val timeoutPolicy: String = "",
        open val questions: List<SurveyQuestion> = listOf()
) {
    fun toJson(): String = try {
        Klaxon().toJsonString(this)
    } catch (e: Exception) {
        ""
    }

    companion object {
        inline fun <reified T : Survey> parse(surveyString: String?): T {

            return try {
                if (TextUtils.isEmpty(surveyString)) throw EmptySurveyException()
                val survey = Klaxon().parse<T>(surveyString!!)
                        ?: throw InvalidSurveyFormatException()
                if (TextUtils.isEmpty(survey.title)) throw InvalidSurveyFormatException()
                survey
            } catch (e: KlaxonException) {
                throw InvalidSurveyFormatException()
            }
        }

        const val TIMEOUT_ALT_TEXT = "TIMEOUT_ALT_TEXT"
        const val TIMEOUT_DISABLED = "TIMEOUT_DISABLED"
        const val TIMEOUT_NONE = "TIMEOUT_NONE"

        const val QUESTION_SINGLE_TEXT = "QUESTION_SINGLE_TEXT"
        const val QUESTION_RADIO_BUTTON = "QUESTION_RADIO_BUTTON"
        const val QUESTION_MULTIPLE_TEXTS = "QUESTION_MULTIPLE_TEXTS"
        const val QUESTION_CHECK_BOX = "QUESTION_CHECK_BOX"
        const val QUESTION_RADIO_BUTTON_HORIZONTAL = "QUESTION_RADIO_BUTTON_HORIZONTAL"
        const val QUESTION_SLIDER = "QUESTION_SLIDER"
    }
}

data class IntervalBasedSurvey(
        override var uuid: String = UUID.randomUUID().toString(),
        override val title: String,
        override val message: String,
        override val instruction: String,
        override val timeoutSec: Long,
        override val timeoutPolicy: String,
        override val questions: List<SurveyQuestion>,
        val initialDelaySec: Long = -1,
        val intervalSec: Long = -1,
        val flexIntervalSec: Long = -1,
        val dailyStartTimeHour: Int = -1,
        val dailyStartTimeMinute: Int = -1,
        val dailyEndTimeHour: Int = -1,
        val dailyEndTimeMinute: Int = -1,
        val daysOfWeek: List<DayOfWeek> = DayOfWeek.values().toList()
) : Survey("interval", uuid, title, message, instruction, timeoutSec, timeoutPolicy, questions)


data class EventBasedSurvey(
        override var uuid: String = UUID.randomUUID().toString(),
        override val title: String,
        override val message: String,
        override val instruction: String,
        override val timeoutSec: Long,
        override val timeoutPolicy: String,
        override val questions: List<SurveyQuestion>,
        val triggerEvents: List<String> = listOf(),
        val cancelEvents: List<String> = listOf(),
        val delayAfterTriggerSec: Long = -1,
        val dailyStartTimeHour: Int = -1,
        val dailyStartTimeMinute: Int = -1,
        val dailyEndTimeHour: Int = -1,
        val dailyEndTimeMinute: Int = -1,
        val daysOfWeek: List<DayOfWeek> = DayOfWeek.values().toList()
) : Survey("event", uuid, title, message, instruction, timeoutSec, timeoutPolicy, questions)


data class ScheduleBasedSurvey(
        override var uuid: String = UUID.randomUUID().toString(),
        override val title: String,
        override val message: String,
        override val instruction: String,
        override val timeoutSec: Long,
        override val timeoutPolicy: String,
        override val questions: List<SurveyQuestion>,
        val schedules: List<Schedule> = listOf()
) : Survey("schedule", uuid, title, message, instruction, timeoutSec, timeoutPolicy, questions)


class SurveyTypeAdapter : TypeAdapter<Survey> {
    override fun classFor(type: Any): KClass<out Survey> = when (type as String) {
        "interval" -> IntervalBasedSurvey::class
        "event" -> EventBasedSurvey::class
        "schedule" -> ScheduleBasedSurvey::class
        else -> throw IllegalArgumentException("Unknown survey type: $type; " +
                "a type of survey should be one of [interval, schedule, event]")
    }
}