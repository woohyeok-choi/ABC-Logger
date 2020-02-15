package kaist.iclab.abclogger.ui.main

import android.content.Context
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kaist.iclab.abclogger.AbcCollector
import kaist.iclab.abclogger.Prefs
import kaist.iclab.abclogger.SyncWorker
import kaist.iclab.abclogger.ui.base.BaseNavigator
import kaist.iclab.abclogger.ui.base.BaseViewModel

class MainViewModel(private val context: Context) : BaseViewModel<BaseNavigator>() {
    override suspend fun onLoad(extras: Bundle?) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (!email.isNullOrBlank()) crashlytics.setUserId(email)

        crashlytics.sendUnsentReports()

        AbcCollector.start(context)
        SyncWorker.requestStart(
                context = context,
                forceStart = false,
                enableMetered = Prefs.canUploadMeteredNetwork,
                isPeriodic = Prefs.isAutoSync
        )
    }

    override suspend fun onStore() {}

}