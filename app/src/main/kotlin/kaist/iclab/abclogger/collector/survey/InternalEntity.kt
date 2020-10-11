package kaist.iclab.abclogger.collector.survey

import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.BaseObservable
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.structure.survey.AltText
import kaist.iclab.abclogger.structure.survey.Question
import kaist.iclab.abclogger.structure.survey.Survey

@Entity
data class InternalSurveyEntity(
    @Id var id: Long = 0,
    var isTransferredToSync: Boolean = false,
    var uuid: String = "",
    var eventTime: Long = Long.MIN_VALUE,
    var eventName: String = "",
    var intendedTriggerTime: Long = Long.MIN_VALUE,
    var actualTriggerTime: Long = Long.MIN_VALUE,
    var reactionTime: Long = Long.MIN_VALUE,
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
    @io.objectbox.annotation.Transient
    var responses: List<InternalResponseEntity> = listOf()
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
    @Convert(converter = InternalAnswerConverter::class, dbType = String::class)
    var answer: InternalAnswer = InternalAnswer(true)
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt(),
        questionConverter.convertToEntityProperty(parcel.readString()),
        answerConverter.convertToEntityProperty(parcel.readString())
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeLong(surveyId)
        parcel.writeInt(index)
        parcel.writeString(questionConverter.convertToDatabaseValue(question))
        parcel.writeString(answerConverter.convertToDatabaseValue(answer))
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<InternalResponseEntity> {
        override fun createFromParcel(parcel: Parcel): InternalResponseEntity {
            return InternalResponseEntity(parcel)
        }

        override fun newArray(size: Int): Array<InternalResponseEntity?> {
            return arrayOfNulls(size)
        }

        private val questionConverter = QuestionConverter()
        private val answerConverter = InternalAnswerConverter()
    }
}

class InternalAnswer(
    private val mutualExclusive: Boolean = true,
    mainAnswer: Set<String> = setOf(),
    otherAnswer: String = ""
) : BaseObservable() {
    fun isEmptyAnswer() = main.isEmpty() && other.isBlank()

    @Transient
    var isInvalid: Boolean = false
        set(value) {
            field = value
            notifyChange()
        }

    var main: Set<String> = mainAnswer
        set(value) {
            if (field == value) return
            field = value
            notifyChange()

            if (field.isNotEmpty() && mutualExclusive) {
                other = ""
            }
        }

    var other: String = otherAnswer
        set(value) {
            if (field == value) return
            field = value
            notifyChange()

            if (field.isNotEmpty() && mutualExclusive) {
                main = setOf()
            }
        }
}

class InternalAnswerConverter : JsonConverter<InternalAnswer>(
    adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build().adapter(InternalAnswer::class.java),
    default = InternalAnswer(true)
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

