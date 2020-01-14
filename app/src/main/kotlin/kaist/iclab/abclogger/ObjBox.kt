package kaist.iclab.abclogger

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import java.io.File


object ObjBox {
    lateinit var boxStore : BoxStore

    fun bind(context: Context) {
        boxStore = MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .maxSizeInKByte(BuildConfig.DB_MAX_SIZE) //3 GB
                .name(BuildConfig.DB_NAME)
                .build()
    }

    fun size(context: Context) : Long {
        val baseDir = File(context.filesDir, "objectbox")
        val dbDir = File(baseDir, BuildConfig.DB_NAME)
        return dbDir.listFiles()?.sumByLong { it.length() } ?: 0
    }

    fun maxSizeInBytes() = BuildConfig.DB_MAX_SIZE * 1000L

    inline fun <reified T> boxFor() = boxStore.boxFor<T>()

    private fun toReadableFileSize(bytes: Long) {

    }
}