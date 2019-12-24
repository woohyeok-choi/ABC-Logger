package kaist.iclab.abclogger.collector

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.beust.klaxon.Klaxon
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.SurveyEntity
import kaist.iclab.abclogger.fillBaseInfo
import kaist.iclab.abclogger.survey.Survey
import java.lang.Exception

class SurveyCollector2 (val context: Context): BaseCollector {
    class TriggerIntentService : IntentService(TriggerIntentService::class.java.name) {
        override fun onHandleIntent(intent: Intent?) {
            if (TextUtils.isEmpty(SharedPrefs.surveyJson)) return

            try {
                val curTime = System.currentTimeMillis()
                val selectedSurvey = intent?.getStringExtra(EXTRA_SURVEY_UUID)?.let { uuid ->
                    Klaxon().parseArray<Survey>(SharedPrefs.surveyJson)?.firstOrNull {
                        survey -> survey.uuid == uuid
                    }
                } ?: return

                SurveyEntity(
                        title = selectedSurvey.title,
                        message = selectedSurvey.message,
                        timeoutSec = selectedSurvey.timeoutSec,
                        timeoutPolicy = selectedSurvey.timeoutPolicy,
                        deliveredTime = curTime
                ).fillBaseInfo(timestamp = curTime).run { putEntity(this) }
            } catch (e: Exception) {

            }
        }

        private fun notify(survey: Survey)
    }

    override fun start() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun checkAvailability(): Boolean = true

    override fun getRequiredPermissions(): List<String> = listOf()

    override fun newIntentForSetup(): Intent? {
        TODO("implement an activity that download and preview surveys")
    }

    companion object {
        private const val EXTRA_SURVEY_UUID = "kaist.iclab.abclogger.EXTRA_SURVEY_UUID"
    }
}