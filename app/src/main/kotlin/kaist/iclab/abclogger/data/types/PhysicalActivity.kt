package kaist.iclab.abclogger.data.types

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.common.type.*

enum class PhysicalActivityType (override val id: Int): HasId {
    UNKNOWN(DetectedActivity.UNKNOWN),
    IN_VEHICLE(DetectedActivity.IN_VEHICLE),
    ON_BICYCLE(DetectedActivity.ON_BICYCLE),
    ON_FOOT(DetectedActivity.ON_FOOT),
    RUNNING(DetectedActivity.RUNNING),
    STILL(DetectedActivity.STILL),
    TILTING(DetectedActivity.TILTING),
    WALKING(DetectedActivity.WALKING),
    IN_ROAD_VEHICLE(16),
    IN_RAIL_VEHICLE(17),
    IN_TWO_WHEELER_VEHICLE(18),
    IN_FOUR_WHEELER_VEHICLE(19);

    companion object: EnumMap<PhysicalActivityType>(buildValueMap())
}


class PhysicalActivityTypeConverter: PropertyConverter<PhysicalActivityType, String> {
    override fun convertToDatabaseValue(entityProperty: PhysicalActivityType?): String {
        return entityProperty?.name ?: PhysicalActivityType.UNKNOWN.name
    }

    override fun convertToEntityProperty(databaseValue: String?): PhysicalActivityType {
        return try { PhysicalActivityType.valueOf(databaseValue!!) } catch (e: Exception) { PhysicalActivityType.UNKNOWN }
    }
}

enum class PhysicalActivityTransitionType (override val row: Int, override val col: Int) : HasRowCol {
    NONE(-1, -1),
    ENTER_IN_VEHICLE(ActivityTransition.ACTIVITY_TRANSITION_ENTER, DetectedActivity.IN_VEHICLE),
    EXIT_IN_VEHICLE(ActivityTransition.ACTIVITY_TRANSITION_EXIT, DetectedActivity.IN_VEHICLE),
    ENTER_RUNNING(ActivityTransition.ACTIVITY_TRANSITION_ENTER, DetectedActivity.RUNNING),
    EXIT_RUNNING(ActivityTransition.ACTIVITY_TRANSITION_EXIT, DetectedActivity.RUNNING),
    ENTER_WALKING(ActivityTransition.ACTIVITY_TRANSITION_ENTER, DetectedActivity.WALKING),
    EXIT_WALKING(ActivityTransition.ACTIVITY_TRANSITION_EXIT, DetectedActivity.WALKING),
    ENTER_ON_BICYCLE(ActivityTransition.ACTIVITY_TRANSITION_ENTER, DetectedActivity.ON_BICYCLE),
    EXIT_ON_BICYCLE(ActivityTransition.ACTIVITY_TRANSITION_EXIT, DetectedActivity.ON_BICYCLE),
    ENTER_STILL(ActivityTransition.ACTIVITY_TRANSITION_ENTER, DetectedActivity.STILL),
    EXIT_STILL(ActivityTransition.ACTIVITY_TRANSITION_EXIT, DetectedActivity.STILL);

    companion object: EnumTable<PhysicalActivityTransitionType>(buildValueTable())
}

class PhysicalActivityTransitionTypeConverter : PropertyConverter<PhysicalActivityTransitionType, String> {
    override fun convertToDatabaseValue(entityProperty: PhysicalActivityTransitionType?): String {
        return entityProperty?.name ?: PhysicalActivityTransitionType.NONE.name
    }

    override fun convertToEntityProperty(databaseValue: String?): PhysicalActivityTransitionType {
        return try { PhysicalActivityTransitionType.valueOf(databaseValue!!) } catch (e: Exception) { PhysicalActivityTransitionType.NONE }
    }
}
