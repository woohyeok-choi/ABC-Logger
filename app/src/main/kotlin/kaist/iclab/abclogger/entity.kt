package kaist.iclab.abclogger

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import io.objectbox.BoxStore
import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.kotlin.boxFor
import kaist.iclab.abclogger.collector.MyObjectBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.isSymlink
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicReference


object ObjBox {
    val boxStore : AtomicReference<BoxStore> = AtomicReference()

    private fun buildStore(context: Context) : BoxStore {
        val store = MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .maxSizeInKByte(BuildConfig.DB_MAX_SIZE) //3 GB
                .name("${BuildConfig.DB_NAME}-${GeneralPrefs.dbVersion}")
                .build()
        GeneralPrefs.dbVersion += 1
        return store
    }

    fun boxStore() = boxStore.get()

    fun bind(context: Context) {
        boxStore.set(buildStore(context))
    }

    fun flush(context: Context) = GlobalScope.launch(Dispatchers.IO) {
        val oldStore = boxStore.getAndSet(buildStore(context)) ?: return@launch
        oldStore.close()
        oldStore.deleteAllFiles()
    }

    fun size(context: Context) : Long {
        val baseDir = File(context.filesDir, "objectbox")
        return FileUtils.sizeOfDirectory(baseDir)
    }

    fun maxSizeInBytes() = BuildConfig.DB_MAX_SIZE * 1000L

    inline fun <reified T> boxFor() = boxStore.get()?.boxFor<T>()

    inline fun <reified T : Base> putSync(entity: T?) : Long {
        entity ?: return -1L
        if(boxStore.get()?.isClosed != false) return -1

        if (BuildConfig.DEBUG) AppLog.d(any = entity)
        return boxFor<T>()?.put(entity) ?: -1
    }

    inline fun <reified T : Base> putSync(entities: Collection<T>?) {
        if(entities.isNullOrEmpty()) return
        if(boxStore.get()?.isClosed != false) return

        if (BuildConfig.DEBUG) AppLog.d(any = entities)
        boxFor<T>()?.put(entities)
    }

    inline fun <reified T : Base> put(entity: T?) = GlobalScope.launch (Dispatchers.IO) {
        entity ?: return@launch
        if(boxStore.get()?.isClosed != false) return@launch

        if (BuildConfig.DEBUG) AppLog.d(any = entity)
        boxFor<T>()?.put(entity) ?: -1
    }

    inline fun <reified T : Base> put(entities: Collection<T>?) = GlobalScope.launch (Dispatchers.IO) {
        if(entities.isNullOrEmpty()) return@launch
        if(boxStore.get()?.isClosed != false) return@launch

        if (BuildConfig.DEBUG) AppLog.d(any = entities)
        boxFor<T>()?.put(entities)
    }
}

@BaseEntity
abstract class Base(
        @Id var id: Long = 0,
        var timestamp: Long = -1,
        var utcOffset: Float = Float.MIN_VALUE,
        var subjectEmail: String = "",
        var participationTime: Long = -1,
        var deviceInfo: String = "",
        @Index var isUploaded: Boolean = false
)

fun <T : Base> T.fill(timeMillis: Long) : T {
    timestamp = timeMillis
    utcOffset = TimeZone.getDefault().rawOffset.toFloat() / (1000 * 60 * 60)
    subjectEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    participationTime = GeneralPrefs.participationTime
    deviceInfo = "${Build.MANUFACTURER}-${Build.MODEL}-${Build.VERSION.RELEASE}"
    isUploaded = false

    return this
}


