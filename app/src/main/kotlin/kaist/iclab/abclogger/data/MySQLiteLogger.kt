package kaist.iclab.abclogger.data

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import kaist.iclab.abclogger.prefs
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MySQLiteLogger(base: Context) : ContextWrapper(base) {
    init {
        debug("MySQLiteLogger()")
    }

    private fun debug(msg: String) {
        debug(TAG, msg)

    }

    private fun addContentValues(TAG: String?, msg: String?, timestamp: Long) {
        // Validate.notEmpty(TAG)
        // Validate.notEmpty(msg)
        if (TAG == null) throw NullPointerException()
        if (msg == null) throw NullPointerException()

        val contentValues = ContentValues()
        contentValues.put(MySQLiteOpenHelper.LOG_FIELD_TYPE, TAG)
        contentValues.put(MySQLiteOpenHelper.LOG_FIELD_JSON, msg)
        contentValues.put(MySQLiteOpenHelper.LOG_FIELD_REG, timestamp)
        getContentValuesListToWrite().add(contentValues)

        tryToSend(this, false)
    }

    protected fun debug(TAG: String, msg: String) {
        //addContentValues(TAG, msg, System.currentTimeMillis());
        Log.d(TAG, msg)
    }

    protected fun info(TAG: String, msg: String) {
        //addContentValues(TAG, msg, System.currentTimeMillis());
        //Log.i(TAG, msg);
    }

    protected fun data(tag: String, timestamp: Long, format: String, vararg args: Any) {
        addContentValues(tag, String.format(format, *args), timestamp)
        Log.d(TAG, String.format(format, *args))
    }

    companion object {

        private val TAG = MySQLiteLogger::class.java.simpleName

        private val contentValuesListToWrite = ArrayList<ContentValues>()

        private val MAX_NUMBER_OF_LOGS_IN_MEMORY = 900

        private var sending = false

        private var exported = false

        fun setSending(flag: Boolean) { sending = flag }

        fun setExported(flag: Boolean) {exported = flag}

        fun getExported(): Boolean { return exported}

        private fun getContentValuesListToWrite(): MutableList<ContentValues> {
            return contentValuesListToWrite
        }

        fun forceToWriteContentValues(context: Context): Boolean{
            return tryToSend(context, true)
        }

        private fun tryToSend(context: Context, force: Boolean): Boolean {
            Log.d(TAG, "tryToSend // force: $force, sending: $sending, exported: $exported")
            return if (sending) {
                false
            } else if (getContentValuesListToWrite().size > MAX_NUMBER_OF_LOGS_IN_MEMORY || force) {
                setSending(true)
                val contentValuesArray = getContentValuesListToWrite().toTypedArray()
                Log.d(TAG, "tryToSend () BulkInsert start")
                val mHandler = Handler(Looper.getMainLooper())
                mHandler.postDelayed({
                    val handler = DatabaseHandler(context.contentResolver)
                    handler.startBulkInsert(1, -1, DataProvider.CONTENT_URI_LOG, contentValuesArray)
                }, 0)
                getContentValuesListToWrite().clear()
                Log.d(TAG, "tryToSend () BulkInsert end")
                true
            } else {
                false
            }
        }

        fun writeStringData(context: Context, tag: String, timestamp: Long, data: String) {
            // MySQLiteLogger.addContentValues(tag, data, timestamp)
            val contentValues = ContentValues()

            contentValues.put(MySQLiteOpenHelper.LOG_FIELD_TYPE, tag)
            contentValues.put(MySQLiteOpenHelper.LOG_FIELD_JSON, data)
            contentValues.put(MySQLiteOpenHelper.LOG_FIELD_REG, timestamp)
            //Log.d(TAG, "$tag: $timestamp, $data")

            getContentValuesListToWrite().add(contentValues)
            tryToSend(context, false)
        }

        fun exportSQLite(context: Context, phoneNumber: String?) {
            Log.d(TAG, "sqlite export start")
            forceToWriteContentValues(context)
            val mHandler = Handler(Looper.getMainLooper())
            mHandler.postDelayed({
                exportSQLiteFile()
            }, 10 * 1000)
        }

        fun exportSQLiteFile() {
            try {
                val internal = Environment.getDataDirectory()
                val external = Environment.getExternalStorageDirectory()

                val directory = File(external.absolutePath + "/ABC_Logger")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                if (external.canWrite()) {
                    val name = MySQLiteOpenHelper.DATABASE_NAME
                    var pNum = prefs.participantPhoneNumber
                    if (pNum == null) {
                        pNum = "01087654321"
                    }
                    val currentDB = File(internal, "/user/0/kaist.iclab.abclogger/databases/$name")
                    val exportDB = File(external, "/ABC_Logger/${SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Date(System.currentTimeMillis()))}-$pNum.db")

                    if (currentDB.exists()) {
                        val src = FileInputStream(currentDB).channel
                        val dst = FileOutputStream(exportDB).channel

                        dst.transferFrom(src, 0, src.size())

                        src.close()
                        dst.close()

                        Log.d("Ria", ">>> SQLite > SQLiteExport")
                        //Toast.makeText(context, "DB export success !", Toast.LENGTH_LONG).show()
                        //currentDB.delete()
                        //getMySQLiteOpenHelper()
                        setExported(true)
                    } else {
                        Log.d(TAG, "current db not found.")
                    }
                    //return currentDB
                } else {
                    Log.d(TAG, "external write permission denied.")
                }
            } catch (e: Exception) {
                Log.d(TAG, "DB export failed.")
            }
        }
    }
}