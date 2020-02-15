package kaist.iclab.abclogger

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import kaist.iclab.abclogger.collector.Base
import kaist.iclab.abclogger.collector.MyObjectBox
import kaist.iclab.abclogger.commons.firstNotNullResult
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.concurrent.atomic.AtomicReference

object ObjBox {
    val boxStore: AtomicReference<BoxStore> = AtomicReference()

    fun bind(context: Context) {
        val dbName = "${BuildConfig.DB_NAME}-${Prefs.dbVersion}"
        val newStore = buildStore(context, dbName)
        boxStore.set(newStore)
    }

    fun flush(context: Context) {
        val dbVersion = Prefs.dbVersion
        val dbName = "${BuildConfig.DB_NAME}-$dbVersion"
        val newStore = buildStore(context, dbName)
        val oldStore = boxStore.getAndSet(newStore)

        Prefs.dbVersion += 1

        oldStore?.close()
        oldStore?.deleteAllFiles()
    }

    private fun buildStore(context: Context, dbName: String): BoxStore {
        return (1..500).firstNotNullResult { multiple ->
            val size = BuildConfig.DB_MAX_SIZE * multiple
            try {
                val tempStore = MyObjectBox.builder()
                        .androidContext(context.applicationContext)
                        .maxSizeInKByte(size) //3 GB
                        .name(dbName)
                        .build()

                Prefs.maxDbSize = size

                tempStore
            } catch (e: Exception) {
                null
            }
        } ?: throw RuntimeException("DB size is too large!!")
    }

    @JvmStatic
    fun size(context: Context): Long {
        val baseDir = File(context.filesDir, "objectbox")
        return FileUtils.sizeOfDirectory(baseDir)
    }

    @JvmStatic
    fun maxSizeInBytes() = Prefs.maxDbSize * 1000L

    inline fun <reified T : Base> boxFor() = try {
        boxStore.get()?.boxFor<T>()
    } catch (e: Exception) {
        null
    }

    inline fun <reified T : Base> put(entity: T?): Long {
        entity ?: return 0
        if (boxStore.get()?.isClosed != false) return 0

        return try {
            boxFor<T>()?.put(entity)
        } catch (e: Exception) {
            null
        } ?: 0
    }

    inline fun <reified T : Base> put(entity: Collection<T>?) {
        entity ?: return
        if (boxStore.get()?.isClosed != false) return

        try {
            boxFor<T>()?.put(entity)
        } catch (e: Exception) {
        }
    }

    inline fun <reified T : Base> query() = boxFor<T>()?.query()

    inline fun <reified T : Base> remove(entity: T?) {
        entity ?: return
        if (boxStore.get()?.isClosed != false) return

        try {
            boxFor<T>()?.remove(entity)
        } catch (e: Exception) {
        }
    }

    inline fun <reified T : Base> remove(entity: Collection<T>?) {
        entity ?: return
        if (boxStore.get()?.isClosed != false) return

        try {
            boxFor<T>()?.remove(entity)
        } catch (e: Exception) {
        }
    }

    inline fun <reified T : Base> get(id: Long?): T? {
        id ?: return null
        if (boxStore.get()?.isClosed != false) return null
        return try {
            boxFor<T>()?.get(id)
        } catch (e: Exception) {
            null
        }
    }
}





