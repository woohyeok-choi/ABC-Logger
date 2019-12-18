package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.data.types.SurveyTimeConverter
import kaist.iclab.abclogger.data.types.SurveyTimeoutPolicyTypeConverter
import kaist.iclab.abclogger.survey.SurveyTime
import kaist.iclab.abclogger.survey.SurveyTimeoutPolicyType
import java.util.concurrent.TimeUnit

@Entity
data class SurveyEntity(
    var title: String = "",
    var message: String = "",
    var deliveredTime: Long = Long.MIN_VALUE,
    var reactionTime: Long = Long.MIN_VALUE,
    var firstQuestionTime: Long = Long.MIN_VALUE,
    var responses: String = "",
    @Convert(converter = SurveyTimeoutPolicyTypeConverter::class, dbType = String::class)
    var timeoutPolicy: SurveyTimeoutPolicyType = SurveyTimeoutPolicyType.NONE,
    @Convert(converter = SurveyTimeConverter::class, dbType = String::class)
    var timeout: SurveyTime = SurveyTime(Long.MIN_VALUE, TimeUnit.MILLISECONDS)
) : BaseEntity() {
    fun isEnableToResponed(now: Long) : Boolean {
        var isAfterTimeout = now - deliveredTime >= timeout.toMillis()
        return timestamp <= 0 && (!isAfterTimeout || timeoutPolicy != SurveyTimeoutPolicyType.DISABLED)
    }

    companion object {
        fun numberNotRepliedEntities(entity: ParticipationEntity, now: Long): Int {
            return App.boxFor<SurveyEntity>().query().filter {
                it.experimentUuid == entity.experimentUuid &&
                    it.subjectEmail == entity.subjectEmail &&
                    it.timestamp <= 0 &&
                    it.deliveredTime >= entity.participateTime &&
                    (now - it.deliveredTime < it.timeout.toMillis() || it.timeoutPolicy != SurveyTimeoutPolicyType.DISABLED)
            }.build().find().count()
        }
    }
}
