package kaist.iclab.abclogger.collector.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.commons.checkPermission
import kaist.iclab.abclogger.commons.safeRegisterContentObserver
import kaist.iclab.abclogger.commons.safeUnregisterContentObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class MediaCollector(private val context: Context) : BaseCollector<MediaCollector.Status>(context) {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null,
                      val lastTimeAccessedInternalPhoto: Long = 0,
                      val lastTimeAccessedInternalVideo: Long = 0,
                      val lastTimeAccessedExternalPhoto: Long = 0,
                      val lastTimeAccessedExternalVideo: Long = 0) : BaseStatus() {
        override fun info(): Map<String, Any> = mapOf()
    }

    override val clazz: KClass<Status> = Status::class

    override val name: String = context.getString(R.string.data_name_media)

    override val description: String = context.getString(R.string.data_desc_media)

    override val requiredPermissions: List<String> = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    override val newIntentForSetUp: Intent? = null

    override suspend fun checkAvailability(): Boolean = context.checkPermission(requiredPermissions)

    override suspend fun onStart() {
        context.contentResolver.safeUnregisterContentObserver(internalPhotoObserver)
        context.contentResolver.safeUnregisterContentObserver(internalVideoObserver)
        context.contentResolver.safeUnregisterContentObserver(externalPhotoObserver)
        context.contentResolver.safeUnregisterContentObserver(externalVideoObserver)

        context.contentResolver.safeRegisterContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, internalPhotoObserver)
        context.contentResolver.safeRegisterContentObserver(MediaStore.Video.Media.INTERNAL_CONTENT_URI, true, internalVideoObserver)
        context.contentResolver.safeRegisterContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, externalPhotoObserver)
        context.contentResolver.safeRegisterContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, externalVideoObserver)
    }

    override suspend fun onStop() {
        context.contentResolver.safeUnregisterContentObserver(internalPhotoObserver)
        context.contentResolver.safeUnregisterContentObserver(internalVideoObserver)
        context.contentResolver.safeUnregisterContentObserver(externalPhotoObserver)
        context.contentResolver.safeUnregisterContentObserver(externalVideoObserver)
    }

    private fun handleMediaRetrieval(type: Int) = launch {
        val curTime = System.currentTimeMillis()
        val lastTimeAccessed = getStatus()?.let { status ->
            when (type) {
                TYPE_INTERNAL_PHOTO -> status.lastTimeAccessedInternalPhoto
                TYPE_INTERNAL_VIDEO -> status.lastTimeAccessedInternalVideo
                TYPE_EXTERNAL_PHOTO -> status.lastTimeAccessedExternalPhoto
                TYPE_EXTERNAL_VIDEO -> status.lastTimeAccessedExternalVideo
                else -> null
            }
        } ?: curTime - TimeUnit.DAYS.toMillis(1)

        val cursor = when (type) {
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
        } ?: return@launch

        val timestamps = mutableListOf<Long>()
        val entities = cursor.use { cs ->
            val data: MutableList<MediaEntity> = mutableListOf()
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
            when (type) {
                TYPE_INTERNAL_PHOTO -> setStatus(Status(lastTimeAccessedInternalPhoto = timestamp))
                TYPE_INTERNAL_VIDEO -> setStatus(Status(lastTimeAccessedInternalPhoto = timestamp))
                TYPE_EXTERNAL_PHOTO -> setStatus(Status(lastTimeAccessedInternalPhoto = timestamp))
                TYPE_EXTERNAL_VIDEO -> setStatus(Status(lastTimeAccessedInternalPhoto = timestamp))
            }
        }
    }

    private val handler : Handler
        get() {
            val thread = HandlerThread(this::class.java.name)
            thread.start()
            return Handler(thread.looper)
        }

    private val internalPhotoObserver: ContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handleMediaRetrieval(TYPE_INTERNAL_PHOTO)
        }
    }

    private val internalVideoObserver: ContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handleMediaRetrieval(TYPE_INTERNAL_VIDEO)
        }
    }

    private val externalPhotoObserver: ContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handleMediaRetrieval(TYPE_EXTERNAL_PHOTO)
        }
    }

    private val externalVideoObserver: ContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handleMediaRetrieval(TYPE_EXTERNAL_VIDEO)
        }
    }

    companion object {
        private const val TYPE_INTERNAL_PHOTO = 0x01
        private const val TYPE_INTERNAL_VIDEO = 0x02
        private const val TYPE_EXTERNAL_PHOTO = 0x03
        private const val TYPE_EXTERNAL_VIDEO = 0x04
    }
}