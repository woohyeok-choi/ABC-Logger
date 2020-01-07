package kaist.iclab.abclogger

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor

object ObjBox {
    lateinit var boxStore : BoxStore

    fun bind(context: Context) {
        boxStore = MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .maxSizeInKByte(2097152) //2 GB
                .name("abc-logger-db")
                .build()
        TODO("Check available spaces or db file.")

    }

    fun getDBSize() : Long {
        TODO("Check db size")
    }

    inline fun <reified T> boxFor() = boxStore.boxFor<T>()
}