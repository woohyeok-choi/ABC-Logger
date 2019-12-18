package kaist.iclab.abclogger.data.types

import android.provider.CallLog
import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.common.type.EnumMap
import kaist.iclab.abclogger.common.type.HasId
import kaist.iclab.abclogger.common.type.buildValueMap

enum class CallType (override val id: Int): HasId {
    UNDEFINED(0),
    INCOMING(CallLog.Calls.INCOMING_TYPE),
    OUTGOING(CallLog.Calls.OUTGOING_TYPE),
    MISSED(CallLog.Calls.MISSED_TYPE);

    companion object: EnumMap<CallType>(buildValueMap())
}

class  CallTypeConverter: PropertyConverter<CallType, String> {
    override fun convertToDatabaseValue(entityProperty: CallType?): String {
        return entityProperty?.name ?: CallType.UNDEFINED.name
    }

    override fun convertToEntityProperty(databaseValue: String?): CallType {
        return try { CallType.valueOf(databaseValue!!)} catch (e: Exception) { CallType.UNDEFINED }
    }
}

enum class CallPresentationType (override val id: Int): HasId {
    UNDEFINED(0),
    ALLOWED(CallLog.Calls.PRESENTATION_ALLOWED),
    PAYPHONE(CallLog.Calls.PRESENTATION_PAYPHONE),
    RESTRICTED(CallLog.Calls.PRESENTATION_RESTRICTED),
    UNKNOWN(CallLog.Calls.PRESENTATION_UNKNOWN);

    companion object: EnumMap<CallPresentationType>(buildValueMap())
}

class CallPresentationTypeConverter: PropertyConverter<CallPresentationType, String> {
    override fun convertToDatabaseValue(entityProperty: CallPresentationType?): String {
        return entityProperty?.name ?: CallPresentationType.UNDEFINED.name
    }

    override fun convertToEntityProperty(databaseValue: String?): CallPresentationType {
        return try { CallPresentationType.valueOf(databaseValue!!)} catch (e: Exception) { CallPresentationType.UNDEFINED }
    }
}
