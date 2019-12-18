package kaist.iclab.abclogger.background.collector

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import kaist.iclab.abclogger.common.util.FormatUtils

fun <T> getRecentContents(contentResolver: ContentResolver,
                          uri: Uri, timeColumn: String, columns: Array<String>,
                          lastTime : Long = -1, transform: (cursor: Cursor) -> T): List<T> {
    return contentResolver.query(
            uri, arrayOf(timeColumn), null, null, "$timeColumn DESC LIMIT 1"
    ).use { cursor ->
        return@use if(cursor?.moveToFirst() == true) {
            cursor.getLong(0)
        } else {
            null
        }
    }?.let { latestTime ->
        val formattedLastTime = if (FormatUtils.countNumDigits(latestTime) == FormatUtils.countNumDigits(System.currentTimeMillis())) {
            lastTime
        } else {
            lastTime / 1000
        }

        return@let contentResolver.query(
                uri, columns, "$timeColumn >= ?", arrayOf(formattedLastTime.toString()), "$timeColumn ASC"
        ).use { cursor ->
            val entities = mutableListOf<T>()
            while (cursor?.moveToNext() == true) {
                entities.add(transform(cursor))
            }
            return@use entities
        }
    } ?: emptyList()
}

