package kaist.iclab.abclogger.collector.media

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.provider.MediaStore
import androidx.core.content.getSystemService
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.commons.atLeastPositive
import kaist.iclab.abclogger.commons.safeRegisterReceiver
import kaist.iclab.abclogger.commons.safeUnregisterReceiver
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.core.collector.*
import java.util.concurrent.TimeUnit

class MediaCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<MediaEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    override val permissions: List<String> = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override val setupIntent: Intent? = null

    private var lastTimeInternalVideoWritten by ReadWriteStatusLong(Long.MIN_VALUE)
    private var lastTimeExternalVideoWritten by ReadWriteStatusLong(Long.MIN_VALUE)
    private var lastTimeInternalPhotoWritten by ReadWriteStatusLong(Long.MIN_VALUE)
    private var lastTimeExternalPhotoWritten by ReadWriteStatusLong(Long.MIN_VALUE)

    private val intent by lazy {
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MEDIA_SCAN_REQUEST,
            Intent(ACTION_MEDIA_SCAN_REQUEST),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val alarmManager by lazy {
        context.getSystemService<AlarmManager>()!!
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleMediaScanRequest()
        }
    }

    private val contentResolver by lazy { context.applicationContext.contentResolver }

    override fun isAvailable(): Boolean = true

    override fun getDescription(): Array<Description> = arrayOf(
        R.string.collector_media_info_internal_photo_written with
                formatDateTime(context, lastTimeInternalPhotoWritten),
        R.string.collector_media_info_external_photo_written with
                formatDateTime(context, lastTimeExternalPhotoWritten),
        R.string.collector_media_info_internal_video_written with
                formatDateTime(context, lastTimeInternalVideoWritten),
        R.string.collector_media_info_external_video_written with
                formatDateTime(context, lastTimeExternalVideoWritten)
    )

    override suspend fun onStart() {
        val filter = IntentFilter().apply {
            addAction(ACTION_MEDIA_SCAN_REQUEST)
        }
        context.safeRegisterReceiver(receiver, filter)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20),
            TimeUnit.MINUTES.toMillis(30),
            intent
        )

    }

    override suspend fun onStop() {
        context.safeUnregisterReceiver(receiver)

        alarmManager.cancel(intent)
    }

    override suspend fun count(): Long = dataRepository.count<MediaEntity>()

    override suspend fun flush(entities: Collection<MediaEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<MediaEntity> = dataRepository.find(0, limit)

    private fun handleMediaScanRequest() = launch {
        val toTime = System.currentTimeMillis()

        val fromTimeInternalPhoto = atLeastPositive(
            least = toTime - TimeUnit.HOURS.toMillis(12),
            value = lastTimeInternalPhotoWritten
        )
        val fromTimeExternalPhoto = atLeastPositive(
            least = toTime - TimeUnit.HOURS.toMillis(12),
            value = lastTimeExternalPhotoWritten
        )
        val fromTimeInternalVideo = atLeastPositive(
            least = toTime - TimeUnit.HOURS.toMillis(12),
            value = lastTimeInternalVideoWritten
        )
        val fromTimeExternalVideo = atLeastPositive(
            least = toTime - TimeUnit.HOURS.toMillis(12),
            value = lastTimeExternalVideoWritten
        )

        /**
         * Retrieve internal photo
         */
        val internalPhotos = getRecentContentsWithSeconds(
            contentResolver = contentResolver,
            uri = MediaStore.Images.Media.INTERNAL_CONTENT_URI,
            timeColumn = MediaStore.Images.Media.DATE_ADDED,
            columns = arrayOf(
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.MIME_TYPE
            ),
            lastTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(fromTimeInternalPhoto)
        ) { millis, cursor ->
            MediaEntity(
                mimeType = cursor.getStringOrNull(1) ?: "image/*"
            ).apply {
                timestamp = millis
            }
        }

        internalPhotos.forEach { put(it) }

        lastTimeInternalPhotoWritten = internalPhotos.maxOfOrNull {
            it.timestamp
        }?.coerceAtLeast(lastTimeInternalPhotoWritten) ?: lastTimeInternalPhotoWritten

        /**
         * Retrieve external photo
         */
        val externalPhotos = getRecentContentsWithSeconds(
            contentResolver = contentResolver,
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            timeColumn = MediaStore.Images.Media.DATE_ADDED,
            columns = arrayOf(
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.MIME_TYPE,
            ),
            lastTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(fromTimeExternalPhoto)
        ) { millis, cursor ->
            MediaEntity(
                mimeType = cursor.getStringOrNull(1) ?: "image/*"
            ).apply {
                timestamp = millis
            }
        }
        externalPhotos.forEach { put(it) }

        lastTimeExternalPhotoWritten = externalPhotos.maxOfOrNull {
            it.timestamp
        }?.coerceAtLeast(lastTimeExternalPhotoWritten) ?: lastTimeExternalPhotoWritten

        /**
         * Retrieve internal video
         */
        val internalVideos = getRecentContentsWithSeconds(
            contentResolver = contentResolver,
            uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI,
            timeColumn = MediaStore.Video.Media.DATE_ADDED,
            columns = arrayOf(
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.MIME_TYPE
            ),
            lastTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(fromTimeInternalVideo)
        ) { millis, cursor ->
            MediaEntity(
                mimeType = cursor.getStringOrNull(1) ?: "video/*"
            ).apply {
                timestamp = millis
            }
        }

        internalVideos.forEach { put(it) }

        lastTimeInternalVideoWritten = internalVideos.maxOfOrNull {
            it.timestamp
        }?.coerceAtLeast(lastTimeInternalVideoWritten) ?: lastTimeInternalVideoWritten

        /**
         * Retrieve external video
         */
        val externalVideos = getRecentContentsWithSeconds(
            contentResolver = contentResolver,
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            timeColumn = MediaStore.Video.Media.DATE_ADDED,
            columns = arrayOf(
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.MIME_TYPE
            ),
            lastTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(fromTimeExternalVideo)
        ) { millis, cursor ->
            MediaEntity(
                mimeType = cursor.getStringOrNull(1) ?: "video/*"
            ).apply {
                timestamp = millis
            }
        }

        externalVideos.forEach { put(it) }

        lastTimeExternalVideoWritten = externalVideos.maxOfOrNull {
            it.timestamp
        }?.coerceAtLeast(lastTimeExternalVideoWritten) ?: lastTimeExternalVideoWritten
    }

    companion object {
        private const val ACTION_MEDIA_SCAN_REQUEST =
            "${BuildConfig.APPLICATION_ID}.ACTION_MEDIA_SCAN_REQUEST"
        private const val REQUEST_CODE_MEDIA_SCAN_REQUEST = 0x12
    }
}