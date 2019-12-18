package kaist.iclab.abclogger.foreground.fragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.tasks.Tasks
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.background.collector.*
import kaist.iclab.abclogger.common.ABCException
import kaist.iclab.abclogger.common.NoNetworkAvailableException
import kaist.iclab.abclogger.common.NoParticipatedExperimentException
import kaist.iclab.abclogger.common.base.BaseFragment
import kaist.iclab.abclogger.common.type.LoadState
import kaist.iclab.abclogger.common.type.LoadStatus
import kaist.iclab.abclogger.common.util.*
import kaist.iclab.abclogger.communication.GrpcApi
import kaist.iclab.abclogger.data.FirestoreAccessor
import kaist.iclab.abclogger.data.entities.ParticipationEntity
import kaist.iclab.abclogger.foreground.dialog.SimpleMessageDialogFragment
import kaist.iclab.abclogger.foreground.dialog.YesNoDialogFragment
import kaist.iclab.abclogger.foreground.view.DataView
import kaist.iclab.abclogger.foreground.view.ExperimentItemView
// import kaist.iclab.abc.protos.ExperimentProtos
import kotlinx.android.synthetic.main.fragment_participated_experiment.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class ParticipatedExperimentFragment : BaseFragment() {
    private lateinit var experimentInfoView: ExperimentItemView
    private lateinit var experimenterView: View
    private lateinit var participationInfoView: View
    private lateinit var dataView: DataView

    private lateinit var essential: MutableLiveData<Pair<UInt, UInt>>
    private lateinit var participation: MutableLiveData<ParticipationEntity>
    private lateinit var experimenter: MutableLiveData<UInt>
    private lateinit var description: MutableLiveData<String>
    private lateinit var loadState: MutableLiveData<LoadState>

    interface OnLoadParticipatedExperimentListener {
        fun onLoadParticipatedExperiment(isParticipating: Boolean)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_participated_experiment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(view.context)
        setupObserver()
        setupListener()

        loadParticipatedExperiment()
    }

    private fun setupView(context: Context) {
        experimentInfoView = ExperimentItemView(context).apply {
            setPadding(
                resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
                0,
                resources.getDimensionPixelOffset(R.dimen.itemSpaceHorizontal),
                0
            )
        }
        sectionExperimentParticipated.setContentView(experimentInfoView)

        experimenterView = layoutInflater.inflate(R.layout.view_experiment_contact, scrollView, false)
        sectionExperimenter.setContentView(experimenterView)

        participationInfoView = layoutInflater.inflate(R.layout.view_participated_participation_info, scrollView, false)
        sectionParticipationInfo.setContentView(participationInfoView)

        dataView = DataView(context)
        sectionDataProvided.setContentView(dataView)
    }

    private fun setupListener() {
        fabDropout.setOnClickListener {
            val title = getString(R.string.dialog_title_dropout)
            val message = getString(R.string.dialog_message_dropout)
            val dialog = YesNoDialogFragment.newInstance(title, message)
            dialog.setOnDialogOptionSelectedListener { result -> if(result) dropOut() }
            dialog.show(fragmentManager, TAG)
        }
        swipeLayout.setOnRefreshListener { loadParticipatedExperiment() }
    }

    private fun setupObserver() {
        participation = MutableLiveData()
        essential = MutableLiveData()
        experimenter = MutableLiveData()
        description = MutableLiveData()
        loadState = MutableLiveData()

        essential.observe(this, Observer {
            if(it != null) {
                experimentInfoView.bindView(it.first, it.second)
            }
        })

        experimenter.observe(this, Observer {
            if(it != null) {
                // ViewUtils.bindContactView(experimenterView, it)
            }
        })

        participation.observe(this, Observer {
            if(it != null) {
                ViewUtils.bindParticipationInfo(participationInfoView, it, fragmentManager)
                dataView.setShowMore(null, null, null)
                dataView.setVisibilities(
                    requiresEventAndTraffic = it.requiresEventAndTraffic,
                    requiresLocationAndActivity = it.requiresLocationAndActivity,
                    requiresContentProviders = it.requiresContentProviders,
                    requiresAmbientSound = it.requiresAmbientSound,
                    requiresAppUsage = it.requiresAppUsage,
                    requiresNotification = it.requiresNotification,
                    requiresGoogleFitness = it.requiresGoogleFitness
                )
            }
        })

        description.observe(this, Observer {
            if(!TextUtils.isEmpty(it)) {
                val title = getString(R.string.label_experiment_description)
                val message = it!!

                sectionExperimentParticipated.setMoreClickListener {
                    SimpleMessageDialogFragment.newInstance(title, message).show(fragmentManager, TAG)
                }
            }
        })

        loadState.observe(this, Observer {
            if(it?.status == LoadStatus.SUCCESS) fabDropout.show() else fabDropout.hide()
            swipeLayout.isRefreshing = it?.status == LoadStatus.RUNNING
            scrollView.visibility = if(it?.status == LoadStatus.SUCCESS) View.VISIBLE else View.GONE
            txtError.visibility = if(it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            txtError.setText(
                when(it?.error) {
                    is ABCException -> it.error.getErrorStringRes()
                    else -> R.string.error_general_error
                }
            )
        })

        DeviceEventAndTrafficCollector.status.removeObservers(this)
        DeviceEventAndTrafficCollector.status.observe(this, Observer {
            dataView.setDescriptions(
                descEventAndTraffic = it?.state?.id?.let { res -> getString(res) } ?: getString(R.string.status_unknown)
            )
        })

        LocationAndActivityCollector.status.removeObservers(this)
        LocationAndActivityCollector.status.observe(this, Observer {
            dataView.setDescriptions(
                descLocationAndActivity = it?.state?.id?.let { res -> getString(res) } ?: getString(R.string.status_unknown)
            )
        })

        ContentProviderCollector.status.removeObservers(this)
        ContentProviderCollector.status.observe(this, Observer {
            dataView.setDescriptions(
                descContentProviders = it?.state?.id?.let { res -> getString(res) } ?: getString(R.string.status_unknown)
            )
        })

        AmbientSoundCollector.status.removeObservers(this)
        AmbientSoundCollector.status.observe(this, Observer {
            dataView.setDescriptions(
                descAmbientSound = it?.state?.id?.let { res -> getString(res) } ?: getString(R.string.status_unknown)
            )
        })

        AppUsageCollector.status.removeObservers(this)
        AppUsageCollector.status.observe(this, Observer {
            dataView.setDescriptions(
                descAppUsage = it?.state?.id?.let { res -> getString(res) } ?: getString(R.string.status_unknown)
            )
        })

        NotificationCollector.status.removeObservers(this)
        NotificationCollector.status.observe(this, Observer {
            dataView.setDescriptions(
                descNotification = it?.state?.id?.let { res -> getString(res) } ?: getString(R.string.status_unknown)
            )
        })

        GoogleFitnessCollector.status.removeObservers(this)
        GoogleFitnessCollector.status.observe(this, Observer {
            dataView.setDescriptions(
                descGoogleFitness = it?.state?.id?.let { res -> getString(res) } ?: getString(R.string.status_unknown)
            )
        })
    }

    private fun loadParticipatedExperiment() {
        val executor = Executors.newSingleThreadExecutor()
        loadState.postValue(LoadState.LOADING)

        Tasks.call(executor, Callable {
            val entity = ParticipationEntity.getParticipatedExperimentFromServer(requireContext())
            val experiment = null // GrpcApi.getExperiment(entity.experimentUuid)
            Pair(entity, experiment)
        }).addOnSuccessListener {
            (activity as? OnLoadParticipatedExperimentListener)?.onLoadParticipatedExperiment(true)

            participation.postValue(it.first)
            /*
            essential.postValue(Pair(it.second.basic, it.second.constraint))
            experimenter.postValue(it.second.experimenter)
            description.postValue(it.second.description)
            */

            loadState.postValue(LoadState.LOADED)
        }.addOnFailureListener {
            if(it is NoParticipatedExperimentException) {
                (activity as? OnLoadParticipatedExperimentListener)?.onLoadParticipatedExperiment(false)
            }
            loadState.postValue(LoadState.ERROR(it))
        }
    }

    private fun dropOut() {
        val executor = Executors.newSingleThreadExecutor()
        loadState.postValue(LoadState.LOADING)

        Tasks.call(executor, Callable {
            if(!NetworkUtils.isNetworkAvailable(requireContext())) throw NoNetworkAvailableException()

            val entityLocal= ParticipationEntity.getParticipatedExperimentFromLocal()

            FirestoreAccessor.delete(
                subjectEmail = entityLocal.subjectEmail,
                experimentUuid = entityLocal.experimentUuid
            )

            /*
            GrpcApi.dropOutExperiment(
                experimentUuid = entityLocal.experimentUuid,
                email = entityLocal.subjectEmail
            )
            */

            val entityServer = ParticipationEntity.getParticipatedExperimentFromServer(requireContext())
            val experiment = null // GrpcApi.getExperiment(entityServer.experimentUuid)
            Pair(entityServer, experiment)
        }).addOnSuccessListener {
            (activity as? OnLoadParticipatedExperimentListener)?.onLoadParticipatedExperiment(true)

            /*
            participation.postValue(it.first)
            essential.postValue(Pair(it.second.basic, it.second.constraint))
            experimenter.postValue(it.second.experimenter)
            description.postValue(it.second.description)
            */

            loadState.postValue(LoadState.LOADED)
        }.addOnFailureListener {
            if(it is NoParticipatedExperimentException) {
                (activity as? OnLoadParticipatedExperimentListener)?.onLoadParticipatedExperiment(false)
            }
            loadState.postValue(LoadState.ERROR(it))
        }
    }
}