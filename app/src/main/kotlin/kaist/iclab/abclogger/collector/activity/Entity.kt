package kaist.iclab.abclogger.collector.activity

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity
import kaist.iclab.abclogger.commons.JsonConverter


@Entity
data class PhysicalActivityEntity(
        @Convert(converter = PhysicalActivityConverter::class, dbType = String::class)
        var activities: List<Activity> = listOf()
) : AbstractEntity() {
    data class Activity(
            val type: String = "",
            val confidence: Int = Int.MIN_VALUE
    )
}

class PhysicalActivityConverter: JsonConverter<List<PhysicalActivityEntity.Activity>>(
        adapter = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(Types.newParameterizedType(List::class.java, PhysicalActivityEntity.Activity::class.java)),
        default = listOf()
)
