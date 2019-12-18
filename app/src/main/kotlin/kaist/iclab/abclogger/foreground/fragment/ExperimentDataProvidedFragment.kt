package kaist.iclab.abclogger.foreground.fragment

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_experiment_base.*
import kaist.iclab.abclogger.background.collector.*
import kaist.iclab.abclogger.common.*
import kaist.iclab.abclogger.common.type.LoadStatus
import kaist.iclab.abclogger.common.util.*
import kaist.iclab.abclogger.foreground.activity.ExperimentParticipationActivity
import kaist.iclab.abclogger.foreground.view.DataView

class ExperimentDataProvidedFragment : BaseFragment() {
    private lateinit var dataView: DataView
    private lateinit var viewModel: ExperimentParticipationActivity.ParticipationViewModel

    interface OnParticipationRequestListener {
        fun onParticipationRequested()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_experiment_base, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView(view.context)
        setupListener()
        setupObserver()

        bindView()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionGranted()
    }

    private fun setupView(context: Context) {
        dataView = DataView(context)
        sectionItem.setContentView(dataView)
    }

    private fun setupListener() {
        button.setOnClickListener {
            (activity as? OnParticipationRequestListener)?.onParticipationRequested()
        }
    }

    private fun setupObserver() {
        viewModel = ViewModelProviders.of(requireActivity()).get(ExperimentParticipationActivity.ParticipationViewModel::class.java)
        viewModel.step3State.removeObservers(this)
        viewModel.step3State.observe(this, Observer {
            dataView.isEnabled = it?.status != LoadStatus.RUNNING

            when(it?.status) {
                LoadStatus.RUNNING -> button.startWith()
                LoadStatus.SUCCESS -> button.succeedWith(false) {
                    ViewUtils.showToast(context, R.string.msg_start_experiment)
                    (activity as? ExperimentParticipationActivity)?.requestNextStep(this)
                }
                LoadStatus.FAILED -> button.failedWith {
                    ViewUtils.showToast(context, when(it.error) {
                        is ABCException -> it.error.getErrorStringRes()
                        is SecurityException -> FitnessDataSubscriptionFailedException().getErrorStringRes()
                        else -> R.string.error_general_error
                    })
                }
                else -> { }
            }
        })
    }

    private fun bindView() {
        dataView.setVisibilities(
            requiresEventAndTraffic = arguments?.getBoolean(ARG_REQUIRES_EVENT_AND_TRAFFIC) == true,
            requiresLocationAndActivity = arguments?.getBoolean(ARG_REQUIRES_LOCATION_AND_ACTIVITY) == true,
            requiresContentProviders = arguments?.getBoolean(ARG_REQUIRES_CONTENT_PROVIDERS) == true,
            requiresAmbientSound = arguments?.getBoolean(ARG_REQUIRES_AMBIENT_SOUND) == true,
            requiresAppUsage = arguments?.getBoolean(ARG_REQUIRES_APP_USAGE) == true,
            requiresNotification = arguments?.getBoolean(ARG_REQUIRES_NOTIFICATION) == true,
            requiresGoogleFitness = arguments?.getBoolean(ARG_REQUIRES_GOOGLE_FITNESS) == true
        )

        dataView.setShowMore(
            appUsageBlock = {
                ViewUtils.showToast(context, R.string.msg_set_app_usage)
                startActivity(AppUsageCollector.newIntentForSetup())
            },
            notificationBlock = {
                ViewUtils.showToast(context, R.string.msg_set_notification)
                startActivity(NotificationCollector.newIntentForSetup())
            },
            fitnessBlock = {
                Toast.makeText(context, R.string.msg_set_google_fitness, Toast.LENGTH_SHORT).show()
                GoogleFitnessCollector.newIntentForSetup(requireContext())
                    .addOnSuccessListener { intent ->
                        startActivity(intent)
                        ViewUtils.showToast(context, R.string.msg_set_google_fitness)
                    }.addOnFailureListener { _ -> ViewUtils.showToast(context, R.string.error_google_sign_out_error)}
            }
        )

        sectionItem.setHeader(getString(R.string.label_enter_data_provided))

        button.text = getString(R.string.btn_participate)
    }

    private fun updatePermissionGranted() {
        context?.let {
            dataView.setGranted(
                appUsage = AppUsageCollector.checkEnableToCollect(it),
                notification = NotificationCollector.checkEnableToCollect(it),
                fitness = GoogleFitnessCollector.checkEnableToCollect(it)
            )
        }
    }

    companion object {
        private val ARG_REQUIRES_EVENT_AND_TRAFFIC = "${ExperimentDataProvidedFragment::class.java.canonicalName}.ARG_REQUIRES_EVENT_AND_TRAFFIC"
        private val ARG_REQUIRES_LOCATION_AND_ACTIVITY = "${ExperimentDataProvidedFragment::class.java.canonicalName}.ARG_REQUIRES_LOCATION_AND_ACTIVITY"
        private val ARG_REQUIRES_CONTENT_PROVIDERS = "${ExperimentDataProvidedFragment::class.java.canonicalName}.ARG_REQUIRES_CONTENT_PROVIDERS"
        private val ARG_REQUIRES_AMBIENT_SOUND = "${ExperimentDataProvidedFragment::class.java.canonicalName}.ARG_REQUIRES_AMBIENT_SOUND"
        private val ARG_REQUIRES_NOTIFICATION = "${ExperimentDataProvidedFragment::class.java.canonicalName}.ARG_REQUIRES_NOTIFICATION"
        private val ARG_REQUIRES_APP_USAGE = "${ExperimentDataProvidedFragment::class.java.canonicalName}.ARG_REQUIRES_APP_USAGE"
        private val ARG_REQUIRES_GOOGLE_FITNESS = "${ExperimentDataProvidedFragment::class.java.canonicalName}.ARG_REQUIRES_GOOGLE_FITNESS"

        fun newInstance(requiresEventAndTraffic: Boolean,
                        requiresLocationAndActivity: Boolean,
                        requiresContentProviders : Boolean,
                        requiresAmbientSound: Boolean,
                        requiresAppUsage: Boolean,
                        requiresNotification: Boolean,
                        requiresGoogleFitness: Boolean) = ExperimentDataProvidedFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_REQUIRES_EVENT_AND_TRAFFIC, requiresEventAndTraffic)
                putBoolean(ARG_REQUIRES_LOCATION_AND_ACTIVITY, requiresLocationAndActivity)
                putBoolean(ARG_REQUIRES_CONTENT_PROVIDERS, requiresContentProviders)
                putBoolean(ARG_REQUIRES_AMBIENT_SOUND, requiresAmbientSound)
                putBoolean(ARG_REQUIRES_APP_USAGE, requiresAppUsage)
                putBoolean(ARG_REQUIRES_NOTIFICATION, requiresNotification)
                putBoolean(ARG_REQUIRES_GOOGLE_FITNESS, requiresGoogleFitness)
            }
        }
    }
}