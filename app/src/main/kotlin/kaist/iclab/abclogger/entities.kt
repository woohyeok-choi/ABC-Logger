package kaist.iclab.abclogger

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import io.objectbox.BoxStore
import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Transient
import io.objectbox.kotlin.boxFor
import kaist.iclab.abclogger.collector.MyObjectBox
import java.io.File
import java.util.*

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

fun <T : Base> T.fillBaseInfo(timeMillis: Long): T {
    timestamp = timeMillis
    utcOffset = TimeZone.getDefault().rawOffset.toFloat() / (1000 * 60 * 60)
    subjectEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    participationTime = GeneralPrefs.participationTime
    deviceInfo = "${Build.MANUFACTURER}-${Build.MODEL}-${Build.VERSION.RELEASE}"
    isUploaded = false

    return this
}

























