package kaist.iclab.abclogger.collector.survey

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.observe
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.databinding.FragmentSurveyPreviewBinding
import kaist.iclab.abclogger.ui.Status
import org.koin.androidx.viewmodel.ext.android.viewModel

class SurveyPreviewDialogFragment : DialogFragment() {
    private val viewModel: SurveyPreviewViewModel by viewModel()
    private lateinit var dataBinding: FragmentSurveyPreviewBinding
    private lateinit var url: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        url = arguments?.getString(ARG_SURVEY_URL, "") ?: ""
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dataBinding = DataBindingUtil.inflate(
                layoutInflater,
                R.layout.fragment_survey_preview,
                null,
                false
        )
        return AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_title_survey_preview)
                .setCancelable(false)
                .setView(dataBinding.root)
                .setNeutralButton(R.string.general_close) { _, _ -> dismiss() }.create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding.viewModel = viewModel
        dataBinding.lifecycleOwner = this

        viewModel.loadStatus.observe(this) { status ->
            when(status.state) {
                Status.STATE_LOADING -> dataBinding.loadProgressBar.show()
                Status.STATE_SUCCESS -> dataBinding.loadProgressBar.hide()
                Status.STATE_FAILURE -> {
                    dataBinding.loadProgressBar.hide()
                    showToast(status.error, false)
                }
            }
        }

        viewModel.load(url)
    }

    companion object {
        private const val ARG_SURVEY_URL = "${BuildConfig.APPLICATION_ID}.ARG_SURVEY_URL"

        fun showDialog(fragmentManager: FragmentManager, url: String?) {
            SurveyPreviewDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SURVEY_URL, url)
                }
            }.show(fragmentManager, "${SurveyPreviewDialogFragment::class.java.canonicalName}")
        }
    }
}