package kaist.iclab.abclogger.collector.survey

import androidx.databinding.BaseObservable
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import kaist.iclab.abclogger.collector.AbstractEntity
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.survey.AltText
import kaist.iclab.abclogger.survey.Option
import kaist.iclab.abclogger.survey.Question
import kaist.iclab.abclogger.survey.Survey

@Entity
data class InternalSurveyEntity(
    @Id var id: Long = 0,
    var triggerEvent: String = "",
    var cancelEvent: String = "",
    var intendedTriggerTime: Long = Long.MIN_VALUE,
    var actualTriggerTime: Long = Long.MIN_VALUE,
    var firstReactionTime: Long = Long.MIN_VALUE,
    var lastReactionTime: Long = Long.MIN_VALUE,
    var responseTime: Long = Long.MIN_VALUE,
    var url: String = "",
    @Convert(converter = AltTextConverter::class, dbType = String::class)
    var title: AltText = AltText(),
    @Convert(converter = AltTextConverter::class, dbType = String::class)
    var message: AltText = AltText(),
    @Convert(converter = AltTextConverter::class, dbType = String::class)
    var instruction: AltText = AltText(),
    var timeoutUntil: Long = Long.MIN_VALUE,
    @Convert(converter = TimeoutActionConverter::class, dbType = Int::class)
    var timeoutAction: Survey.TimeoutAction = Survey.TimeoutAction.NONE,
    @Convert(converter = LongListConverter::class, dbType = String::class)
    var responses: List<Long> = listOf()
) {
    fun isAnswered(): Boolean = responseTime >= 0

    fun isExpired(timestamp: Long): Boolean =
        timestamp > timeoutUntil && timeoutAction == Survey.TimeoutAction.DISABLED

    fun isAltTextShown(timestamp: Long): Boolean {
        val baseTime = if (isAnswered()) responseTime else timestamp
        return baseTime > timeoutUntil && timeoutAction == Survey.TimeoutAction.ALT_TEXT
    }

    fun isEnabled(timestamp: Long) = !isAnswered() && !isExpired(timestamp)
}

@Entity
data class InternalResponseEntity(
    @Id var id: Long = 0,
    var surveyId: Long = 0,
    var index: Int = 0,
    @Convert(converter = QuestionConverter::class, dbType = String::class)
    var question: Question = Question(),
    @Convert(converter = AnswerConverter::class, dbType = String::class)
    var answer: Answer = Answer(true)
)

class Answer(val mutualExclusive: Boolean) : BaseObservable() {
    fun isEmptyAnswer() = main.isEmpty() && other.isBlank()

    @Transient
    var isInvalid: Boolean = false
        set(value) {
            field = value
            notifyChange()
        }

    var main: Set<String> = setOf()
        set(value) {
            if (field == value) return
            field = value
            notifyChange()

            if (field.isNotEmpty() && mutualExclusive) {
                other = ""
            }
        }

    var other: String = ""
        set(value) {
            if (field == value) return
            field = value
            notifyChange()

            if (field.isNotEmpty() && mutualExclusive) {
                main = setOf()
            }
        }
}

@Entity
data class SurveyEntity(
    var triggerEvent: String = "",
    var cancelEvent: String = "",
    var intendedTriggerTime: Long = Long.MIN_VALUE,
    var actualTriggerTime: Long = Long.MIN_VALUE,
    var firstReactionTime: Long = Long.MIN_VALUE,
    var lastReactionTime: Long = Long.MIN_VALUE,
    var responseTime: Long = Long.MIN_VALUE,
    var url: String = "",
    var title: String = "",
    var altTitle: String = "",
    var message: String = "",
    var altMessage: String = "",
    var instruction: String = "",
    var altInstruction: String = "",
    var timeoutUntil: Long = Long.MIN_VALUE,
    var timeoutAction: String = "",
    @Convert(converter = SyncableResponseConverter::class, dbType = String::class)
    var responses: List<SyncableResponse> = listOf()
) : AbstractEntity()

data class SyncableResponse(
    val index: Int = Int.MIN_VALUE,
    val type: String = "",
    val question: String = "",
    val altQuestion: String = "",
    val answer: Set<String> = setOf()
)

class AnswerConverter : JsonConverter<Answer>(
    adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build().adapter(Answer::class.java),
    default = Answer(true)
)

class QuestionConverter : JsonConverter<Question>(
    adapter = Moshi.Builder()
        .add(Question.Factory)
        .add(KotlinJsonAdapterFactory())
        .build().adapter(Question::class.java),
    default = Question()
)

class TimeoutActionConverter : EnumConverter<Survey.TimeoutAction>(
    enumValues = Survey.TimeoutAction.values(),
    default = Survey.TimeoutAction.NONE
)

class AltTextConverter : JsonConverter<AltText>(
    adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build().adapter(AltText::class.java),
    default = AltText()
)

class SyncableResponseConverter : JsonConverter<SyncableResponse>(
    adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(Types.newParameterizedType(List::class.java, SyncableResponse::class.java)),
    default = SyncableResponse()
)