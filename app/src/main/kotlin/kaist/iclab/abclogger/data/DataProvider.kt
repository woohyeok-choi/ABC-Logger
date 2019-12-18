package kaist.iclab.abclogger.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.util.Log

class DataProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        Log.i(TAG, "onCreate")

        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        try {
            when (uriMatcher.match(uri)) {
                LOG -> {
                    return getMySQLiteOpenHelper()!!.queryLogCount(projection, selection, selectionArgs)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        try {
            when (uriMatcher.match(uri)) {
                LOG -> {
                    Log.d(TAG, "CP insert: $uri")
                    val id = getMySQLiteOpenHelper()!!.writeLog(values)
                    Log.d(TAG, "new id: $id")
                    return Uri.withAppendedPath(CONTENT_URI_LOG, id.toString() + "")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

        return null
    }


    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        Log.d(TAG, "Insert: $uri")
        val match = uriMatcher.match(uri)
        // Validate.isTrue(match == LOG)
        return if (match == LOG) {
            getMySQLiteOpenHelper()!!.writeLog(values).toInt()
        } else {
            0
        }
    }

    override fun delete(uri: Uri, selection: String, selectionArgs: Array<String>): Int {
        //We don't delete any previous logs
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        //We don't update any previous logs
        return 0
    }

    fun getMySQLiteOpenHelper(): MySQLiteOpenHelper? {
        if (mySQLiteOpenHelper == null) {
            mySQLiteOpenHelper = MySQLiteOpenHelper(context)
        }
        return mySQLiteOpenHelper
    }

    companion object {

        private val TAG = "DataProvider"

        val PROVIDER_NAME = "kaist.iclab.abclogger.data.dataprovider"
        val CONTENT_URI_LOG = Uri.parse("content://$PROVIDER_NAME/log")

        private val LOG = 10
        private val uriMatcher: UriMatcher

        init {
            uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            uriMatcher.addURI(PROVIDER_NAME, "log", LOG)
        }


        private var mySQLiteOpenHelper: MySQLiteOpenHelper? = null
    }


}
