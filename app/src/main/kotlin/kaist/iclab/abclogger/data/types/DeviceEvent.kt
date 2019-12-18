package kaist.iclab.abclogger.data.types

import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.common.type.EnumMap
import kaist.iclab.abclogger.common.type.HasId
import kaist.iclab.abclogger.common.type.buildValueMap

enum class DeviceEventType (override val id: Int): HasId {
    UNDEFINED(0),
    HEADSET_PLUGGED(1),
    HEADSET_UNPLUGGED(2),
    HEADSET_MIC_PLUGGED(3),
    HEADSET_MIC_UNPLUGGED(4),
    POWER_CONNECTED(5),
    POWER_DISCONNECTED(6),
    TURN_ON_DEVICE(7),
    TURN_OFF_DEVICE(8),
    CHANGE_POWER_SAVE_MODE(9),
    ACTIVATE_POWER_SAVE_MODE(10),
    DEACTIVATE_POWER_SAVE_MODE(11),
    ACTIVATE_AIRPLANE_MODE(12),
    DEACTIVATE_AIRPLANE_MODE(13),
    UNLOCK(14),
    SCREEN_ON(15),
    SCREEN_OFF(16),
    RINGER_MODE_NORMAL(17),
    RINGER_MODE_SILENT(18),
    RINGER_MODE_VIBRATE(19),
    BATTERY_LOW(20),
    BATTERY_OKAY(21);

    companion object: EnumMap<DeviceEventType>(buildValueMap())
}

class  DeviceEventTypeConverter: PropertyConverter<DeviceEventType, String> {
    override fun convertToDatabaseValue(entityProperty: DeviceEventType?): String {
        return entityProperty?.name ?: DeviceEventType.UNDEFINED.name
    }

    override fun convertToEntityProperty(databaseValue: String?): DeviceEventType {
        return try { DeviceEventType.valueOf(databaseValue!!)} catch (e: Exception) { DeviceEventType.UNDEFINED }
    }
}
