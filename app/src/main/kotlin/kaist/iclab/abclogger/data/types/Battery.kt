package kaist.iclab.abclogger.data.types

import android.os.BatteryManager
import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.common.type.EnumMap
import kaist.iclab.abclogger.common.type.HasId
import kaist.iclab.abclogger.common.type.buildValueMap

enum class BatteryPluggedType (override val id: Int): HasId {
    UNDEFINED(0),
    AC(BatteryManager.BATTERY_PLUGGED_AC),
    USB(BatteryManager.BATTERY_PLUGGED_USB),
    WIRELESS(BatteryManager.BATTERY_PLUGGED_WIRELESS);

    companion object: EnumMap<BatteryPluggedType>(buildValueMap())
}

class  BatteryPluggedTypeConverter: PropertyConverter<BatteryPluggedType, String> {
    override fun convertToDatabaseValue(entityProperty: BatteryPluggedType?): String {
        return entityProperty?.name ?: BatteryPluggedType.UNDEFINED.name
    }

    override fun convertToEntityProperty(databaseValue: String?): BatteryPluggedType {
        return try { BatteryPluggedType.valueOf(databaseValue!!)} catch (e: Exception) { BatteryPluggedType.UNDEFINED }
    }
}

enum class BatteryStatusType (override val id: Int) : HasId {
    UNDEFINED(0),
    CHARGING(BatteryManager.BATTERY_STATUS_CHARGING),
    DISCHARGING(BatteryManager.BATTERY_STATUS_DISCHARGING),
    FULL(BatteryManager.BATTERY_STATUS_FULL),
    NOT_CHARGING(BatteryManager.BATTERY_STATUS_NOT_CHARGING),
    UNKNOWN(BatteryManager.BATTERY_STATUS_UNKNOWN);

    companion object: EnumMap<BatteryStatusType>(buildValueMap())
}

class BatteryStatusTypeConverter: PropertyConverter<BatteryStatusType, String> {
    override fun convertToDatabaseValue(entityProperty: BatteryStatusType?): String {
        return entityProperty?.name ?: BatteryStatusType.UNDEFINED.name
    }

    override fun convertToEntityProperty(databaseValue: String?): BatteryStatusType {
        return try { BatteryStatusType.valueOf(databaseValue!!)} catch (e: Exception) { BatteryStatusType.UNDEFINED }
    }
}
