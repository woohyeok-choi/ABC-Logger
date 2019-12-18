package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Entity


@Entity
data class EmotionalStatusEntity(
    var anger: Float = Float.MIN_VALUE,
    var contempt: Float = Float.MIN_VALUE,
    var disgust: Float = Float.MIN_VALUE,
    var fear: Float = Float.MIN_VALUE,
    var happiness: Float = Float.MIN_VALUE,
    var neutral: Float = Float.MIN_VALUE,
    var sadness: Float = Float.MIN_VALUE,
    var surprise: Float = Float.MIN_VALUE
) : BaseEntity()