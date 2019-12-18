package kaist.iclab.abclogger.data.types

import android.provider.Telephony
import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.common.type.EnumMap
import kaist.iclab.abclogger.common.type.HasId
import kaist.iclab.abclogger.common.type.buildValueMap

enum class MessageBoxType (override val id: Int): HasId {
    UNDEFINED(0),
    INBOX(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX),
    SENT(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT),
    OUTBOX(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX),
    DRAFT(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT),
    FAILED(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED),
    QUEUED(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED);

    companion object: EnumMap<MessageBoxType>(buildValueMap())
}


class  MessageBoxTypeConverter: PropertyConverter<MessageBoxType, String> {
    override fun convertToDatabaseValue(entityProperty: MessageBoxType?): String {
        return entityProperty?.name ?: MessageBoxType.UNDEFINED.name
    }

    override fun convertToEntityProperty(databaseValue: String?): MessageBoxType {
        return try { MessageBoxType.valueOf(databaseValue!!)} catch (e: Exception) { MessageBoxType.UNDEFINED }
    }
}


enum class MessageClassType (override val id: Int): HasId {
    UNDEFINED(0),
    SMS(1),
    MMS(2);

    companion object: EnumMap<MessageClassType>(buildValueMap())
}

class  MessageClassTypeConverter: PropertyConverter<MessageClassType, String> {
    override fun convertToDatabaseValue(entityProperty: MessageClassType?): String {
        return entityProperty?.name ?: MessageBoxType.UNDEFINED.name
    }

    override fun convertToEntityProperty(databaseValue: String?): MessageClassType {
        return try { MessageClassType.valueOf(databaseValue!!)} catch (e: Exception) { MessageClassType.UNDEFINED }
    }
}
