package kaist.iclab.abclogger.core

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import com.squareup.moshi.JsonAdapter
import kaist.iclab.abclogger.EventBus
import kaist.iclab.abclogger.collector.formatDateTime
import kaist.iclab.abclogger.commons.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.Serializable
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

/**
 * Default interface for data collector
 */
abstract class AbstractCollector<E : AbstractEntity>(
        val context: Context,
        val qualifiedName: String,
        val name: String,
        val description: String,
        val dataRepository: DataRepository
) {
    private val collectorScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        get() {
            return if (field.isActive) {
                field
            } else {
                CoroutineScope(SupervisorJob() + Dispatchers.IO)
            }
        }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val time = System.currentTimeMillis()
        val exception = AbcError.wrap(throwable)
        val message = exception.toString(context)
        val formattedTime = formatDateTime(context, time)

        NotificationRepository.notifyError(
            context = context,
            timestamp = time,
            where = name,
            error = exception
        )
        Log.e(javaClass, exception, report = true)

        lastErrorMessage = "[$formattedTime] $message"
        statusChannel.offer(Status.ERROR)
    }

    internal val preference: SharedPreferences by lazy {
        context.applicationContext.getSharedPreferences(qualifiedName, Context.MODE_PRIVATE)
    }

    var isEnabled: Boolean by ReadWriteStatusBoolean(false)
        private set

    var lastErrorMessage: String by ReadWriteStatusString("")
        private set

    var turnedOnTime: Long by ReadWriteStatusLong(Long.MIN_VALUE)
        private set

    var lastTimeDataWritten: Long by ReadWriteStatusLong(Long.MIN_VALUE)

    var recordsCollected: Long by ReadWriteStatusLong(0)

    var recordsUploaded: Long by ReadWriteStatusLong(0)

    private val statusChannel = Channel<Status>()

    val status = statusChannel.receiveAsFlow()

    abstract val permissions: List<String>

    abstract val setupIntent: Intent?

    enum class Status {
        OFF, ON, ERROR;
    }


    /**
     * Define operations when a user requests for starting this collector.
     * This function should be called in non-UI thread.
     */
    @WorkerThread
    abstract suspend fun onStart()

    /**
     * Define operations when a user requests for stopping this collector.
     * This function should be called in non-UI thread.
     */
    @WorkerThread
    abstract suspend fun onStop()

    abstract fun isAvailable(): Boolean

    /**
     * Describing status of current status of the collector.
     * This is only used to show the status to uses via UI;
     * therefore, keys of map should string res to support different locales.
     * @return a map of descriptions, whose key is integer of StringRes
     */
    abstract fun getStatus(): Array<Info>

    data class Info(
            @StringRes
            val stringRes: Int,
            val value: Any?
    ) : Serializable

    fun isPermissionGranted() = isPermissionGranted(context, permissions)

    fun start() {
        isEnabled = true
        lastErrorMessage = ""

        turnedOnTime = if (turnedOnTime > 0) {
            turnedOnTime
        } else {
            System.currentTimeMillis()
        }

        statusChannel.offer(Status.ON)

        launch {
            onStart()
        }
    }

    /**
     * function stop is called only by a user's explicit interaction.
     */
    fun stop() {
        isEnabled = false
        turnedOnTime = Long.MIN_VALUE

        statusChannel.offer(Status.OFF)

        launch {
            onStop()
        }

        collectorScope.cancel()
    }

    fun clear() {
        this::class.declaredMemberProperties.filterIsInstance<KProperty1<AbstractCollector<*>, *>>().forEach {
            (it.getDelegate(this) as? ClearableStatusProperty)?.clear(this, it)
        }
    }

    suspend inline fun <reified T: Any> put(datum: T, timeInMillis: Long = System.currentTimeMillis(), isStatUpdates: Boolean = true) {
        val entity = if (datum is AbstractEntity) {
            datum.apply {
                timestamp = if (timestamp < 0) timeInMillis else timestamp
                utcOffset = TimeZone.getDefault().rawOffset / 1000
                email = AuthRepository.email()
                deviceInfo = AuthRepository.deviceInfo()
                deviceId = AuthRepository.instanceId()
            }
        } else {
            datum
        }

        dataRepository.put(entity)

        if (isStatUpdates) {
            recordsCollected++
            lastTimeDataWritten = timeInMillis.coerceAtLeast(lastTimeDataWritten)
        }

        EventBus.post(datum)
    }


    suspend inline fun <reified T: Any> put(data: Collection<T>, timeInMillis: Long = System.currentTimeMillis(), isStatUpdates: Boolean = true) {
        val entities = data.map { datum ->
            if (datum is AbstractEntity) {
                datum.apply {
                    timestamp = if (timestamp < 0) timeInMillis else timestamp
                    utcOffset = TimeZone.getDefault().rawOffset / 1000
                    email = AuthRepository.email()
                    deviceInfo = AuthRepository.deviceInfo()
                    deviceId = AuthRepository.instanceId()
                }
            } else {
                datum
            }
        }
        dataRepository.put(entities)

        if (isStatUpdates) {
            recordsCollected += entities.size
            lastTimeDataWritten = timeInMillis.coerceAtLeast(lastTimeDataWritten)
        }

        entities.forEach { EventBus.post(it) }
    }

    abstract suspend fun count() : Long

    abstract suspend fun flush(entities: Collection<E>)

    abstract suspend fun list(limit: Long) : Collection<E>

    protected fun launch(block: suspend () -> Unit) = collectorScope.launch(coroutineExceptionHandler) {
        block.invoke()
    }
}

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
