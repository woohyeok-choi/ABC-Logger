package kaist.iclab.abclogger.collector

import android.Manifest
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.provider.MediaStore
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseCollector

class MediaCollector (val context: Context) : BaseCollector {

    private val internalPhotoObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                val timestamps = mutableListOf<Long>()

                getRecentContents(
                        contentResolver = context.contentResolver,
                        uri = MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                        timeColumn = MediaStore.Images.ImageColumns.DATE_ADDED,
                        columns = arrayOf(
                                MediaStore.Images.ImageColumns.DATE_ADDED,
                                MediaStore.Images.ImageColumns.MIME_TYPE
                        ),
                        lastTime = SharedPrefs.lastAccessTimeInternalPhoto
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "image/*"
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                SharedPrefs.lastAccessTimeInternalPhoto = timestamps.max() ?: SharedPrefs.lastAccessTimeInternalPhoto
            }
        }
    }

    private val internalVideoObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                val timestamps = mutableListOf<Long>()
                MediaStore.MediaColumns.DATE_ADDED
                getRecentContents(
                        contentResolver = context.contentResolver,
                        uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                        timeColumn =  MediaStore.Video.VideoColumns.DATE_ADDED,
                        columns = arrayOf(
                                MediaStore.Video.VideoColumns.DATE_ADDED,
                                MediaStore.Video.VideoColumns.MIME_TYPE
                        ),
                        lastTime = SharedPrefs.lastAccessTimeInternalVideo
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "video/*"
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                SharedPrefs.lastAccessTimeInternalVideo = timestamps.max() ?: SharedPrefs.lastAccessTimeInternalVideo
            }
        }
    }

    private val externalPhotoObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)

                val timestamps = mutableListOf<Long>()

                getRecentContents(
                        contentResolver = context.contentResolver,
                        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        timeColumn = MediaStore.Images.ImageColumns.DATE_ADDED,
                        columns = arrayOf(
                                MediaStore.Images.ImageColumns.DATE_ADDED,
                                MediaStore.Images.ImageColumns.MIME_TYPE
                        ),
                        lastTime = SharedPrefs.lastAccessTimeExternalPhoto
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "image/*"
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                SharedPrefs.lastAccessTimeExternalPhoto = timestamps.max() ?: SharedPrefs.lastAccessTimeExternalPhoto
            }
        }
    }

    private val externalVideoObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                val timestamps = mutableListOf<Long>()

                getRecentContents(
                        contentResolver = context.contentResolver,
                        uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        timeColumn =  MediaStore.Video.VideoColumns.DATE_ADDED,
                        columns = arrayOf(
                                MediaStore.Video.VideoColumns.DATE_ADDED,
                                MediaStore.Video.VideoColumns.MIME_TYPE
                        ),
                        lastTime = SharedPrefs.lastAccessTimeExternalVideo
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "video/*"
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                SharedPrefs.lastAccessTimeExternalVideo = timestamps.max() ?: SharedPrefs.lastAccessTimeExternalVideo
            }
        }
    }
    override fun onStart() {
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, internalPhotoObserver)
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.INTERNAL_CONTENT_URI, true, internalVideoObserver)
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, externalPhotoObserver)
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, externalVideoObserver)
    }

    override fun onStop() {
        context.contentResolver.unregisterContentObserver(internalPhotoObserver)
        context.contentResolver.unregisterContentObserver(internalVideoObserver)
        context.contentResolver.unregisterContentObserver(externalPhotoObserver)
        context.contentResolver.unregisterContentObserver(externalVideoObserver)
    }

    override fun checkAvailability(): Boolean = Utils.checkPermissionAtRuntime(context, requiredPermissions)

    override fun handleActivityResult(resultCode: Int, intent: Intent?) { }

    override val requiredPermissions: List<String>
        get() = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    override val newIntentForSetUp: Intent?
        get() = null

}