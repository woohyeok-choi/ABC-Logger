package kaist.iclab.abclogger.collector

import android.Manifest
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.provider.MediaStore
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.common.util.PermissionUtils
import kaist.iclab.abclogger.MediaEntity
import kaist.iclab.abclogger.fillBaseInfo

class MediaCollector (val context: Context) : BaseCollector {

    private val internalPhotoObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                val timestamps = mutableListOf<Long>()

                getRecentContents(
                        contentResolver = context.contentResolver,
                        uri = MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                        timeColumn = MediaStore.Images.ImageColumns.DATE_TAKEN,
                        columns = arrayOf(
                                MediaStore.Images.ImageColumns.DATE_TAKEN,
                                MediaStore.Images.ImageColumns.MIME_TYPE,
                                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME
                        ),
                        lastTime = SharedPrefs.lastInternalPhotoAccessTime
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "image/*",
                            bucketDisplay = cursor.getString(2)
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                SharedPrefs.lastInternalPhotoAccessTime = timestamps.max() ?: SharedPrefs.lastInternalPhotoAccessTime
            }
        }
    }

    private val internalVideoObserver: ContentObserver by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                val timestamps = mutableListOf<Long>()

                getRecentContents(
                        contentResolver = context.contentResolver,
                        uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                        timeColumn =  MediaStore.Video.VideoColumns.DATE_TAKEN,
                        columns = arrayOf(
                                MediaStore.Video.VideoColumns.DATE_TAKEN,
                                MediaStore.Video.VideoColumns.MIME_TYPE,
                                MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME
                        ),
                        lastTime = SharedPrefs.lastInternalVideoAccessTime
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "video/*",
                            bucketDisplay = cursor.getString(2)
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                SharedPrefs.lastInternalVideoAccessTime = timestamps.max() ?: SharedPrefs.lastInternalVideoAccessTime
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
                        timeColumn = MediaStore.Images.ImageColumns.DATE_TAKEN,
                        columns = arrayOf(
                                MediaStore.Images.ImageColumns.DATE_TAKEN,
                                MediaStore.Images.ImageColumns.MIME_TYPE,
                                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME
                        ),
                        lastTime = SharedPrefs.lastExternalPhotoAccessTime
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "image/*",
                            bucketDisplay = cursor.getString(2)
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                SharedPrefs.lastExternalPhotoAccessTime = timestamps.max() ?: SharedPrefs.lastExternalPhotoAccessTime
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
                        timeColumn =  MediaStore.Video.VideoColumns.DATE_TAKEN,
                        columns = arrayOf(
                                MediaStore.Video.VideoColumns.DATE_TAKEN,
                                MediaStore.Video.VideoColumns.MIME_TYPE,
                                MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME
                        ),
                        lastTime = SharedPrefs.lastExternalVideoAccessTime
                ) { cursor ->
                    val timestamp = cursor.getLong(0)
                    timestamps.add(timestamp)

                    MediaEntity(
                            mimeType = cursor.getString(1) ?: "video/*",
                            bucketDisplay = cursor.getString(2)
                    ).fillBaseInfo(toMillis(timestamp = timestamp))
                }?.run {
                    putEntity(this)
                }

                SharedPrefs.lastExternalVideoAccessTime = timestamps.max() ?: SharedPrefs.lastExternalVideoAccessTime
            }
        }
    }
    override fun start() {
        if(!SharedPrefs.isProvidedMediaGeneration || !checkAvailability()) return

        context.contentResolver.registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, internalPhotoObserver)
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.INTERNAL_CONTENT_URI, true, internalVideoObserver)
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, externalPhotoObserver)
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, externalVideoObserver)
    }

    override fun stop() {
        if(!SharedPrefs.isProvidedMediaGeneration || !checkAvailability()) return

        context.contentResolver.unregisterContentObserver(internalPhotoObserver)
        context.contentResolver.unregisterContentObserver(internalVideoObserver)
        context.contentResolver.unregisterContentObserver(externalPhotoObserver)
        context.contentResolver.unregisterContentObserver(externalVideoObserver)
    }

    override fun checkAvailability(): Boolean = PermissionUtils.checkPermissionAtRuntime(context, getRequiredPermissions())

    override fun getRequiredPermissions(): List<String> = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun newIntentForSetup(): Intent? = null
}