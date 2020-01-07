package kaist.iclab.abclogger.ui.dialog

import android.app.Dialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.widget.NestedScrollView
import androidx.appcompat.app.AlertDialog
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Tasks
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.ABCException
import kaist.iclab.abclogger.common.type.LoadState
import kaist.iclab.abclogger.common.type.LoadStatus
import kaist.iclab.abclogger.sync.HttpApi
import kaist.iclab.abclogger.data.entities.ParticipationEntity
import kaist.iclab.abclogger.ui.view.SurveyView
import kaist.iclab.abclogger.Survey
import kotlinx.android.synthetic.main.fragment_survey_preview_dialog.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class SurveyPreviewDialogFragment: androidx.fragment.app.DialogFragment() {
    private lateinit var loadState: MutableLiveData<LoadState>
    private lateinit var survey: MutableLiveData<Survey?>

    companion object {
        private val ARG_URL = "${SurveyPreviewDialogFragment::class.java.canonicalName}.ARG_URL"

        fun newInstance(url: String?) = SurveyPreviewDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_URL, url ?: "")
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        loadState = MutableLiveData()
        survey = MutableLiveData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_survey_preview_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadState.observe(this, Observer {
            progress_bar.visibility = if(it?.status == LoadStatus.RUNNING) View.VISIBLE else View.OVER_SCROLL_ALWAYS
            txt_error.visibility = if(it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            scroll_view.visibility = if(it?.status == LoadStatus.SUCCESS) View.VISIBLE else View.GONE

            if(it?.error != null) txt_error.setText(
                if(it.error is ABCException) {
                    it.error.getErrorStringRes()
                } else {
                    R.string.error_general_error
                }
            )
        })

        survey.observe(this, Observer {
            if(it != null) surveyView.bindView(it, System.currentTimeMillis(), true, false)
        })

        if(TextUtils.isEmpty(arguments?.getString(ARG_URL))) {
            loadSurveyFromLocal()
        } else {
            loadSurveyFromUrl(arguments?.getString(ARG_URL))
        }
    }

    private fun loadSurveyFromLocal() {
        loadState.postValue(LoadState.LOADING)
        val executor = Executors.newSingleThreadExecutor()

        Tasks.call(executor, Callable {
            val entity = ParticipationEntity.getParticipatedExperimentFromLocal()
            Survey.parse(entity.survey)
        }).addOnSuccessListener {
            survey.postValue(it)
            loadState.postValue(LoadState.LOADED)
        }.addOnFailureListener {
            loadState.postValue(LoadState.ERROR(it))
        }
    }

    private fun loadSurveyFromUrl(url: String?) {
        loadState.postValue(LoadState.LOADING)

        val executor = Executors.newSingleThreadExecutor()

        HttpApi.request(requireContext(), url)
            .onSuccessTask(executor, SuccessContinuation<String, Survey> {
                Tasks.call { Survey.parse(it) }
            }).addOnSuccessListener {
                survey.postValue(it)
                loadState.postValue(LoadState.LOADED)
            }.addOnFailureListener {
                loadState.postValue(LoadState.ERROR(it))
            }

    }

    override fun onStart() {
        super.onStart()

        val progressBar = dialog.findViewById<ProgressBar>(R.id.progress_bar)
        val txtError = dialog.findViewById<TextView>(R.id.txt_error)
        val scrollView = dialog.findViewById<NestedScrollView>(R.id.scroll_view)
        val surveyView = dialog.findViewById<SurveyView>(R.id.surveyView)

        surveyView.setShowProgressBar(false)

        loadState.observe(this, Observer {
            progressBar?.visibility = if(it?.status == LoadStatus.RUNNING) View.VISIBLE else View.GONE
            txtError?.visibility = if(it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            scrollView?.visibility = if(it?.status == LoadStatus.SUCCESS) View.VISIBLE else View.GONE

            if(it?.error != null) txtError.setText(
                if(it.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
            )
        })

        survey.observe(this, Observer {
            if(it != null) surveyView?.bindView(it, System.currentTimeMillis(), true, false)
        })

        if(TextUtils.isEmpty(arguments?.getString(ARG_URL))) {
            loadSurveyFromLocal()
        } else {
            loadSurveyFromUrl(arguments?.getString(ARG_URL))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.label_survey_preview)
            .setCancelable(false)
            .setView(R.layout.fragment_survey_preview_dialog)
            .setNeutralButton(R.string.general_close) { _, _ -> dismiss() }.create()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)

    }
}