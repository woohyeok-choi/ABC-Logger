package kaist.iclab.abclogger.data.types

import android.app.usage.UsageEvents
import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.common.type.EnumMap
import kaist.iclab.abclogger.common.type.HasId
import kaist.iclab.abclogger.common.type.buildValueMap

enum class AppUsageEventType (override val id: Int): HasId {
    NONE(0),
    CONFIGURATION_CHANGE(UsageEvents.Event.CONFIGURATION_CHANGE),
    KEYGUARD_HIDDEN(0x00000012),
    KEYGUARD_SHOWN(0x00000017),
    MOVE_TO_BACKGROUND(UsageEvents.Event.MOVE_TO_BACKGROUND),
    MOVE_TO_FOREGROUND(UsageEvents.Event.MOVE_TO_FOREGROUND),
    SCREEN_INTERACTIVE(0x0000000f),
    SCREEN_NON_INTERACTIVE(0x00000010),
    SHORTCUT_INVOCATION(0x00000008),
    USER_INTERACTION(0x00000007);

    companion object: EnumMap<AppUsageEventType>(buildValueMap())
}

class  AppUsageEventTypeConverter: PropertyConverter<AppUsageEventType, String> {
    override fun convertToDatabaseValue(entityProperty: AppUsageEventType?): String {
        return entityProperty?.name ?: AppUsageEventType.NONE.name
    }

    override fun convertToEntityProperty(databaseValue: String?): AppUsageEventType {
        return try { AppUsageEventType.valueOf(databaseValue!!)} catch (e: Exception) { AppUsageEventType.NONE }
    }
}
