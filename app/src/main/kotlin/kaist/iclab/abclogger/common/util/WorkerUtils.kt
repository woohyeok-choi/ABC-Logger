package kaist.iclab.abclogger.common.util

import androidx.annotation.WorkerThread
import androidx.work.*
import java.util.concurrent.TimeUnit

object WorkerUtils {
    @WorkerThread
    inline fun <reified T: Worker> startOneTimeWorker(delayInMs: Long, isForced: Boolean, data: Data = Data.EMPTY) {
        val tag = T::class.java.name

        val request = OneTimeWorkRequest.Builder(T::class.java)
            .setInputData(data)
            .setInitialDelay(delayInMs, TimeUnit.MILLISECONDS)
            .addTag(tag)
            .build()

        val syncManager = WorkManager.getInstance().synchronous()
        syncManager.pruneWorkSync()

        if (isForced) {
            syncManager.cancelAllWorkByTagSync(tag)
            syncManager.enqueueSync(request)
        } else {
            val isFinished = syncManager.getStatusesByTagSync(tag).all { it.state.isFinished }
            if (isFinished) {
                syncManager.enqueueSync(request)
            }
        }
    }

    @WorkerThread
    inline fun <reified T: Worker> startPeriodicWorker(periodInMs: Long, isForced: Boolean = false, data: Data = Data.EMPTY) {
        val id = T::class.java.name

        val request = PeriodicWorkRequest.Builder(T::class.java, periodInMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        val syncManager = WorkManager.getInstance().synchronous()
        syncManager.pruneWorkSync()
        if(isForced) {
            syncManager.enqueueUniquePeriodicWorkSync(id, ExistingPeriodicWorkPolicy.REPLACE, request)
        } else {
            val isFinished = syncManager.getStatusesForUniqueWorkSync(id).all { it.state.isFinished }
            if(isFinished) {
                syncManager.enqueueUniquePeriodicWorkSync(id, ExistingPeriodicWorkPolicy.REPLACE, request)
            }
        }
    }

    inline fun <reified T: Worker> startPeriodicWorkerAsync(periodInMs: Long, isForced: Boolean, data: Data = Data.EMPTY) {
        val request = PeriodicWorkRequest.Builder(T::class.java, periodInMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        val asyncManager = WorkManager.getInstance()
        asyncManager.pruneWork()
        asyncManager.enqueueUniquePeriodicWork(T::class.java.name, if(isForced) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP, request)
    }

    @WorkerThread
    inline fun <reified T: Worker> stopPeriodicWorker() {
        WorkManager.getInstance().synchronous().cancelUniqueWorkSync(T::class.java.name)
    }

    inline fun <reified T: Worker> stopPeriodicWorkerAsync() {
        WorkManager.getInstance().cancelUniqueWork(T::class.java.name)
    }

    @WorkerThread
    inline fun <reified T: Worker> stopOneTimeWorker() {
        WorkManager.getInstance().synchronous().cancelAllWorkByTagSync(T::class.java.name)
    }
}