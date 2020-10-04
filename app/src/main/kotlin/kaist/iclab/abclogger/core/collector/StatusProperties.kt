package kaist.iclab.abclogger.core.collector

import com.squareup.moshi.JsonAdapter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


interface ClearableStatusProperty {
    fun clear(thisRef: AbstractCollector<*>, property: KProperty<*>)
}

abstract class ReadWritePrimitiveProperty<T>(val default: T) : ReadWriteProperty<AbstractCollector<*>, T>,
    ClearableStatusProperty {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: AbstractCollector<*>, property: KProperty<*>): T {
        val key = "${thisRef.qualifiedName}.${property.name}"

        return when (default) {
            is String -> thisRef.preference.getString(key, default as String) as T
            is Int -> thisRef.preference.getInt(key, default as Int) as T
            is Long -> thisRef.preference.getLong(key, default as Long) as T
            is Float -> thisRef.preference.getFloat(key, default as Float) as T
            is Boolean -> thisRef.preference.getBoolean(key, default as Boolean) as T
            else -> default
        }
    }

    override fun setValue(thisRef: AbstractCollector<*>, property: KProperty<*>, value: T) {
        val key = "${thisRef.qualifiedName}.${property.name}"

        when (value) {
            is String -> thisRef.preference.edit().putString(key, value).apply()
            is Int -> thisRef.preference.edit().putInt(key, value).apply()
            is Long -> thisRef.preference.edit().putLong(key, value).apply()
            is Float -> thisRef.preference.edit().putFloat(key, value).apply()
            is Boolean -> thisRef.preference.edit().putBoolean(key, value).apply()
        }
    }

    override fun clear(thisRef: AbstractCollector<*>, property: KProperty<*>) {
        setValue(thisRef, property, default)
    }
}

class ReadWriteStatusJson<T: Any>(val default: T, val adapter: JsonAdapter<T>): ReadWriteProperty<AbstractCollector<*>, T>,
    ClearableStatusProperty {
    override fun getValue(thisRef: AbstractCollector<*>, property: KProperty<*>): T {
        val key = "${thisRef.qualifiedName}.${property.name}"
        val json = thisRef.preference.getString(key, "") ?: ""
        return try {
            adapter.fromJson(json) ?: default
        } catch (e: Exception) {
            default
        }
    }

    override fun setValue(thisRef: AbstractCollector<*>, property: KProperty<*>, value: T) {
        val key = "${thisRef.qualifiedName}.${property.name}"
        val json = try {
            adapter.toJson(value) ?: ""
        } catch (e: Exception) {
            ""
        }
        thisRef.preference.edit().putString(key, json).apply()
    }

    override fun clear(thisRef: AbstractCollector<*>, property: KProperty<*>) {
        setValue(thisRef, property, default)
    }
}


class ReadWriteStatusBoolean(default: Boolean): ReadWritePrimitiveProperty<Boolean>(default)
class ReadWriteStatusString(default: String): ReadWritePrimitiveProperty<String>(default)
class ReadWriteStatusInt(default: Int): ReadWritePrimitiveProperty<Int>(default)
class ReadWriteStatusLong(default: Long): ReadWritePrimitiveProperty<Long>(default)
class ReadWriteStatusFloat(default: Float): ReadWritePrimitiveProperty<Float>(default)
