package kaist.iclab.abclogger.background.collector

import android.Manifest
import androidx.lifecycle.MutableLiveData
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import android.util.Log
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.background.Status
import kaist.iclab.abclogger.common.util.FormatUtils
import kaist.iclab.abclogger.common.util.PermissionUtils
import kaist.iclab.abclogger.common.util.Utils
import kaist.iclab.abclogger.data.PreferenceAccessor
import kaist.iclab.abclogger.data.entities.CallLogEntity
import kaist.iclab.abclogger.data.entities.MediaEntity
import kaist.iclab.abclogger.data.entities.MessageEntity
import kaist.iclab.abclogger.data.types.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ContentProviderCollector(val context: Context): BaseCollector {
    private var scheduledFuture: ScheduledFuture<*>? = null

    override fun startCollection(uuid: String, group: String, email: String) {
        if(scheduledFuture?.isDone == false) return
        status.postValue(Status.STARTED)

        scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            try {
                val now = System.currentTimeMillis()
                val pref = PreferenceAccessor.getInstance(context)

                collect(uuid, group, email, if(pref.lastTimeContentAccessed < 0) now - TimeUnit.HOURS.toMillis(1) else pref.lastTimeContentAccessed)

                pref.lastTimeContentAccessed = now
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

    private fun collect(uuid: String, group: String, email: String, lastTime: Long) {
        status.postValue(Status.RUNNING)

        if(!checkEnableToCollect(context)) throw SecurityException("Content provider is not granted to be collected.")

        App.boxFor<MessageEntity>().let { box ->
            val smsMessages = collectSmsLogs(lastTime, uuid, group, email)
            val mmsMessages = collectMmsLogs(lastTime, uuid, group, email)
            box.put(smsMessages)
            box.put(mmsMessages)

            smsMessages.forEach {
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                    "experimentGroup = ${it.experimentGroup}, entity = $it)")

                /* SMS - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */
                //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
            }

            mmsMessages.forEach {
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                    "experimentGroup = ${it.experimentGroup}, entity = $it)")

                /* MMS - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */
                //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
            }
        }

        App.boxFor<MediaEntity>().let { box ->
            val internalPhotos = collectInternalPhotos(lastTime, uuid, group, email)
            val internalVideos = collectInternalVideos(lastTime, uuid, group, email)
            val externalPhotos = collectExternalPhotos(lastTime, uuid, group, email)
            val externalVideos = collectExternalVideos(lastTime, uuid, group, email)

            box.put(internalPhotos)
            box.put(internalVideos)
            box.put(externalPhotos)
            box.put(externalVideos)

            internalPhotos.forEach {
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                    "experimentGroup = ${it.experimentGroup}, entity = $it)")

                /* internalPhoto - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */
                //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
            }

            internalVideos.forEach {
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                    "experimentGroup = ${it.experimentGroup}, entity = $it)")

                /* internal Video - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */
                //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
            }

            externalPhotos.forEach {
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                    "experimentGroup = ${it.experimentGroup}, entity = $it)")

                /* external Photo - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */
                //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
            }

            externalVideos.forEach {
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                    "experimentGroup = ${it.experimentGroup}, entity = $it)")

                /* external Video - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */
                //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
            }
        }

        App.boxFor<CallLogEntity>().let { box ->
            val callLogs = collectCallLogs(lastTime, uuid, group, email)
            box.put(callLogs)

            callLogs.forEach {
                Log.d(TAG, "Box.put(" +
                    "timestamp = ${it.timestamp}, subjectEmail = ${it.subjectEmail}, experimentUuid = ${it.experimentUuid}, " +
                    "experimentGroup = ${it.experimentGroup}, entity = $it)")

                /* call Log - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(it)
                /*
                val values = ContentValues()
                values.put(DAO.LOG_FIELD_JSON, jsonEntity)
                val c = context.contentResolver
                val handler = object: AsyncQueryHandler(c) {}
                handler.startInsert(-1, null, DataProvider.CONTENT_URI_LOG, values)
                */
                //MySQLiteLogger.writeStringData(context, it.javaClass.simpleName, it.timestamp, jsonEntity)
            }
        }
    }

    private data class Contact(var contactType: ContactType = ContactType.UNDEFINED,
                               var timesContacted: Int = 0,
                               var isStarred: Boolean = false,
                               var isPinned: Boolean = false)

    private fun isMillis (time: Long): Boolean {
        return FormatUtils.countNumDigits(System.currentTimeMillis()) == FormatUtils.countNumDigits(time)
    }

    private fun <T> getContentByDate(uri: Uri, lastTime: Long, timeColumn: String,
                                     columns: Array<String>, cursorToValue: (cursor: Cursor) -> T): List<T> {
        Log.d(TAG, "getContentByDate(uri=$uri, lastTime=$lastTime, timeColumn=$timeColumn, columns=${columns.joinToString(", ")})")

        val time = context.contentResolver.query(uri, arrayOf(timeColumn),null, null, "$timeColumn DESC LIMIT 1")
            .use { cursor ->
                return@use if(cursor?.moveToFirst() == true) { cursor.getLong(0) } else { null }
            }
        return time?.let {
            val timeInDb = if (isMillis(it)) { lastTime } else { lastTime / 1000 }

            return@let context.contentResolver.query(uri, columns, "$timeColumn >= ?", arrayOf(timeInDb.toString()), "$timeColumn ASC")
                .use { cursor ->
                    val entities = mutableListOf<T>()
                    while(cursor?.moveToNext() == true) {
                        entities.add(cursorToValue(cursor))
                    }
                    return@use entities
                }
        } ?: listOf()
    }

    private fun collectInternalPhotos(lastTime: Long, uuid: String, group: String, email: String) = getContentByDate(
        uri = MediaStore.Images.Media.INTERNAL_CONTENT_URI,
        lastTime = lastTime,
        timeColumn = MediaStore.Images.ImageColumns.DATE_TAKEN,
        columns = arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.MIME_TYPE, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME),
        cursorToValue = {
            MediaEntity(
                mimetype = it.getString(1) ?: "image/*",
                bucketDisplay = it.getString(2)
            ).apply {
                timestamp = if(isMillis(it.getLong(0))) it.getLong(0) else it.getLong(0) * 1000
                utcOffset = Utils.utcOffsetInHour()
                experimentUuid = uuid
                experimentGroup = group
                subjectEmail = email
                isUploaded = false
            }
        }
    )

    private fun collectExternalPhotos(lastTime: Long, uuid: String, group: String, email: String) = getContentByDate(
        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        lastTime = lastTime,
        timeColumn = MediaStore.Images.ImageColumns.DATE_TAKEN,
        columns = arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.MIME_TYPE, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME),
        cursorToValue = {
            MediaEntity(
                mimetype = it.getString(1) ?: "image/*",
                bucketDisplay = it.getString(2)
            ).apply {
                timestamp = if(isMillis(it.getLong(0))) it.getLong(0) else it.getLong(0) * 1000
                utcOffset = Utils.utcOffsetInHour()
                experimentUuid = uuid
                experimentGroup = group
                subjectEmail = email
                isUploaded = false
            }
        }
    )

    private fun collectInternalVideos(lastTime: Long, uuid: String, group: String, email: String) = getContentByDate(
        uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI,
        lastTime = lastTime,
        timeColumn = MediaStore.Video.VideoColumns.DATE_TAKEN,
        columns = arrayOf(MediaStore.Video.VideoColumns.DATE_TAKEN, MediaStore.Video.VideoColumns.MIME_TYPE, MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME),
        cursorToValue = {
            MediaEntity(
                mimetype = it.getString(1) ?: "video/*",
                bucketDisplay = it.getString(2)
            ).apply {
                timestamp = if(isMillis(it.getLong(0))) it.getLong(0) else it.getLong(0) * 1000
                utcOffset = Utils.utcOffsetInHour()
                experimentUuid = uuid
                experimentGroup = group
                subjectEmail = email
                isUploaded = false
            }
        }
    )

    private fun collectExternalVideos(lastTime: Long, uuid: String, group: String, email: String) = getContentByDate(
        uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        lastTime = lastTime,
        timeColumn = MediaStore.Video.VideoColumns.DATE_TAKEN,
        columns = arrayOf(MediaStore.Video.VideoColumns.DATE_TAKEN, MediaStore.Video.VideoColumns.MIME_TYPE, MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME),
        cursorToValue = {cursor ->
            MediaEntity(
                mimetype = cursor.getString(1) ?: "video/*",
                bucketDisplay = cursor.getString(2)
            ).apply {
                timestamp = if(isMillis(cursor.getLong(0))) cursor.getLong(0) else cursor.getLong(0) * 1000
                utcOffset = Utils.utcOffsetInHour()
                experimentUuid = uuid
                experimentGroup = group
                subjectEmail = email
                isUploaded = false
            }
        }
    )

    private fun collectCallLogs(lastTime: Long, uuid: String, group: String, email: String) = getContentByDate(
        uri = CallLog.Calls.CONTENT_URI,
        lastTime = lastTime,
        timeColumn = CallLog.Calls.DATE,
        columns = arrayOf(CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.NUMBER_PRESENTATION, CallLog.Calls.DATA_USAGE),
        cursorToValue = {cursor ->
            with(getContact(cursor.getString(2))) {
                CallLogEntity (
                    duration = cursor.getLong(1),
                    number = FormatUtils.formatHash(cursor.getString(2), 0, 4),
                    type = CallType.fromValue(cursor.getInt(3), CallType.UNDEFINED),
                    presentation = CallPresentationType.fromValue(cursor.getInt(4), CallPresentationType.UNKNOWN),
                    dataUsage = cursor.getLong(5),
                    contact = contactType,
                    timesContacted = timesContacted,
                    isStarred = isStarred,
                    isPinned = isPinned
                ).apply {
                    timestamp = if(isMillis(cursor.getLong(0))) cursor.getLong(0) else cursor.getLong(0) * 1000
                    utcOffset = Utils.utcOffsetInHour()
                    experimentUuid = uuid
                    experimentGroup = group
                    subjectEmail = email
                    isUploaded = false
                }
            }
        }
    )

    private fun collectSmsLogs(lastTime: Long, uuid: String, group: String, email: String) = getContentByDate(
        uri = Telephony.Sms.CONTENT_URI,
        lastTime = lastTime,
        timeColumn = Telephony.Sms.DATE,
        columns = arrayOf(Telephony.Sms.DATE, Telephony.Sms.ADDRESS, Telephony.Sms.TYPE),
        cursorToValue = {cursor ->
            with(getContact(cursor.getString(2))) {
                MessageEntity(
                    number = FormatUtils.formatHash(cursor.getString(1), 0, 4),
                    messageClass = MessageClassType.SMS,
                    messageBox = MessageBoxType.fromValue(cursor.getInt(2), MessageBoxType.UNDEFINED),
                    contact = contactType,
                    timesContacted = timesContacted,
                    isStarred = isStarred,
                    isPinned = isPinned
                ).apply {
                    timestamp = if(isMillis(cursor.getLong(0))) cursor.getLong(0) else cursor.getLong(0) * 1000
                    utcOffset = Utils.utcOffsetInHour()
                    experimentUuid = uuid
                    experimentGroup = group
                    subjectEmail = email
                    isUploaded = false
                }
            }
        }
    )

    private fun collectMmsLogs(lastTime: Long, uuid: String, group: String, email: String) = getContentByDate(
        uri = Telephony.Mms.CONTENT_URI,
        lastTime = lastTime,
        timeColumn = Telephony.Mms.DATE,
        columns = arrayOf(Telephony.Mms.DATE, Telephony.Mms._ID, Telephony.Mms.MESSAGE_BOX),
        cursorToValue = {cursor ->
            with(getContact(cursor.getString(1))) {
                MessageEntity(
                    number = getMmsAddress(cursor.getLong(1))?.let { FormatUtils.formatHash(it, 0, 4) } ?: "",
                    messageClass = MessageClassType.MMS,
                    messageBox = MessageBoxType.fromValue(cursor.getInt(2), MessageBoxType.UNDEFINED),
                    contact = contactType,
                    timesContacted = timesContacted,
                    isStarred = isStarred,
                    isPinned = isPinned
                ).apply {
                    timestamp = if(isMillis(cursor.getLong(0))) cursor.getLong(0) else cursor.getLong(0) * 1000
                    utcOffset = Utils.utcOffsetInHour()
                    experimentUuid = uuid
                    experimentGroup = group
                    subjectEmail = email
                    isUploaded = false
                }
            }
        }
    )

    private fun getContact (number: String?): Contact {
        return number?.let {
            return@let context.contentResolver.query(
                Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(it)),
                arrayOf(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED, ContactsContract.CommonDataKinds.Phone.STARRED, ContactsContract.CommonDataKinds.Phone.PINNED),
                null, null, null
            ).use {cursor ->
                val contact = Contact()

                while (cursor?.moveToNext() == true) {
                    contact.contactType = ContactType.fromValue(cursor.getInt(0), ContactType.UNDEFINED)
                    contact.timesContacted += cursor.getInt(1)
                    contact.isStarred = contact.isStarred or (cursor.getInt(2) == 1)
                    contact.isPinned = contact.isPinned or (cursor.getInt(3) == 1)
                }
                return@use contact
            }
        } ?: Contact()
    }

    private fun getMmsAddress (id: Long): String? {
        return context.contentResolver.query(
            Uri.withAppendedPath(
                Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, id.toString()), "addr"),
            arrayOf(Telephony.Mms.Addr.ADDRESS),
            null, null, null
        )?.use {
            return@use if (it.moveToFirst()) it.getString(0) else null
        }
    }

    companion object {
        val REQUIRED_PERMISSIONS = listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_SMS
        )

        val status = MutableLiveData<Status>().apply {
            postValue(Status.CANCELED)
        }

        private val TAG : String = ContentProviderCollector::class.java.simpleName

        fun checkEnableToCollect(context: Context) = PermissionUtils.checkPermissionAtRuntime(context, REQUIRED_PERMISSIONS)
    }
}