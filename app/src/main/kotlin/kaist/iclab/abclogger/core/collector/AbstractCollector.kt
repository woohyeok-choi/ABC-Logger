package kaist.iclab.abclogger.core.collector

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.*
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        get() =
            if (field.isActive) {
                field
            } else {
                CoroutineScope(SupervisorJob() + Dispatchers.IO)
            }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val time = System.currentTimeMillis()
        val exception = AbcError.wrap(throwable)
        val message = exception.toSimpleString(context)

        NotificationRepository.notifyCollectorError(
            context = context,
            timestamp = time,
            message = message,
            name = name,
            qualifiedName = qualifiedName,
            description = description
        )

        Log.e(javaClass, throwable, report = true)
        lastErrorMessage = message
        statusChannel.offer(getStatus())
    }

    internal val preference: SharedPreferences by lazy {
        context.applicationContext.getSharedPreferences(qualifiedName, Context.MODE_PRIVATE)
    }

    private val statusChannel = Channel<Status>()

    protected var isEnabled: Boolean by ReadWriteStatusBoolean(false)

    private var lastErrorMessage: String by ReadWriteStatusString("")

    var turnedOnTime: Long by ReadWriteStatusLong(Long.MIN_VALUE)
        private set

    var lastTimeDataWritten: Long by ReadWriteStatusLong(Long.MIN_VALUE)

    var recordsUploaded: Long by ReadWriteStatusLong(0)

    val statusFlow = statusChannel.receiveAsFlow()

    fun getStatus(): Status = when {
        !isEnabled -> Status.Off
        isEnabled && lastErrorMessage.isBlank() -> Status.On
        else -> Status.Error(lastErrorMessage)
    }

    abstract val permissions: List<String>

    abstract val setupIntent: Intent?

    init {
        statusChannel.offer(getStatus())
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
    abstract fun getDescription(): Array<Description>

    abstract suspend fun count(): Long

    abstract suspend fun flush(entities: Collection<E>)

    abstract suspend fun list(limit: Long): Collection<E>

    /**
     * function start is called when
     * 1) a user changes the collector status into On, or
     * 2) the app restarts (e.g., rebooting, error...)
     */
    fun start() {
        if (!isAvailable()) {
            stop(CollectorError.turningOnRequestWhenUnavaiable(qualifiedName))
        }

        isEnabled = true

        lastErrorMessage = ""

        turnedOnTime = if (turnedOnTime > 0) {
            turnedOnTime
        } else {
            System.currentTimeMillis()
        }

        statusChannel.offer(getStatus())

        launch {
            onStart()

            put(
                DeviceEventEntity(
                    eventType = javaClass.simpleName.toString(),
                    extras = mapOf( "status" to "On")
                ).apply { this.timestamp = System.currentTimeMillis() }
            )
        }
    }

    /**
     * function stop is called only by a user's explicit interaction.
     */
    fun stop(throwable: Throwable? = null) {
        isEnabled = false

        turnedOnTime = Long.MIN_VALUE

        statusChannel.offer(getStatus())

        launch {
            put(
                DeviceEventEntity(
                    eventType = javaClass.simpleName.toString(),
                    extras = mapOf( "status" to "Off")
                ).apply { this.timestamp = System.currentTimeMillis() }
            )

            onStop()
            if (throwable != null) throw throwable
        }

        scope.cancel()
    }

    fun clear() {
        this::class.declaredMemberProperties.filterIsInstance<KProperty1<AbstractCollector<*>, *>>()
            .forEach {
                (it.getDelegate(this) as? ClearableStatusProperty)?.clear(this, it)
            }
    }

    fun <T : Any> fill(datum: T) = if (datum is AbstractEntity) {
        datum.apply {
            utcOffset = TimeZone.getDefault().rawOffset / 1000
            groupName = AuthRepository.groupName
            email = AuthRepository.email
            instanceId = AuthRepository.instanceId
            source = AuthRepository.source
            deviceManufacturer = AuthRepository.deviceManufacturer
            deviceModel = AuthRepository.deviceModel
            deviceVersion = AuthRepository.deviceVersion
            deviceOs = AuthRepository.deviceOs
            appId = AuthRepository.appId
            appVersion = AuthRepository.appVersion
        }
    } else {
        datum
    }

    suspend inline fun <reified T : Any> put(
        datum: T,
        isStatUpdates: Boolean = true
    ) {
        val entity = fill(datum)

        dataRepository.put(entity)
        EventBus.post(entity)
        Log.d(javaClass, entity)

        if (isStatUpdates) {
            val timestamp = (entity as? AbstractEntity)?.timestamp ?: System.currentTimeMillis()
            lastTimeDataWritten = timestamp.coerceAtLeast(lastTimeDataWritten)
        }
    }

    protected fun launch(block: suspend () -> Unit) = scope.launch(exceptionHandler) {
        block.invoke()
    }
}