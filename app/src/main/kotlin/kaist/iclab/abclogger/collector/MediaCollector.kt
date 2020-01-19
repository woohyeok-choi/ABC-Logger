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
                        lastTime = CollectorPrefs.lastAccessTimeInternalPhoto
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "image/*"
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                CollectorPrefs.lastAccessTimeInternalPhoto = timestamps.max() ?: CollectorPrefs.lastAccessTimeInternalPhoto
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
                        lastTime = CollectorPrefs.lastAccessTimeInternalVideo
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "video/*"
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                CollectorPrefs.lastAccessTimeInternalVideo = timestamps.max() ?: CollectorPrefs.lastAccessTimeInternalVideo
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
                        lastTime = CollectorPrefs.lastAccessTimeExternalPhoto
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "image/*"
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                CollectorPrefs.lastAccessTimeExternalPhoto = timestamps.max() ?: CollectorPrefs.lastAccessTimeExternalPhoto
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
                        lastTime = CollectorPrefs.lastAccessTimeExternalVideo
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "video/*"
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                CollectorPrefs.lastAccessTimeExternalVideo = timestamps.max() ?: CollectorPrefs.lastAccessTimeExternalVideo
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

    override fun checkAvailability(): Boolean = context.checkPermission(requiredPermissions)

    override fun handleActivityResult(resultCode: Int, intent: Intent?) { }

    override val requiredPermissions: List<String>
        get() = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    override val newIntentForSetUp: Intent?
        get() = null

}