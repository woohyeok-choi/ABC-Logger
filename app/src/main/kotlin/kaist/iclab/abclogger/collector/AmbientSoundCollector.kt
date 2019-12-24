package kaist.iclab.abclogger.collector

import android.Manifest
import androidx.lifecycle.MutableLiveData
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.background.Status
import kaist.iclab.abclogger.common.util.PermissionUtils
import kaist.iclab.abclogger.common.util.Utils
import kaist.iclab.abclogger.data.entities.Record
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class AmbientSoundCollector(val context: Context) : BaseCollector {
    private var scheduledFuture: ScheduledFuture<*>? = null

    override fun startCollection(uuid: String, group: String, email: String) {
        if(scheduledFuture?.isDone == false) return

        status.postValue(Status.STARTED)
        scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            try {
                collect(uuid, group, email)
            } catch (e: Exception) {
                if (e is SecurityException) {
                    stopCollection()
                    status.postValue(Status.ABORTED(e))
                }
            }
        }, 0, 15, TimeUnit.MINUTES)
    }

    override fun stopCollection() {
        scheduledFuture?.cancel(true)
        status.postValue(Status.CANCELED)
    }

    private fun collect(uuid: String, group: String, email: String) {
        status.postValue(Status.RUNNING)

        if(!checkEnableToCollect(context)) throw SecurityException("Ambient sound is not granted to be collected.")

        val fileName = "${System.currentTimeMillis()}.record"
        val bufSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATES_IN_HZ, AUDIO_CHANNEL, AUDIO_ENCODING) * 2
        var recorder: AudioRecord? = null

        try {
            val file = File(context.getExternalFilesDir(null), fileName)
            recorder = AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_SAMPLE_RATES_IN_HZ, AUDIO_CHANNEL, AUDIO_ENCODING, bufSize)

            var retryCount = 0

            while (recorder.state != AudioRecord.STATE_INITIALIZED && retryCount < MAX_RETRIES) {
                SystemClock.sleep(1000)
                retryCount++
            }

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("Audio step1State not initialized.")
            }
            recorder.startRecording()

            FileOutputStream(file).use {
                var length = 0
                val startTime = SystemClock.uptimeMillis()
                val readBuffer = ShortArray(bufSize)
                val writeBuffer = ByteArray(bufSize * 2)

                while (SystemClock.uptimeMillis() - startTime <= AUDIO_RECORD_DURATION) {
                    val ret = recorder.read(readBuffer, 0, readBuffer.size)
                    if (ret > 0) {
                        val offset = shortArrayToByteArray(readBuffer, writeBuffer)
                        if(offset > 0) {
                            length += offset
                            it.write(writeBuffer)
                        }
                    }
                }

                if(length <= 0) throw IllegalStateException("Record file is empty.")

                val entity= Record(
                    sampleRate = AUDIO_SAMPLE_RATES_IN_HZ,
                    channelMask = "MONO",
                    encoding = "PCM16BIT",
                    path = file.absolutePath,
                    duration = AUDIO_RECORD_DURATION
                ).apply {
                    timestamp = System.currentTimeMillis()
                    utcOffset = Utils.utcOffsetInHour()
                    subjectEmail = email
                    experimentUuid = uuid
                    experimentGroup = group
                    isUploaded = false
                }
                App.boxFor<Record>().put(entity)
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${entity.timestamp}, subjectEmail = ${entity.subjectEmail}, experimentUuid = ${entity.experimentUuid}, " +
                    "experimentGroup = ${entity.experimentGroup}, entity = $entity)")

                /* Record - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */

                //MySQLiteLogger.writeStringData(context, entity.javaClass.simpleName, entity.timestamp, jsonEntity)
            }

            recorder.stop()
            recorder.release()

            /* SW EDIT */

            val external = Environment.getExternalStorageDirectory()

            val directory = File(external.absolutePath + "/ABC_Logger/record")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            if (external.canWrite()) {
                val src = FileInputStream(file).channel

                val outputFile = File(external, "/ABC_Logger/record/$uuid-$fileName")
                val dst = FileOutputStream(outputFile).channel

                dst.transferFrom(src, 0, src.size())

                src.close()
                dst.close()

                Log.d(TAG, ">>> Record File Exported.")
            } else {
                Log.d(TAG, "external write permission denied.")
            }
        } catch (e: Exception) {
            try {
                File(context.getExternalFilesDir(null), fileName).let { if(it.exists()) it.delete() }
            } catch (ee: Exception) { }
        } finally {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (e: Exception) { }
        }
    }

    private fun shortArrayToByteArray(shortArray: ShortArray, byteArray: ByteArray, isBigEndian: Boolean = true): Int {
        if (shortArray.size * 2 > byteArray.size) {
            return -1
        }
        var offset = 0
        if (isBigEndian) {
            for (i in 0 until shortArray.size) {
                offset = i * 2 + 1
                byteArray[i * 2] = shortArray[i].toInt().and(0xFF00).shr(8).toByte()
                byteArray[i * 2 + 1] = shortArray[i].toInt().and(0x00FF).toByte()
            }
        } else {
            for (i in 0 until shortArray.size) {
                offset = i * 2 + 1
                byteArray[i * 2] = shortArray[i].toInt().and(0x00FF).toByte()
                byteArray[i * 2 + 1] = shortArray[i].toInt().and(0xFF00).shr(8).toByte()
            }
        }
        return offset
    }

    companion object {
        val REQUIRED_PERMISSIONS = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        private const val MAX_RETRIES = 10
        private const val AUDIO_SAMPLE_RATES_IN_HZ = 8000
        private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_RECORD_DURATION : Long = 1000 * 15

        val status = MutableLiveData<Status>().apply {
            postValue(Status.CANCELED)
        }
        
        private val TAG : String = AmbientSoundCollector::class.java.simpleName

        fun checkEnableToCollect(context: Context) = PermissionUtils.checkPermissionAtRuntime(context, REQUIRED_PERMISSIONS)
    }
}