package kaist.iclab.abclogger.foreground.activity

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import android.text.TextUtils
import android.transition.Fade
import android.transition.Transition
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.background.ABCPlatform
import kaist.iclab.abclogger.common.*
import kaist.iclab.abclogger.common.base.BaseAppCompatActivity
import kaist.iclab.abclogger.common.type.HourMin
import kaist.iclab.abclogger.common.type.LoadState
import kaist.iclab.abclogger.common.type.LoadStatus
import kaist.iclab.abclogger.common.util.*
import kaist.iclab.abclogger.communication.GrpcApi
import kaist.iclab.abclogger.data.PreferenceAccessor
import kaist.iclab.abclogger.foreground.view.DataView
//import kaist.iclab.abc.protos.ExperimentProtos
import kotlinx.android.synthetic.main.activity_container_with_toolbar.*
import kotlinx.android.synthetic.main.activity_experiment_detail.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class ExperimentDetailActivity : BaseAppCompatActivity() {
    //private lateinit var experiment: MutableLiveData<ExperimentProtos.ExperimentFull>
    private lateinit var experiment: MutableLiveData<Unit>
    private lateinit var initLoadState: MutableLiveData<LoadState>
    private lateinit var loadState: MutableLiveData<LoadState>

    private lateinit var descriptionView: View
    private lateinit var dataView: DataView
    private lateinit var contactView: View

    private var isInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupContentView()
        setupActionBar()
        setupAnimation()
        setupObservers()
        setupListener()

        bindView()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                supportFinishAfterTransition()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onBackPressed() {
        supportFinishAfterTransition()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_FOR_PARTICIPATION) {
            Log.d(TAG, "onActivityResult ()")
            loadExperiment(false)
            ABCPlatform.start(this)
        }
    }

    private fun setupContentView() {
        setContentView(R.layout.activity_container_with_toolbar)
        container.addView(layoutInflater.inflate(R.layout.activity_experiment_detail, container, false))

        descriptionView = layoutInflater.inflate(R.layout.view_experiment_description, scrollView, false)
        sectionExperimentDescription.setContentView(descriptionView)

        dataView = DataView(this)
        sectionDataRequires.setContentView(dataView)

        contactView = layoutInflater.inflate(R.layout.view_experiment_contact, scrollView, false)
        sectionExperimentContact.setContentView(contactView)
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.activity_title_experiment_detail)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupObservers() {
        /*
        experiment = MutableLiveData()
        initLoadState = MutableLiveData()
        loadState = MutableLiveData()

        experiment.observe(this, Observer {
            if(it != null) {
                itemExperimentView.bindView(
                    basic = it.basic,
                    constraint = it.constraint
                )
                ViewUtils.bindExperimentDescriptionView(descriptionView, it.description)

                dataView.setShowMore(null, null, null)
                dataView.setVisibilities(
                    requiresEventAndTraffic = it.constraint.requiresDeviceEventAndTraffic,
                    requiresLocationAndActivity = it.constraint.requiresLocationAndActivity,
                    requiresContentProviders = it.constraint.requiresContentProviders,
                    requiresAmbientSound = it.constraint.requiresAmbientSound,
                    requiresAppUsage = it.constraint.requiresAppUsage,
                    requiresNotification = it.constraint.requiresNotificationReceived,
                    requiresGoogleFitness = it.constraint.requiresGoogleFitness
                )
                ViewUtils.bindContactView(contactView, it.experimenter)

                val isOpened =
                    it.basic.deadlineTimestamp > System.currentTimeMillis() &&
                        it.constraint.isOpened &&
                        it.basic.currentSubjects < it.basic.maxSubjects
                val isFree = !PreferenceAccessor.getInstance(this).isParticipated

                val msg = when {
                    !isOpened -> R.string.error_closed_experiment
                    !isFree ->  R.string.error_already_participate
                    else -> null
                }
                if(msg != null) {
                    ViewUtils.showSnackBar(rootContainer, msg)
                    fabParticipate.hide()
                } else {
                    fabParticipate.show()
                }
            }
        })

        initLoadState.observe(this, Observer {
            progressBar.visibility = if(it?.status == LoadStatus.RUNNING) View.VISIBLE else View.GONE
            lazyContainer.visibility = if(it?.status == LoadStatus.SUCCESS) View.VISIBLE else View.GONE
            txtError.visibility = if(it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            if (it?.error != null) txtError.setText(
                if (it.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
            )
        })

        loadState.observe(this, Observer {
            swipeLayout.isRefreshing = it?.status == LoadStatus.RUNNING
            scrollView.visibility = if(it?.status == LoadStatus.SUCCESS) View.VISIBLE else View.GONE
            txtError.visibility = if(it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            if (it?.error != null) txtError.setText(
                if (it.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
            )
        })
        */
    }

    private fun bindView() {
        itemExperimentView.bindView(
            isOpened = intent.getBooleanExtra(EXTRA_IS_OPENED, false),
            deadlineTimestamp = intent.getLongExtra(EXTRA_DEADLINE_TIMESTAMP, System.currentTimeMillis()),
            title = intent.getStringExtra(EXTRA_TITLE),
            affiliation = intent.getStringExtra(EXTRA_AFFILIATION),
            registeredTimestamp = intent.getLongExtra(EXTRA_REGISTERED_TIMESTAMP, System.currentTimeMillis()),
            currentSubjects = intent.getIntExtra(EXTRA_CURRENT_SUBJECTS, 0),
            maxSubjects = intent.getIntExtra(EXTRA_MAX_SUBJECTS, 0),
            durationInHour = intent.getLongExtra(EXTRA_DURATION_IN_HOUR, 0),
            compensation = intent.getStringExtra(EXTRA_COMPENSATION),
            dailyStartTime = intent.getParcelableExtra(EXTRA_DAILY_START_TIME),
            dailyEndTime= intent.getParcelableExtra(EXTRA_DAILY_END_TIME),
            containsWeekend = intent.getBooleanExtra(EXTRA_CONTAINS_WEEKEND, false)
        )
    }

    private fun setupAnimation() {
        ViewCompat.setTransitionName(itemExperimentView, intent.getStringExtra(EXTRA_EXPERIMENT_UUID))
        window.sharedElementEnterTransition.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(p0: Transition?) {
                if(!isInitialized) loadExperiment(true)
                isInitialized = true
                Log.d(TAG, "onTransitionEnd()")
            }

            override fun onTransitionResume(p0: Transition?) { }

            override fun onTransitionPause(p0: Transition?) { }

            override fun onTransitionCancel(p0: Transition?) { }

            override fun onTransitionStart(p0: Transition?) { }
        })
        window.reenterTransition = Fade().setDuration(750)
    }

    private fun setupListener() {
        fabParticipate.setOnClickListener {
            val exp = experiment.value
            if(exp != null) {
                startActivityForResult(
                    ExperimentParticipationActivity.newIntent(this, exp),
                    REQUEST_CODE_FOR_PARTICIPATION,
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle())
            } else {
                ViewUtils.showToast(this, R.string.error_wait_for_loading)
            }
        }
        swipeLayout.setOnRefreshListener { loadExperiment(false) }
    }

    private fun loadExperiment(isFirstTime: Boolean) {
        if(isFirstTime) {
            initLoadState.postValue(LoadState.LOADING)
        } else {
            loadState.postValue(LoadState.LOADING)
        }

        val uuid = intent.getStringExtra(EXTRA_EXPERIMENT_UUID)
        val email = FirebaseAuth.getInstance().currentUser?.email

        Tasks.call(Executors.newSingleThreadExecutor(), Callable {
            if(!NetworkUtils.isNetworkAvailable(this)) throw NoNetworkAvailableException()
            if(TextUtils.isEmpty(uuid)) throw NoCorrespondingExperimentException()
            if(TextUtils.isEmpty(email)) throw NoSignedAccountException()
            //GrpcApi.getExperiment(uuid!!)
        }).addOnSuccessListener {
            experiment.postValue(it)

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

    companion object {
        private const val REQUEST_CODE_FOR_PARTICIPATION = 0x00000005

        private val EXTRA_EXPERIMENT_UUID = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_EXPERIMENT_UUID"
        private val EXTRA_IS_OPENED = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_IS_OPENED"
        private val EXTRA_DEADLINE_TIMESTAMP = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_DEADLINE_TIMESTAMP"
        private val EXTRA_TITLE = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_TITLE"
        private val EXTRA_AFFILIATION = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_AFFILIATION"
        private val EXTRA_REGISTERED_TIMESTAMP = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_REGISTERED_TIMESTAMP"
        private val EXTRA_CURRENT_SUBJECTS = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_CURRENT_SUBJECTS"
        private val EXTRA_MAX_SUBJECTS = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_MAX_SUBJECTS"
        private val EXTRA_DURATION_IN_HOUR = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_DURATION_IN_HOUR"
        private val EXTRA_COMPENSATION = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_COMPENSATION"
        private val EXTRA_DAILY_START_TIME = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_DAILY_START_TIME"
        private val EXTRA_DAILY_END_TIME = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_DAILY_END_TIME"
        private val EXTRA_CONTAINS_WEEKEND = "${ExperimentDetailActivity::class.java.canonicalName}.EXTRA_CONTAINS_WEEKEND"

        /*
        fun newIntent(context: Context, essential: ExperimentProtos.ExperimentEssential): Intent = Intent(context, ExperimentDetailActivity::class.java)
                .putExtra(EXTRA_EXPERIMENT_UUID, essential.basic.uuid)
                .putExtra(EXTRA_IS_OPENED, essential.constraint.isOpened)
                .putExtra(EXTRA_DEADLINE_TIMESTAMP, essential.basic.deadlineTimestamp)
                .putExtra(EXTRA_TITLE, essential.basic.title)
                .putExtra(EXTRA_AFFILIATION, essential.basic.affiliation)
                .putExtra(EXTRA_REGISTERED_TIMESTAMP, essential.basic.registeredTimestamp)
                .putExtra(EXTRA_CURRENT_SUBJECTS, essential.basic.currentSubjects)
                .putExtra(EXTRA_MAX_SUBJECTS, essential.basic.maxSubjects)
                .putExtra(EXTRA_DURATION_IN_HOUR, essential.constraint.durationInHour)
                .putExtra(EXTRA_COMPENSATION, essential.basic.compensation)
                .putExtra(EXTRA_DAILY_START_TIME, HourMin(essential.constraint.dailyStartTime.hour, essential.constraint.dailyStartTime.min))
                .putExtra(EXTRA_DAILY_END_TIME, HourMin(essential.constraint.dailyEndTime.hour, essential.constraint.dailyEndTime.min))
                .putExtra(EXTRA_CONTAINS_WEEKEND, essential.constraint.containsWeekend)
        */
    }
}
