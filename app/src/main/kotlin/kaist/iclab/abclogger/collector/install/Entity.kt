package kaist.iclab.abclogger.collector.install

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity
import kaist.iclab.abclogger.commons.JsonConverter

@Entity
data class InstalledAppEntity(
        @Convert(converter = InstalledAppsConverter::class, dbType = String::class)
        val apps: List<App> = listOf()
) : AbstractEntity() {
    data class App(
            var name: String = "",
            var packageName: String = "",
            var isSystemApp: Boolean = false,
            var isUpdatedSystemApp: Boolean = false,
            var firstInstallTime: Long = Long.MIN_VALUE,
            var lastUpdateTime: Long = Long.MIN_VALUE
    )
}

class InstalledAppsConverter : JsonConverter<List<InstalledAppEntity.App>>(
        adapter = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(Types.newParameterizedType(List::class.java, InstalledAppEntity.App::class.java)),
        default = listOf()
)