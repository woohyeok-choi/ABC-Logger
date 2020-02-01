package kaist.iclab.abclogger

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import kaist.iclab.abclogger.collector.MyObjectBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.concurrent.atomic.AtomicReference

object ObjBox {
    val boxStore: AtomicReference<BoxStore> = AtomicReference()

    private fun notifyFlushProgress(context: Context) {
        val ntf = Notifications.build(
                context = context,
                channelId = Notifications.CHANNEL_ID_PROGRESS,
                title = context.getString(R.string.ntf_title_flush),
                text = context.getString(R.string.ntf_text_flush),
                progress = 0,
                indeterminate = true
        )
        NotificationManagerCompat.from(context).notify(Notifications.ID_FLUSH_PROGRESS, ntf)
    }

    private fun notifyFlushComplete(context: Context) {
        val ntf = Notifications.build(
                context = context,
                channelId = Notifications.CHANNEL_ID_PROGRESS,
                title = context.getString(R.string.ntf_title_flush),
                text = context.getString(R.string.ntf_text_flush_complete)
        )
        NotificationManagerCompat.from(context).notify(Notifications.ID_FLUSH_PROGRESS, ntf)
    }

    private fun notifyFlushError(context: Context, throwable: Throwable?) {
        val ntf = Notifications.build(
                context = context,
                channelId = Notifications.CHANNEL_ID_PROGRESS,
                title = context.getString(R.string.ntf_title_flush),
                text = listOfNotNull(
                        context.getString(R.string.ntf_text_flush_failed),
                        ABCException.wrap(throwable).toString(context)
                ).joinToString(": ")
        )
        NotificationManagerCompat.from(context).notify(Notifications.ID_FLUSH_PROGRESS, ntf)
    }

    private suspend fun buildStore(context: Context): BoxStore = withContext(Dispatchers.IO) {
        return@withContext (1..500).firstNotNullResult { multiple ->
            try {
                val tempStore = MyObjectBox.builder()
                        .androidContext(context.applicationContext)
                        .maxSizeInKByte(BuildConfig.DB_MAX_SIZE * multiple) //3 GB
                        .name("${BuildConfig.DB_NAME}-${Prefs.dbVersion}")
                        .build()
                Prefs.maxDbSize = BuildConfig.DB_MAX_SIZE * multiple

                tempStore
            } catch (e: Exception) {
                null
            }
        } ?: throw RuntimeException("DB size is too large!!")
    }

    suspend fun bind(context: Context) = withContext(Dispatchers.IO) {
        boxStore.set(buildStore(context))
    }

    suspend fun flush(context: Context, showProgress: Boolean = false) = withContext(Dispatchers.IO) {
        if (showProgress) notifyFlushProgress(context)
        try {
            Prefs.dbVersion += 1
            val oldStore = boxStore.getAndSet(buildStore(context))
            oldStore?.close()
            oldStore?.deleteAllFiles()
            if (showProgress) notifyFlushComplete(context)
        } catch (e: Exception) {
            if(showProgress) notifyFlushError(context, e)
        }
    }

    fun size(context: Context): Long {
        val baseDir = File(context.filesDir, "objectbox")
        return FileUtils.sizeOfDirectory(baseDir)
    }

    fun maxSizeInBytes() = Prefs.maxDbSize * 1000L

    inline fun <reified T : Any> boxFor() = try { boxStore.get()?.boxFor<T>() } catch (e: Exception) { null }

    fun <T : Any> boxFor(clazz: Class<T>) = try { boxStore.get()?.boxFor(clazz) } catch (e: Exception) { null }

    inline fun <reified T : Any> putSync(entity: T?): Long {
        entity ?: return -1L
        if (boxStore.get()?.isClosed != false) return -1

        if (BuildConfig.DEBUG) AppLog.d(T::class.java.name, entity)
        return try { boxFor<T>()?.put(entity) } catch (e: Exception) { null } ?: -1
    }

    inline fun <reified T : Any> putSync(entities: Collection<T>?) {
        if (entities.isNullOrEmpty()) return
        if (boxStore.get()?.isClosed != false) return

        if (BuildConfig.DEBUG) AppLog.d(T::class.java.name, entities)
        try { boxFor<T>()?.put(entities) } catch (e: Exception) { }
    }

    suspend inline fun <reified T : Any> put(entity: T?): Long = withContext<Long>(Dispatchers.IO) {
        entity ?: return@withContext -1
        if (boxStore.get()?.isClosed != false) return@withContext -1

        if (BuildConfig.DEBUG) AppLog.d(T::class.java.name, entity)
        return@withContext try { boxFor<T>()?.put(entity) } catch (e: Exception) { null } ?: -1
    }

    suspend inline fun <reified T : Any> put(entities: Collection<T>?) = withContext(Dispatchers.IO) {
        if (entities.isNullOrEmpty()) return@withContext
        if (boxStore.get()?.isClosed != false) return@withContext

        if (BuildConfig.DEBUG) AppLog.d(T::class.java.name, entities)
        try { boxFor<T>()?.put(entities) } catch (e: Exception) { }
    }
}





