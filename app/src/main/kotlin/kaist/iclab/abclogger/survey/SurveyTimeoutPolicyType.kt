package kaist.iclab.abclogger.survey

import kaist.iclab.abclogger.common.type.EnumMap
import kaist.iclab.abclogger.common.type.HasId
import kaist.iclab.abclogger.common.type.buildValueMap

enum class SurveyTimeoutPolicyType (override val id: Int): HasId {
    NONE(0),
    DISABLED(1),
    ALTERNATIVE_TEXT(2);

    companion object: EnumMap<SurveyTimeoutPolicyType>(buildValueMap())
}