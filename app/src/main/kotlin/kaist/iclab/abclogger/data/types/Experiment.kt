package kaist.iclab.abclogger.data.types

import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.common.type.HourMin
import kaist.iclab.abclogger.common.type.YearMonthDay

class HourMinConverter: PropertyConverter<HourMin, String> {
    override fun convertToDatabaseValue(entityProperty: HourMin?): String {
        return entityProperty?.toString() ?: HourMin.now().toString()
    }

    override fun convertToEntityProperty(databaseValue: String?): HourMin {
        return try { HourMin.fromString(databaseValue!!) } catch (e: Exception) { HourMin.now() }
    }
}

class YearMonthDayConverter: PropertyConverter<YearMonthDay, String> {
    override fun convertToDatabaseValue(entityProperty: YearMonthDay?): String {
        return entityProperty?.toString() ?: YearMonthDay.now().toString()
    }

    override fun convertToEntityProperty(databaseValue: String?): YearMonthDay {
        return try { YearMonthDay.fromString(databaseValue!!) ?: YearMonthDay.now() }
        catch (e: Exception) { YearMonthDay.now() }
    }
}