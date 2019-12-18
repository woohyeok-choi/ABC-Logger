package kaist.iclab.abclogger.data.types

import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.survey.SurveyTime
import kaist.iclab.abclogger.survey.SurveyTimeoutPolicyType
import java.util.concurrent.TimeUnit


class SurveyTimeoutPolicyTypeConverter: PropertyConverter<SurveyTimeoutPolicyType, String> {
    override fun convertToDatabaseValue(entityProperty: SurveyTimeoutPolicyType?): String {
        return entityProperty?.name ?: SurveyTimeoutPolicyType.NONE.name
    }

    override fun convertToEntityProperty(databaseValue: String?): SurveyTimeoutPolicyType {
        return try { SurveyTimeoutPolicyType.valueOf(databaseValue!!)} catch (e: Exception) { SurveyTimeoutPolicyType.NONE }
    }
}

class SurveyTimeConverter: PropertyConverter<SurveyTime, String> {
    override fun convertToDatabaseValue(entityProperty: SurveyTime?): String {
        return entityProperty?.let {
           "${it.value},${it.unit.name}"
        } ?: "${Long.MIN_VALUE},${TimeUnit.MILLISECONDS.name}"
    }

    override fun convertToEntityProperty(databaseValue: String?): SurveyTime {
        return try {
            databaseValue?.let {
                val parts = it.split(",")
                SurveyTime(parts[0].toLong(), TimeUnit.valueOf(parts[1]))
            } ?: SurveyTime(Long.MIN_VALUE, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            SurveyTime(Long.MIN_VALUE, TimeUnit.MILLISECONDS)
        }
    }
}