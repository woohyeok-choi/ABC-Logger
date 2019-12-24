package kaist.iclab.abclogger.base

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import android.util.Log

abstract class BaseJobIntentService : JobIntentService() {
    protected val TAG: String = javaClass.simpleName

    companion object {
        inline fun <reified T: JobIntentService> enqueue(context: Context, intent: Intent, jobId: Int) {
            enqueueWork(context, T::class.java, jobId, intent)
        }
    }

    override fun onHandleWork(intent: Intent) {
        Log.d(TAG, "onHandleWork()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
    }
}