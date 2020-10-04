package kaist.iclab.abclogger.commons

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.objectbox.converter.PropertyConverter

open class JsonConverter<T>(val adapter: JsonAdapter<T>, val default: T) : PropertyConverter<T, String> {
    override fun convertToDatabaseValue(entityProperty: T): String =
            try {
                adapter.toJson(entityProperty) ?: ""
            } catch (e: Exception) {
                ""
            }

    override fun convertToEntityProperty(databaseValue: String?): T =
            try {
                adapter.fromJson(databaseValue ?: "") ?: default
            } catch (e: Exception) {
                default
            }
}

class LongListConverter: JsonConverter<List<Long>>(
        adapter = Moshi.Builder().build().adapter(
                Types.newParameterizedType(List::class.java, String::class.java)
        ),
        default = listOf()
)


class StringListConverter: JsonConverter<List<String>>(
        adapter = Moshi.Builder().build().adapter(
                Types.newParameterizedType(List::class.java, String::class.java)
        ),
        default = listOf()
)

class StringSetConverter: JsonConverter<Set<String>>(
        adapter = Moshi.Builder().build().adapter(
                Types.newParameterizedType(Set::class.java, String::class.java)
        ),
        default = setOf()
)


class StringMapConverter: JsonConverter<Map<String, String>>(
        adapter = Moshi.Builder().build().adapter(
                Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        ),
        default = mapOf()
)

open class EnumConverter<T : Enum<T>> (private val enumValues: Array<T>, private val default: T): PropertyConverter<T, Int> {
    override fun convertToEntityProperty(databaseValue: Int?): T =
            enumValues.find { it.ordinal == databaseValue } ?: default

    override fun convertToDatabaseValue(entityProperty: T): Int =
        entityProperty.ordinal
}

