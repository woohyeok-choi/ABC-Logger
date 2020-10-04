package kaist.iclab.abclogger.commons

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.objectbox.converter.PropertyConverter
import kaist.iclab.abclogger.collector.survey.*

class StringListConverter : PropertyConverter<List<String>, String> {
    private val type = Types.newParameterizedType(List::class.java, String::class.java)
    private val adapter = Moshi.Builder().build().adapter<List<String>>(type)

    override fun convertToDatabaseValue(entityProperty: List<String>?): String =
            try {
                adapter.toJson(entityProperty ?: listOf()) ?: ""
            } catch (e: Exception) {
                ""
            }

    override fun convertToEntityProperty(databaseValue: String?): List<String> =
            try {
                adapter.fromJson(databaseValue ?: "") ?: listOf()
            } catch (e: Exception) {
                listOf()
            }
}

class NonEmptyStringMapConverter : PropertyConverter<Map<String, String>, String> {
    private val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val adapter = Moshi.Builder().build().adapter<Map<String, String>>(type)

    override fun convertToDatabaseValue(entityProperty: Map<String, String>?): String =
            try {
                val nonEmptyProperty = entityProperty?.filterValues { it.isNotBlank() } ?: mapOf()
                adapter.toJson(nonEmptyProperty) ?: ""
            } catch (e: Exception) {
                ""
            }

    override fun convertToEntityProperty(databaseValue: String?): Map<String, String> =
            try {
                adapter.fromJson(databaseValue ?: "")?.filterValues { it.isNotBlank() } ?: mapOf()
            } catch (e: Exception) {
                mapOf()
            }
}

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