package kaist.iclab.abclogger.collector

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.ABCException
import kaist.iclab.abclogger.commons.Notifications
import kaist.iclab.abclogger.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor


/**
 * Default interface for data collector
 */
abstract class BaseCollector<T : BaseStatus>(private val context: Context) : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val preference: SharedPreferences by lazy {
        context.getSharedPreferences(BuildConfig.PREF_NAME, Context.MODE_PRIVATE)
    }

    private val serializer: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    abstract val clazz: KClass<T>

    abstract val name: String

    abstract val description: String

    /**
     * Check whether a given collector can operate (e.g., permissions).
     * If not available (even after started), it will be stopped.
     */
    abstract suspend fun checkAvailability(): Boolean

    /**
     * List of permissions (Manifest.permissions.XXX) for this collector.
     */
    abstract val requiredPermissions: List<String>

    /**
     * Intent to make this collector available;
     * for example, to collect notifications, ABC Logger needs a user's manual setting.
     * This function is used to start an activity for the setting.
     */
    abstract val newIntentForSetUp: Intent?

    /**
     * Define operations when a user requests for starting this collector.
     * This function should be called in non-UI thread.
     */
    abstract suspend fun onStart()

    /**
     * Define operations when a user requests for stopping this collector.
     * This function should be called in non-UI thread.
     */
    abstract suspend fun onStop()


    suspend fun setStatus(newStatus: T) {
        val oldStatus = getStatus()
        val mergedStatus = merge(oldStatus, newStatus)

        try {
            val json = serializer.adapter(clazz.java).toJson(mergedStatus)
                    ?: throw Exception("Failed to serialize")
            preference.edit { putString(clazz.java.name, json) }
        } catch (e: Exception) {

        }
    }

    suspend fun getStatus(): T? =
            try {
                preference.getString(clazz.java.name, null)?.let { json ->
                    withContext(Dispatchers.IO) {
                        serializer.adapter(clazz.java).fromJson(json)
                    }
                }
            } catch (e: Exception) {
                null
            }

    fun start() = launch {
        try {
            onStart()
            setStatus(buildDefaultStatus(true))
        } catch (e: Exception) {
            e.printStackTrace()
            notifyError(e)
        }
    }

    fun stop() = launch {
        try {
            onStop()
            setStatus(buildDefaultStatus(false))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clear() {
        try {
            preference.edit {
                remove(clazz.java.name)
            }
        } catch (e: Exception) {

        }
    }

    fun notifyError(throwable: Throwable?) {
        val ntf = Notifications.build(
                context = context,
                channelId = Notifications.CHANNEL_ID_REQUIRE_SETTING,
                title = context.getString(R.string.ntf_title_collector_runtime_error, name),
                bigText = context.getString(R.string.ntf_text_collector_runtime_error,
                        name, ABCException.wrap(throwable).toString(context), name
                ),
                intent = PendingIntent.getActivity(context, REQUEST_CODE_MAIN_ACTIVITY, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        )
        Notifications.notify(context, Notifications.ID_REQUIRE_SETTING, ntf)
    }

    private fun buildDefaultStatus(hasStarted: Boolean): T {
        val primaryConstructor = clazz.primaryConstructor!!
        val hasStartedParameter = primaryConstructor.parameters.find { it.name == "hasStarted" }!!
        val lastTimeParameter = primaryConstructor.parameters.find { it.name == "lastTime" }!!
        val args = mapOf(
                hasStartedParameter to hasStarted,
                lastTimeParameter to null
        )
        return primaryConstructor.callBy(args)
    }

    private fun merge(one: T?, other: T): T {
        if (one == null) return other

        val nameToProperty = clazz.memberProperties.associateBy { it.name }
        val primaryConstructor = other::class.primaryConstructor!!
        val args = primaryConstructor.parameters.associateWith { parameter ->
            val property = nameToProperty[parameter.name]!!
            return@associateWith property.get(other) ?: property.get(one)
        }
        return primaryConstructor.callBy(args)
    }

    companion object {
        const val REQUEST_CODE_MAIN_ACTIVITY = 0xf1
    }
}