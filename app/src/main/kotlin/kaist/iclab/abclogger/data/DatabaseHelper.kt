package kaist.iclab.abclogger.data

import android.content.ContentResolver
import android.util.Log
import com.madrapps.asyncquery.AsyncQueryHandler
import kaist.iclab.abclogger.data.MySQLiteLogger.Companion.setSending

class DatabaseHandler(cr: ContentResolver) : AsyncQueryHandler(cr) {

    override fun onBulkInsertComplete(token: Int, cookie: Any, result: Int) {
        Log.d(TAG, "Bulk Insert Done")
        setSending(false)
        super.onBulkInsertComplete(token, cookie, result)
    }

    companion object {
        val TAG = this::class.java.simpleName
    }
}