package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.data.types.PhysicalActivityTransitionType
import kaist.iclab.abclogger.data.types.PhysicalActivityTransitionTypeConverter

@Entity
data class PhysicalActivityTransitionEntity(
    @Convert(converter = PhysicalActivityTransitionTypeConverter::class, dbType = String::class)
    var transitionType: PhysicalActivityTransitionType = PhysicalActivityTransitionType.NONE
) : BaseEntity()