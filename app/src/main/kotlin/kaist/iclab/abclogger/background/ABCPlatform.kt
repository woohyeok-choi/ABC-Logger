package kaist.iclab.abclogger.background

import android.content.Context
import kaist.iclab.abclogger.collector.SurveyCollector
import kaist.iclab.abclogger.data.PreferenceAccessor

object ABCPlatform {
    fun start(context: Context) {
        CollectorWorker.start(true)
        SurveyCollector.schedule(context)
        SyncManager.sync(true)
    }

    fun maintain(context: Context) {
        CollectorWorker.start(false)
        SurveyCollector.schedule(context)
        SyncManager.sync(false)
    }

    fun stop(context: Context) {
        CollectorWorker.stop(context)
        SurveyCollector.cancel(context)
        PreferenceAccessor.getInstance(context).clear()
    }
}