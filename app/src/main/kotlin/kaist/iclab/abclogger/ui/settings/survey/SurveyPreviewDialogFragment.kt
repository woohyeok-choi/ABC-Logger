package kaist.iclab.abclogger.ui.settings.survey

import android.app.Dialog
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.collector.survey.InternalAnswer
import kaist.iclab.abclogger.collector.survey.InternalResponseEntity
import kaist.iclab.abclogger.commons.AbcError
import kaist.iclab.abclogger.commons.Formatter
import kaist.iclab.abclogger.commons.HttpRequestError
import kaist.iclab.abclogger.commons.crossFade
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.databinding.FragmentSurveyPreviewBinding
import kaist.iclab.abclogger.structure.survey.Option
import kaist.iclab.abclogger.structure.survey.Survey
import kaist.iclab.abclogger.ui.survey.response.SurveyResponseListAdapter


class SurveyPreviewDialogFragment : DialogFragment(), CompoundButton.OnCheckedChangeListener {
    private val adapter by lazy { SurveyResponseListAdapter() }
    private val survey by lazy { arguments?.getString(ARG_SURVEY)?.let { Survey.fromJson(it) } }
    private lateinit var viewBinding: FragmentSurveyPreviewBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = FragmentSurveyPreviewBinding.inflate(layoutInflater)

        viewBinding.recyclerView.adapter = adapter
        viewBinding.recyclerView.itemAnimator = DefaultItemAnimator()
        viewBinding.recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                rv.requestFocusFromTouch()
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            }

        })
        viewBinding.swiAltText.setOnCheckedChangeListener(this)

        return MaterialAlertDialogBuilder(requireContext())
            .setCancelable(true)
            .setView(viewBinding.root)
            .create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        )

        lifecycleScope.launchWhenCreated {
            viewBinding.progressBar.visibility = View.VISIBLE
            viewBinding.container.visibility = View.GONE
            viewBinding.txtError.visibility = View.GONE

            if (survey == null) {
                viewBinding.txtError.text =
                    AbcError.wrap(HttpRequestError.invalidJsonFormat())
                        .toSimpleString(requireContext())

                crossFade(
                    fadeIn = viewBinding.txtError,
                    fadeOut = viewBinding.progressBar,
                )
            } else {
                survey?.let {
                    bind(survey = it, isAltTextShown = false)
                }
                crossFade(
                    fadeIn = viewBinding.container,
                    fadeOut = viewBinding.progressBar,
                )
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        survey?.let {
            bind(survey = it, isAltTextShown = isChecked)
        }
    }

    private fun bind(survey: Survey, isAltTextShown: Boolean) {
        viewBinding.txtTitle.text = survey.title.text(isAltTextShown)
        viewBinding.txtMessage.text = survey.message.text(isAltTextShown)
        viewBinding.txtTriggeredTime.text = Formatter.formatSameDateTime(
            requireContext(),
            System.currentTimeMillis(),
            System.currentTimeMillis()
        )
        viewBinding.txtInstruction.text = survey.instruction.text(isAltTextShown)

        val responses = survey.question.mapIndexed { index, question ->
            InternalResponseEntity(
                index = index,
                question = question,
                answer = InternalAnswer(question.option.type != Option.Type.CHECK_BOX)
            )
        }
        adapter.bind(responses, true, isAltTextShown)
    }

    companion object {
        private const val ARG_SURVEY = "${BuildConfig.APPLICATION_ID}.ui.settings.survey.ARG_SURVEY"

        fun newInstance(survey: Survey) = SurveyPreviewDialogFragment().apply {
            arguments = bundleOf(ARG_SURVEY to survey.toJson())
        }
    }
}