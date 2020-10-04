package kaist.iclab.abclogger.collector.survey

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity
import kaist.iclab.abclogger.commons.JsonConverter

@Entity
data class SurveyEntity(
    var eventTime: Long = Long.MIN_VALUE,
    var eventName: String = "",
    var intendedTriggerTime: Long = Long.MIN_VALUE,
    var actualTriggerTime: Long = Long.MIN_VALUE,
    var reactionTime: Long = Long.MIN_VALUE,
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
    @Convert(converter = ResponseListConverter::class, dbType = String::class)
    var responses: List<Response> = listOf()
) : AbstractEntity() {
    data class Response(
        val index: Int = Int.MIN_VALUE,
        val type: String = "",
        val question: String = "",
        val altQuestion: String = "",
        val answer: Set<String> = setOf()
    )
}

class ResponseListConverter : JsonConverter<List<SurveyEntity.Response>>(
    adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(Types.newParameterizedType(List::class.java, SurveyEntity.Response::class.java)),
    default = listOf()
)