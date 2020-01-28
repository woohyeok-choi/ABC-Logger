package kaist.iclab.abclogger.collector.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MediaCollector(val context: Context) : BaseCollector {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null,
                      val lastTimeAccessedInternalPhoto: Long = 0,
                      val lastTimeAccessedInternalVideo: Long = 0,
                      val lastTimeAccessedExternalPhoto: Long = 0,
                      val lastTimeAccessedExternalVideo: Long = 0) : BaseStatus() {
        override fun info(): String = ""
    }

    private suspend fun handleObserver(type: Int) {
        val curTime = System.currentTimeMillis()
        val lastTimeAccessed = (getStatus() as? Status)?.let { status ->
            when(type) {
                TYPE_INTERNAL_PHOTO -> status.lastTimeAccessedInternalPhoto
                TYPE_INTERNAL_VIDEO -> status.lastTimeAccessedInternalVideo
                TYPE_EXTERNAL_PHOTO -> status.lastTimeAccessedExternalPhoto
                TYPE_EXTERNAL_VIDEO -> status.lastTimeAccessedExternalVideo
                else -> null
            }
        } ?: curTime - TimeUnit.DAYS.toMillis(1)

        val cursor = when(type) {
            TYPE_INTERNAL_PHOTO -> getRecentContents(
                    contentResolver = context.contentResolver,
                    uri = MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    timeColumn = MediaStore.Images.ImageColumns.DATE_ADDED,
                    columns = arrayOf(
                            MediaStore.Images.ImageColumns.DATE_ADDED,
                            MediaStore.Images.ImageColumns.MIME_TYPE
                    ),
                    lastTime = lastTimeAccessed
            )
            TYPE_INTERNAL_VIDEO -> getRecentContents(
                    contentResolver = context.contentResolver,
                    uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                    timeColumn = MediaStore.Video.VideoColumns.DATE_ADDED,
                    columns = arrayOf(
                            MediaStore.Video.VideoColumns.DATE_ADDED,
                            MediaStore.Video.VideoColumns.MIME_TYPE
                    ),
                    lastTime = lastTimeAccessed
            )
            TYPE_EXTERNAL_PHOTO -> getRecentContents(
                    contentResolver = context.contentResolver,
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    timeColumn = MediaStore.Images.ImageColumns.DATE_ADDED,
                    columns = arrayOf(
                            MediaStore.Images.ImageColumns.DATE_ADDED,
                            MediaStore.Images.ImageColumns.MIME_TYPE
                    ),
                    lastTime = lastTimeAccessed
            )
            TYPE_EXTERNAL_VIDEO -> getRecentContents(
                    contentResolver = context.contentResolver,
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    timeColumn = MediaStore.Video.VideoColumns.DATE_ADDED,
                    columns = arrayOf(
                            MediaStore.Video.VideoColumns.DATE_ADDED,
                            MediaStore.Video.VideoColumns.MIME_TYPE
                    ),
                    lastTime = lastTimeAccessed
            )
            else -> null
        } ?: return

        val timestamps = mutableListOf<Long>()
        val entities = cursor.use { cs ->
            val data : MutableList<MediaEntity> = mutableListOf()
            while (cs.moveToNext()) {
                val timestamp = cs.getLongOrNull(0) ?: 0
                val defaultMimeType = if (type == TYPE_INTERNAL_PHOTO || type == TYPE_EXTERNAL_PHOTO) {
                    "image/*"
                } else {
                    "video/*"
                }
                val entity = MediaEntity(
                        mimeType = cs.getStringOrNull(1) ?: defaultMimeType
                ).fill(toMillis(timestamp = timestamp))

                timestamps.add(timestamp)
                data.add(entity)
            }
            return@use data
        }

        ObjBox.put(entities)
        setStatus(Status(lastTime = curTime))

        timestamps.max()?.also { timestamp ->
            when(type) {
                TYPE_INTERNAL_PHOTO -> setStatus(Status(lastTimeAccessedInternalPhoto = timestamp))
                TYPE_INTERNAL_VIDEO -> setStatus(Status(lastTimeAccessedInternalPhoto = timestamp))
                TYPE_EXTERNAL_PHOTO -> setStatus(Status(lastTimeAccessedInternalPhoto = timestamp))
                TYPE_EXTERNAL_VIDEO -> setStatus(Status(lastTimeAccessedInternalPhoto = timestamp))
            }
        }
    }

    private val internalPhotoObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                GlobalScope.launch { handleObserver(TYPE_INTERNAL_PHOTO) }
            }
        }
    }

    private val internalVideoObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                GlobalScope.launch { handleObserver(TYPE_INTERNAL_VIDEO) }
            }
        }
    }

    private val externalPhotoObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                GlobalScope.launch { handleObserver(TYPE_EXTERNAL_PHOTO) }
            }
        }
    }

    private val externalVideoObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                GlobalScope.launch { handleObserver(TYPE_EXTERNAL_VIDEO) }
            }
        }
    }

    override suspend fun onStart() {
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, internalPhotoObserver)
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.INTERNAL_CONTENT_URI, true, internalVideoObserver)
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, externalPhotoObserver)
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, externalVideoObserver)
    }

    override suspend fun onStop() {
        context.contentResolver.unregisterContentObserver(internalPhotoObserver)
        context.contentResolver.unregisterContentObserver(internalVideoObserver)
        context.contentResolver.unregisterContentObserver(externalPhotoObserver)
        context.contentResolver.unregisterContentObserver(externalVideoObserver)
    }

    override suspend fun checkAvailability(): Boolean = context.checkPermission(requiredPermissions)

    override val requiredPermissions: List<String>
        get() = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    override val newIntentForSetUp: Intent?
        get() = null

    companion object {
        private const val TYPE_INTERNAL_PHOTO = 0x01
        private const val TYPE_INTERNAL_VIDEO = 0x02
        private const val TYPE_EXTERNAL_PHOTO = 0x03
        private const val TYPE_EXTERNAL_VIDEO = 0x04
    }
}