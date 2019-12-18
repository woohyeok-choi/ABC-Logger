package kaist.iclab.abclogger.foreground.activity


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.core.view.ViewCompat
import android.transition.*
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.beust.klaxon.Klaxon
import com.google.android.gms.tasks.Tasks
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.ABCException
import kaist.iclab.abclogger.common.InvalidContentException
import kaist.iclab.abclogger.common.base.BaseAppCompatActivity
import kaist.iclab.abclogger.common.type.LoadState
import kaist.iclab.abclogger.common.type.LoadStatus
import kaist.iclab.abclogger.common.util.*
import kaist.iclab.abclogger.data.entities.SensorEntity
import kaist.iclab.abclogger.data.entities.SurveyEntity
import kaist.iclab.abclogger.foreground.dialog.YesNoDialogFragment
import kaist.iclab.abclogger.foreground.fragment.SurveyListFragment
import kaist.iclab.abclogger.prefs
import kaist.iclab.abclogger.survey.Survey

import kaist.iclab.abclogger.survey.SurveyTimeoutPolicyType
import kotlinx.android.synthetic.main.activity_container_with_toolbar.*
import kotlinx.android.synthetic.main.activity_survey_question.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class SurveyQuestionActivity: BaseAppCompatActivity(), SensorEventListener {
    private lateinit var survey: MutableLiveData<Pair<Survey, SurveyEntity>>
    private lateinit var initLoadState: MutableLiveData<LoadState>
    private lateinit var loadState: MutableLiveData<LoadState>
    private lateinit var storeState: MutableLiveData<LoadState>

    private var isInitialized = false

    /** SW EDIT */
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI)
    }

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null

    override fun onSensorChanged(p0: SensorEvent) {
        val now = System.currentTimeMillis()
        val timestampSensor = p0.timestamp
        val accuracy = p0.accuracy
        val value = p0.values[0]
        val name = p0.sensor.stringType

        val entity = SensorEntity(
                type = name,
                firstValue = value,
                secondValue = accuracy.toFloat(),
                thirdValue = timestampSensor.toFloat()
        ).apply {
            timestamp = now
            utcOffset = Utils.utcOffsetInHour()
            subjectEmail = prefs.participantEmail!!
            experimentUuid = prefs.participantPhoneNumber!!
            experimentGroup = prefs.participantGroup!!
            isUploaded = false
        }
        App.boxFor<SensorEntity>().put(entity)

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //TOD("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationUtils.cancelSurveyDelivered(this)
        NotificationUtils.cancelSurveyRemained(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        setupContentView()
        setupActionBar()
        if(intent.getBooleanExtra(EXTRA_SHOW_FROM_SURVEY_LIST, false)) {
            setupAnimation()
        }

        setupLiveData()
        setupListener()

        bindView()

        if(!intent.getBooleanExtra(EXTRA_SHOW_FROM_SURVEY_LIST, false)) {
            loadSurvey(true)
        }
    }

    override fun onBackPressed() {
        if(intent.getBooleanExtra(EXTRA_SHOW_FROM_SURVEY_LIST, false)) {
            supportFinishAfterTransition()
        } else {
            finish()
        }
    }

    private fun loadSurvey(isFirstTime: Boolean) {
        if(isFirstTime) {
            initLoadState.postValue(LoadState.LOADING)
        } else {
            loadState.postValue(LoadState.LOADING)
        }

        Tasks.call(Executors.newSingleThreadExecutor(), Callable {
            val box = App.boxFor<SurveyEntity>()
            var entity = box.get(intent.getLongExtra(EXTRA_SURVEY_ENTITY_ID, 0))
            val survey = Survey.parse(entity.responses)
            if (entity.reactionTime <= 0) {
                entity = entity.copy(reactionTime = System.currentTimeMillis()).apply {
                    id = entity.id
                    timestamp = entity.timestamp
                    utcOffset = entity.utcOffset
                    experimentUuid = entity.experimentUuid
                    experimentGroup = entity.experimentGroup
                    subjectEmail = entity.subjectEmail
                    isUploaded = entity.isUploaded
                }
                box.put(entity)
            }
            Pair(survey, entity)
        }).addOnSuccessListener {
            survey.postValue(it)

            if(isFirstTime) {
                initLoadState.postValue(LoadState.LOADED)
            } else {
                loadState.postValue(LoadState.LOADED)
            }
        }.addOnFailureListener {
            if(isFirstTime) {
                initLoadState.postValue(LoadState.ERROR(it))
            } else {
                loadState.postValue(LoadState.ERROR(it))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            android.R.id.home -> {
                if(intent.getBooleanExtra(EXTRA_SHOW_FROM_SURVEY_LIST, false)) {
                    supportFinishAfterTransition()
                } else {
                    finish()
                }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun setupContentView() {
        setContentView(R.layout.activity_container_with_toolbar)
        container.addView(layoutInflater.inflate(R.layout.activity_survey_question, container, false))
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.activity_title_survey_question)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupAnimation() {
        ViewCompat.setTransitionName(surveyView.getTitleView(), "${SurveyListFragment.PREFIX_TITLE_VIEW}_${intent.getLongExtra(EXTRA_SURVEY_ENTITY_ID, 0)}")
        ViewCompat.setTransitionName(surveyView.getMessageView(), "${SurveyListFragment.PREFIX_MESSAGE_VIEW}_${intent.getLongExtra(EXTRA_SURVEY_ENTITY_ID, 0)}")
        ViewCompat.setTransitionName(surveyView.getDeliveredTimeView(), "${SurveyListFragment.PREFIX_DELIVERED_TIME_VIEW}_${intent.getLongExtra(EXTRA_SURVEY_ENTITY_ID, 0)}")

        window.sharedElementEnterTransition.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(p0: Transition?) {
                if(!isInitialized) loadSurvey(true)
                isInitialized = true
            }

            override fun onTransitionResume(p0: Transition?) { }

            override fun onTransitionPause(p0: Transition?) { }

            override fun onTransitionCancel(p0: Transition?) { }

            override fun onTransitionStart(p0: Transition?) { }
        })

        window.allowReturnTransitionOverlap = true
    }

    private fun setupLiveData() {
        survey = MutableLiveData()
        loadState = MutableLiveData()
        initLoadState = MutableLiveData()
        storeState = MutableLiveData()

        survey.observe(this, Observer {
            val survey = it?.first
            val entity = it?.second
            if(survey != null && entity != null) {
                Log.d(TAG, "survey loaded...")

                val now = System.currentTimeMillis()
                val isAfterTimeout = now - entity.deliveredTime >= survey.policy.timeout.toMillis()
                val isAlreadyResponded = entity.timestamp > 0
                val isExpired = isAfterTimeout && survey.policy.timeoutPolicyType == SurveyTimeoutPolicyType.DISABLED

                val showAltText = isAfterTimeout && survey.policy.timeoutPolicyType == SurveyTimeoutPolicyType.ALTERNATIVE_TEXT
                val isEnableToRespond = entity.isEnableToResponed(now)

                surveyView.bindView(
                    survey = survey,
                    deliveredTime = entity.deliveredTime,
                    enabledToEdit = isEnableToRespond,
                    showAltText = showAltText
                )

                ViewUtils.bindButton(btnRespond, isEnableToRespond,
                    when {
                        isAlreadyResponded -> R.string.btn_error_already_respond
                        isExpired -> R.string.btn_error_expired_survey
                        else -> R.string.btn_respond
                    }
                )
            }
        })

        initLoadState.observe(this, Observer {
            Log.d(TAG, "initLoadState: ${it?.status}")
            surveyView.setShowQuestions(it?.status == LoadStatus.SUCCESS)
            surveyView.setShowProgressBar(it?.status == LoadStatus.RUNNING)

            scrollView.visibility = if (it?.status == LoadStatus.FAILED) View.GONE else View.VISIBLE
            btnRespond.visibility = if (it?.status == LoadStatus.SUCCESS) View.VISIBLE else View.GONE
            txtError.visibility = if (it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            if (it?.error != null) txtError.setText(
                if (it.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
            )
        })

        loadState.observe(this, Observer {
            surveyView.setShowQuestions(it?.status == LoadStatus.SUCCESS)
            swipeLayout.isRefreshing = it?.status == LoadStatus.RUNNING
            scrollView.visibility = if (it?.status == LoadStatus.SUCCESS) View.VISIBLE else View.GONE
            txtError.visibility = if (it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            if (it?.error != null) txtError.setText(
                if (it.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
            )
        })


        storeState.observe(this, Observer {
            surveyView.setEnabledToEdit(it?.status != LoadStatus.RUNNING)

            when(it?.status) {
                LoadStatus.RUNNING -> btnRespond.startWith()
                LoadStatus.SUCCESS -> btnRespond.succeedWith(false) {
                    ViewUtils.showToast(this, R.string.msg_general_complete_save)
                    overridePendingTransition(0, 0)
                    finish()
                }
                LoadStatus.FAILED -> btnRespond.failedWith {
                    if (it.error != null) ViewUtils.showToast(this,
                        if (it.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
                    )
                }
                else -> { }
            }
        })
    }

    private fun setupListener() {
        btnRespond.setOnClickListener {
            val title = getString(R.string.dialog_title_survey_respond)
            val message = getString(R.string.dialog_message_survey_respond)
            val dialog = YesNoDialogFragment.newInstance(title, message)
            dialog.setOnDialogOptionSelectedListener { result ->
                if(result) {
                    storeState.postValue(LoadState.LOADING)

                    Tasks.call(Executors.newSingleThreadExecutor(), Callable {
                        if(!surveyView.isValid()) throw InvalidContentException()

                        val box = App.boxFor<SurveyEntity>()
                        var entity = box.get(intent.getLongExtra(EXTRA_SURVEY_ENTITY_ID, 0))
                        val survey = Survey.parse(entity.responses).copy(
                            questions = surveyView.getResponses()
                        )
                        entity = entity.copy(
                            responses = Klaxon().toJsonString(survey),
                            firstQuestionTime = surveyView.getFirstInteractionTime()
                        ).apply {
                            id = entity.id
                            timestamp = System.currentTimeMillis()
                            utcOffset = Utils.utcOffsetInHour()
                            experimentUuid = entity.experimentUuid
                            experimentGroup = entity.experimentGroup
                            subjectEmail = entity.subjectEmail
                            isUploaded = entity.isUploaded
                        }
                        box.put(entity)
                        Log.d(TAG, "Box.put(" +
                            "timestamp = ${entity.timestamp}, subjectEmail = ${entity.subjectEmail}, experimentUuid = ${entity.experimentUuid}, " +
                            "experimentGroup = ${entity.experimentGroup}, entity = $entity)")
                    }).addOnSuccessListener { _ ->
                        storeState.postValue(LoadState.LOADED)
                    }.addOnFailureListener { exception ->
                        storeState.postValue(LoadState.ERROR(exception))
                    }
                }
            }
            dialog.show(supportFragmentManager, TAG)
        }

        swipeLayout.setOnRefreshListener { loadSurvey(false) }
    }

    private fun bindView() {
        surveyView.setTitle(intent.getStringExtra(EXTRA_SURVEY_TITLE))
        surveyView.setMessage(intent.getStringExtra(EXTRA_SURVEY_MESSAGE))
        surveyView.setDeliveredTime(intent.getLongExtra(EXTRA_SURVEY_DELIVERED_TIME, 0))
        surveyView.setEnabledToEdit(intent.getBooleanExtra(EXTRA_SURVEY_IS_ENABLE_TO_RESPOND, true))
    }

    companion object {
        private val EXTRA_SURVEY_ENTITY_ID = "${SurveyQuestionActivity::class.java}.EXTRA_SURVEY_ENTITY_ID"
        private val EXTRA_SURVEY_TITLE = "${SurveyQuestionActivity::class.java}.EXTRA_SURVEY_TITLE"
        private val EXTRA_SURVEY_MESSAGE = "${SurveyQuestionActivity::class.java}.EXTRA_SURVEY_MESSAGE"
        private val EXTRA_SURVEY_DELIVERED_TIME = "${SurveyQuestionActivity::class.java}.EXTRA_SURVEY_DELIVERED_TIME"
        private val EXTRA_SURVEY_IS_ENABLE_TO_RESPOND = "${SurveyQuestionActivity::class.java}.EXTRA_SURVEY_IS_ENABLE_TO_RESPOND"
        private val EXTRA_SHOW_FROM_SURVEY_LIST = "${SurveyQuestionActivity::class.java}.EXTRA_SHOW_FROM_SURVEY_LIST"

        fun newIntent(context: Context, entity: SurveyEntity, showFromList: Boolean = true) : Intent = Intent(context, SurveyQuestionActivity::class.java)
            .putExtra(EXTRA_SURVEY_ENTITY_ID, entity.id)
            .putExtra(EXTRA_SURVEY_TITLE, entity.title)
            .putExtra(EXTRA_SURVEY_MESSAGE, entity.message)
            .putExtra(EXTRA_SURVEY_DELIVERED_TIME, entity.deliveredTime)
            .putExtra(EXTRA_SURVEY_IS_ENABLE_TO_RESPOND, entity.isEnableToResponed(System.currentTimeMillis()))
            .putExtra(EXTRA_SHOW_FROM_SURVEY_LIST, showFromList)
    }
}