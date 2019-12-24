package kaist.iclab.abclogger.foreground.activity

import android.Manifest
import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.transition.Explode
import android.transition.Slide
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.common.*
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.common.type.LoadState
import kaist.iclab.abclogger.common.type.YearMonthDay
import kaist.iclab.abclogger.common.util.FormatUtils
import kaist.iclab.abclogger.common.util.NetworkUtils
import kaist.iclab.abclogger.common.util.ViewUtils
import kaist.iclab.abclogger.communication.HttpApi
import kaist.iclab.abclogger.data.FirestoreAccessor
import kaist.iclab.abclogger.data.entities.ParticipationEntity
import kaist.iclab.abclogger.foreground.fragment.*
import kaist.iclab.abclogger.prefs
// import kaist.iclab.abc.protos.ExperimentProtos
import kaist.iclab.abclogger.survey.Survey
import kotlinx.android.synthetic.main.activity_container_with_toolbar.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class ExperimentParticipationActivity : BaseAppCompatActivity(),
    ExperimentSubjectInfoFragment.OnSubjectInfoSubmitListener,
    ExperimentOptionalInfoFragment.OnOptionalInfoListener,
    ExperimentDataProvidedFragment.OnParticipationRequestListener {

    private lateinit var participationInfo: ParticipationInfo
    private lateinit var viewModel: ParticipationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupContentView()
        setupActionBar()
        setupAnimation()
        setupData()

        supportFragmentManager.beginTransaction()
            .add(R.id.container, ExperimentSubjectInfoFragment.newInstance().apply {
                exitTransition = Slide(Gravity.START).setDuration(750).setInterpolator(DecelerateInterpolator())
                reenterTransition = Slide(Gravity.START).setDuration(750).setInterpolator(AccelerateInterpolator())
            })
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            android.R.id.home -> {
                supportFinishAfterTransition()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    fun requestNextStep(fragment: androidx.fragment.app.Fragment) {
        Log.d(TAG, "requestNextStep(${fragment::class.java.name})")
        val nextFragment = when(fragment) {
            is ExperimentSubjectInfoFragment -> ExperimentOptionalInfoFragment.newInstance().apply {
                enterTransition = Slide(Gravity.END).setDuration(750).setInterpolator(DecelerateInterpolator())
                reenterTransition = Slide(Gravity.START).setDuration(750).setInterpolator(AccelerateInterpolator())
                exitTransition = Slide(Gravity.START).setDuration(750).setInterpolator(DecelerateInterpolator())
            }
            is ExperimentOptionalInfoFragment -> ExperimentDataProvidedFragment.newInstance(
                    /*
                requiresEventAndTraffic = intent.getBooleanExtra(EXTRA_REQUIRES_EVENT_AND_TRAFFIC, false),
                requiresLocationAndActivity = intent.getBooleanExtra(EXTRA_REQUIRES_LOCATION_AND_ACTIVITY, false),
                requiresContentProviders =intent.getBooleanExtra(EXTRA_REQUIRES_CONTENT_PROVIDERS, false),
                requiresAmbientSound = intent.getBooleanExtra(EXTRA_REQUIRES_AMBIENT_SOUND, false),
                requiresAppUsage = intent.getBooleanExtra(EXTRA_REQUIRES_APP_USAGE, false),
                requiresNotification = intent.getBooleanExtra(EXTRA_REQUIRES_NOTIFICATION, false),
                requiresGoogleFitness = intent.getBooleanExtra(EXTRA_REQUIRES_GOOGLE_FITNESS, false)
                    */
                    requiresAmbientSound = prefs.requiresAmbientSound,
                    requiresAppUsage = prefs.requiresAppUsage,
                    requiresContentProviders = prefs.requiresContentProviders,
                    requiresEventAndTraffic = prefs.requiresEventAndTraffic,
                    requiresGoogleFitness = prefs.requiresGoogleFitness,
                    requiresLocationAndActivity = prefs.requiresLocationAndActivity,
                    requiresNotification = prefs.requiresNotification
            ).apply {
                enterTransition = Slide(Gravity.END).setDuration(750).setInterpolator(DecelerateInterpolator())
                reenterTransition = Slide(Gravity.START).setDuration(750).setInterpolator(AccelerateInterpolator())
                exitTransition = Slide(Gravity.START).setDuration(750).setInterpolator(DecelerateInterpolator())
            }
            else -> null
        }
        if(nextFragment != null) {
             supportFragmentManager.beginTransaction()
                .replace(R.id.container, nextFragment)
                .addToBackStack(null)
                .commit()
            viewModel.step1State.postValue(LoadState.INIT)
            viewModel.step2State.postValue(LoadState.INIT)
            viewModel.step3State.postValue(LoadState.INIT)
        } else {
            setResult(Activity.RESULT_OK)
            supportFinishAfterTransition()
        }
    }

    override fun onBackPressed() {
        if(supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        } else {
            supportFinishAfterTransition()
        }
    }

    override fun onSubjectInfoSubmitted(phoneNumber: String?, name: String?, affiliation: String?, birthDate: String?, isMale: String?) {
        Log.d(TAG, "onSubjectInfoSubmitted")
        viewModel.step1State.postValue(LoadState.LOADING)

        Tasks.call(Executors.newSingleThreadExecutor(), Callable {
            val isValid = FormatUtils.validatePhoneNumber(phoneNumber) &&
                FormatUtils.validateNonEmpty(name) &&
                FormatUtils.validateNonEmpty(affiliation) &&
                (isMale == getString(R.string.general_gender_male) || isMale == getString(R.string.general_gender_female)) &&
                YearMonthDay.fromString(birthDate) != null
            if(!isValid) throw InvalidContentException()

            participationInfo.name = name!!
            participationInfo.phoneNumber = phoneNumber!!
            participationInfo.affiliation = affiliation!!
            participationInfo.birthDate = YearMonthDay.fromString(birthDate)!!
            participationInfo.isMale = isMale == getString(R.string.general_gender_male)

        }).addOnSuccessListener {
            viewModel.step1State.postValue(LoadState.LOADED)
        }.addOnFailureListener {
            viewModel.step1State.postValue(LoadState.ERROR(it))
        }
    }

    override fun onOptionalInfoSubmitted(groupInfo: String?, surveyUrl: String?) {
        viewModel.step2State.postValue(LoadState.LOADING)
        Log.d(TAG, "$surveyUrl")

        participationInfo.group = groupInfo ?: ""

        if(TextUtils.isEmpty(surveyUrl)) {
            Log.d(TAG, "isEmpty!")
            Tasks.call(Executors.newSingleThreadExecutor(), Callable {
                participationInfo.survey = ""
            })
        } else {
            Log.d(TAG, "isNotEmpty!")
            HttpApi.request(this, surveyUrl)
                .onSuccessTask(Executors.newSingleThreadExecutor(), SuccessContinuation<String, String> {
                    Survey.parse(it)
                    participationInfo.survey = it ?: ""
                    Tasks.call{ it }
                })
        }.addOnSuccessListener {
            viewModel.step2State.postValue(LoadState.LOADED)
        }.addOnFailureListener {
            viewModel.step2State.postValue(LoadState.ERROR(it))
        }
    }

    override fun onParticipationRequested() {
        val requiredPermissions = listOfNotNull(
                /*
            if (intent.getBooleanExtra(EXTRA_REQUIRES_LOCATION_AND_ACTIVITY, false)) LocationAndActivityCollector.REQUIRED_PERMISSIONS else null,
            if (intent.getBooleanExtra(EXTRA_REQUIRES_CONTENT_PROVIDERS, false)) ContentProviderCollector.REQUIRED_PERMISSIONS else null,
            if (intent.getBooleanExtra(EXTRA_REQUIRES_AMBIENT_SOUND, false)) AmbientSoundCollector.REQUIRED_PERMISSIONS else null,
            if (intent.getBooleanExtra(EXTRA_REQUIRES_GOOGLE_FITNESS, false)) GoogleFitnessCollector.REQUIRED_PERMISSIONS else null,
            listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                */
                if (prefs.requiresLocationAndActivity) LocationAndActivityCollector.REQUIRED_PERMISSIONS else null,
                if (prefs.requiresContentProviders) ContentProviderCollector.REQUIRED_PERMISSIONS else null,
                if (prefs.requiresAmbientSound) AmbientSoundCollector.REQUIRED_PERMISSIONS else null,
                if (prefs.requiresGoogleFitness) GoogleFitnessCollector.REQUIRED_PERMISSIONS else null,
                listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ).flatten().toSet().toTypedArray()

        ViewUtils.showPermissionDialog(this, requiredPermissions,
            {
                viewModel.step3State.postValue(LoadState.LOADING)
                val executor = Executors.newSingleThreadExecutor()

                Tasks.call(executor, Callable {
                    if(!NetworkUtils.isNetworkAvailable(this)) throw NoNetworkAvailableException()
                    /*
                    if(intent.getBooleanExtra(EXTRA_REQUIRES_APP_USAGE, false) && !AppUsageCollector.checkEnableToCollect(this)) throw AppUsageDeniedException()
                    if(intent.getBooleanExtra(EXTRA_REQUIRES_NOTIFICATION, false) && !NotificationCollector.checkEnableToCollect(this)) throw NotificationAccessDeniedException()
                    if(intent.getBooleanExtra(EXTRA_REQUIRES_GOOGLE_FITNESS, false) && !GoogleFitnessCollector.checkEnableToCollect(this)) throw GoogleFitnessDeniedException()
                    */
                    if(prefs.requiresAppUsage && !AppUsageCollector.checkEnableToCollect(this)) throw AppUsageDeniedException()
                    if(prefs.requiresNotification && !NotificationCollector.checkEnableToCollect(this)) throw NotificationAccessDeniedException()
                    if(prefs.requiresGoogleFitness && !GoogleFitnessCollector.checkEnableToCollect(this)) throw GoogleFitnessDeniedException()
                }).onSuccessTask(executor, SuccessContinuation <Unit, Void> {
                    if(prefs.requiresGoogleFitness) GoogleFitnessCollector.subscribeFitnessData(this).continueWith { _ -> } else Tasks.call {  }
                    val email = FirebaseAuth.getInstance().currentUser?.email ?: throw NoSignedAccountException()

                    /*
                    GrpcApi.participateExperiment(
                        email = email,
                        birthDate = participationInfo.birthDate,
                        isMale = participationInfo.isMale,
                        phoneNumber = participationInfo.phoneNumber,
                        name = participationInfo.name,
                        affiliation = participationInfo.affiliation,
                        experimentUuid = intent.getStringExtra(EXTRA_EXPERIMENT_UUID),
                        experimentGroup = participationInfo.group,
                        survey = participationInfo.survey
                    )
                    */


                    ParticipationEntity.getParticipatedExperimentFromServer(this)
                    FirestoreAccessor.setOrUpdate(
                        subjectEmail = email,
                        experimentUuid = intent.getStringExtra(EXTRA_EXPERIMENT_UUID),
                        data = FirestoreAccessor.ExperimentData()
                    )
                }).addOnSuccessListener {
                    viewModel.step3State.postValue(LoadState.LOADED)
                }.addOnFailureListener {
                    viewModel.step3State.postValue(LoadState.ERROR(it))
                }
            },
            { viewModel.step3State.postValue(LoadState.ERROR(PermissionDeniedException())) }
        )
    }

    private fun setupContentView() {
        setContentView(R.layout.activity_container_with_toolbar)
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.activity_title_experiment_participation)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupAnimation() {
        window.enterTransition = Explode().setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .excludeTarget(toolbar, true)
    }

    private fun setupData() {
        participationInfo = ParticipationInfo(uuid = intent.getStringExtra(EXTRA_EXPERIMENT_UUID))
        viewModel = ViewModelProviders.of(this).get(ParticipationViewModel::class.java)
    }

    data class ParticipationInfo (
        var uuid: String,
        var birthDate: YearMonthDay = YearMonthDay.empty(),
        var isMale: Boolean = false,
        var phoneNumber: String = "",
        var name: String = "",
        var affiliation: String = "",
        var survey: String = "",
        var group: String = ""
    )

    class ParticipationViewModel: ViewModel() {
        val step1State = MutableLiveData<LoadState>()
        val step2State = MutableLiveData<LoadState>()
        val step3State = MutableLiveData<LoadState>()
    }

    companion object {
        private val EXTRA_EXPERIMENT_UUID = "${ExperimentParticipationActivity::class.java.canonicalName}.EXTRA_EXPERIMENT_UUID"
        private val EXTRA_REQUIRES_EVENT_AND_TRAFFIC = "${ExperimentParticipationActivity::class.java.canonicalName}.EXTRA_REQUIRES_EVENT_AND_TRAFFIC"
        private val EXTRA_REQUIRES_LOCATION_AND_ACTIVITY = "${ExperimentParticipationActivity::class.java.canonicalName}.EXTRA_REQUIRES_LOCATION_AND_ACTIVITY"
        private val EXTRA_REQUIRES_CONTENT_PROVIDERS = "${ExperimentParticipationActivity::class.java.canonicalName}.EXTRA_REQUIRES_CONTENT_PROVIDERS"
        private val EXTRA_REQUIRES_AMBIENT_SOUND = "${ExperimentParticipationActivity::class.java.canonicalName}.EXTRA_REQUIRES_AMBIENT_SOUND"
        private val EXTRA_REQUIRES_NOTIFICATION = "${ExperimentParticipationActivity::class.java.canonicalName}.EXTRA_REQUIRES_NOTIFICATION"
        private val EXTRA_REQUIRES_APP_USAGE = "${ExperimentParticipationActivity::class.java.canonicalName}.EXTRA_REQUIRES_APP_USAGE"
        private val EXTRA_REQUIRES_GOOGLE_FITNESS = "${ExperimentParticipationActivity::class.java.canonicalName}.EXTRA_REQUIRES_GOOGLE_FITNESS"


        fun newIntent(context: Context, experiment :Unit) : Intent = Intent(context, ExperimentParticipationActivity::class.java)
                /*
            .putExtra(EXTRA_EXPERIMENT_UUID, experiment.basic.uuid)
            .putExtra(EXTRA_REQUIRES_EVENT_AND_TRAFFIC, experiment.constraint.requiresDeviceEventAndTraffic)
            .putExtra(EXTRA_REQUIRES_LOCATION_AND_ACTIVITY, experiment.constraint.requiresLocationAndActivity)
            .putExtra(EXTRA_REQUIRES_CONTENT_PROVIDERS, experiment.constraint.requiresContentProviders)
            .putExtra(EXTRA_REQUIRES_AMBIENT_SOUND, experiment.constraint.requiresAmbientSound)
            .putExtra(EXTRA_REQUIRES_APP_USAGE, experiment.constraint.requiresAppUsage)
            .putExtra(EXTRA_REQUIRES_NOTIFICATION, experiment.constraint.requiresNotificationReceived)
            .putExtra(EXTRA_REQUIRES_GOOGLE_FITNESS, experiment.constraint.requiresGoogleFitness)
        */
    }
}