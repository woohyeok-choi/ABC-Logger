package kaist.iclab.abclogger.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kaist.iclab.abclogger.data.MySQLiteLogger.Companion.getExported
import kaist.iclab.abclogger.data.MySQLiteLogger.Companion.setExported

class MySQLiteOpenHelper(context: Context?): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onOpen(db: SQLiteDatabase) {
        Log.i(TAG, "onOpen:${db.version}, $db")
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "onCreate:$db")
        initTables(db)
    }

     //버전이 다를경우 업그레이드 함수가 실행된다. 이경우 마이그레시션 코드를 구현할 필요가 있음
    override fun onUpgrade(db: SQLiteDatabase, oldVersion:Int, newVersion:Int) {
        Log.d(TAG, "onUpgrade: $oldVersion -> $newVersion $db")
        initTables(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion:Int, newVersion:Int) {
        Log.d(TAG, "onDowngrade: $oldVersion -> $newVersion $db")
        initTables(db)
    }



    private fun initTables(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_LOG_SQL)
        db.execSQL(CREATE_TABLE_USER_SQL)
    }

    fun reCreateLogTable() {
        Log.d(TAG, "reCreateLogTable ()")
        writableDatabase.execSQL(DROP_TABLE_LOG_SQL)
        writableDatabase.execSQL(CREATE_TABLE_LOG_SQL)
    }

    fun writeLog(contentValues: ContentValues): Long {
        return writableDatabase.insert(TABLE_LOG, null, contentValues)
    }

    fun writeLog(values:Array<ContentValues>): Long {

        if (getExported()) {
            reCreateLogTable()
            setExported(false)
        }

        val sqLiteDatabase = writableDatabase
        sqLiteDatabase.beginTransaction()

        var stringBuilder = StringBuilder()
        var result:Long = -1

        for (i in values.indices) {
            val contentValues = values[i]
            stringBuilder.append("(")
                    .append("'" + contentValues.getAsString(LOG_FIELD_TYPE) + "'").append(",")
                    .append("'" + contentValues.getAsString(LOG_FIELD_JSON) + "'").append(",")
                    .append(contentValues.getAsLong(LOG_FIELD_REG))
                    .append(")")
            if (i != 0 && i % insertValueLength == 0 || i == values.size - 1) {
                val sql = String.format(LOG_MULTI_INSERT_SQL_FORMAT, TABLE_LOG, LOG_FIELD_TYPE, LOG_FIELD_JSON, LOG_FIELD_REG, stringBuilder.toString())
                val sqLiteStatement = sqLiteDatabase.compileStatement(sql)
                result = sqLiteStatement.executeInsert()
                sqLiteStatement.clearBindings()
                stringBuilder = StringBuilder()
            } else {
                stringBuilder.append(", ")
            }
        }

        sqLiteDatabase.setTransactionSuccessful()
        sqLiteDatabase.endTransaction()
        Log.d(TAG, (values.size).toString() + "logs have been inserted.")
        return result
    }

    fun queryLogCount(projection: Array<String>?, selection: String?, selectionArgs: Array<String>?): Cursor {
        return readableDatabase.query(TABLE_LOG, projection, selection, selectionArgs, null, null, null)
    }

    companion object {

        private val TAG = MySQLiteOpenHelper::class.java.simpleName

        const val DATABASE_NAME = "sensors_data.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_LOG = "log"
        const val LOG_FIELD_TYPE = "type"
        const val LOG_FIELD_JSON = "json"
        const val LOG_FIELD_REG = "reg"

        private val CREATE_TABLE_LOG_SQL = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "insert_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                "%s TEXT NOT NULL, " +
                "%s TEXT NOT NULL, " +
                "%s LONG NOT NULL);",
                TABLE_LOG, LOG_FIELD_TYPE, LOG_FIELD_JSON, LOG_FIELD_REG)

        const val TABLE_USER = "user"

        const val USER_FIELD_NAME = "name"
        const val USER_FIELD_AGE = "age"
        const val USER_FIELD_PHONE_NUMBER = "phone_number"

        private val CREATE_TABLE_USER_SQL = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "insert_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                "%s TEXT NOT NULL, " +
                "%s TEXT NOT NULL, " +
                "%s INT NOT NULL);",
                TABLE_USER, USER_FIELD_NAME, USER_FIELD_PHONE_NUMBER, USER_FIELD_AGE)

        private const val DROP_TABLE_LOG_SQL: String = "DROP TABLE IF EXISTS $TABLE_LOG;"

        private const val LOG_MULTI_INSERT_SQL_FORMAT = "INSERT INTO %s(%s, %s, %s) VALUES %s"

        private const val insertValueLength = 900
    }
}
