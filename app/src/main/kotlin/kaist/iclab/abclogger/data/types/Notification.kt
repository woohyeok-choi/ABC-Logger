package kaist.iclab.abclogger.data.types

import android.app.Notification
import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.common.type.EnumMap
import kaist.iclab.abclogger.common.type.HasId
import kaist.iclab.abclogger.common.type.buildValueMap

enum class NotificationVisibilityType (override val id: Int): HasId {
    UNDEFINED(0),
    PRIVATE(Notification.VISIBILITY_PRIVATE),
    PUBLIC(Notification.VISIBILITY_PUBLIC),
    SECRET(Notification.VISIBILITY_SECRET);

    companion object: EnumMap<NotificationVisibilityType>(buildValueMap())
}

class  NotificationVisibilityTypeConverter: PropertyConverter<NotificationVisibilityType, String> {
    override fun convertToDatabaseValue(entityProperty: NotificationVisibilityType?): String {
        return entityProperty?.name ?: NotificationVisibilityType.UNDEFINED.name
    }

    override fun convertToEntityProperty(databaseValue: String?): NotificationVisibilityType {
        return try { NotificationVisibilityType.valueOf(databaseValue!!)} catch (e: Exception) { NotificationVisibilityType.UNDEFINED }
    }
}
