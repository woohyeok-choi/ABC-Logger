package kaist.iclab.abclogger.ui.question

import android.content.Context
import android.graphics.Typeface
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import kaist.iclab.abclogger.BR
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.databinding.LayoutSurveyResponseBinding
import kaist.iclab.abclogger.base.BaseViewModelFragment
import kaist.iclab.abclogger.commons.Formatter
import kaist.iclab.abclogger.commons.crossFade
import kaist.iclab.abclogger.commons.showSnackBar
import kaist.iclab.abclogger.ui.sharedViewNameForDeliveredTime
import kaist.iclab.abclogger.ui.sharedViewNameForMessage
import kaist.iclab.abclogger.ui.sharedViewNameForTitle
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SurveyResponseFragment : BaseViewModelFragment<LayoutSurveyResponseBinding, SurveyResponseViewModel>() {
    private val reactionTime = System.currentTimeMillis()
    private val args: SurveyResponseFragmentArgs by navArgs()
    private val shortAnimDuration by lazy { resources.getInteger(android.R.integer.config_shortAnimTime).toLong() }
    private val responseAdapter by lazy { SurveyResponseListAdapter() }

    override val layoutId: Int = R.layout.layout_survey_response
    override val viewModelVariable: Int = BR.viewModel
    override val viewModel: SurveyResponseViewModel by viewModel { parametersOf(this) }

    override fun afterViewCreated(context: Context) {
        viewBinding.contentContainer.visibility = View.GONE
        viewBinding.txtError.visibility = View.GONE
        viewBinding.progressBar.show()

        args.title.takeUnless { it.isNullOrBlank() }?.let { viewBinding.txtTitle.text = it }
        args.message.takeUnless { it.isNullOrBlank() }?.let { viewBinding.txtMessage.text = it }
        args.triggerTime.takeIf { it > 0 }?.let { viewBinding.txtTriggerTime.text = Formatter.formatSameDateTime(context, it, reactionTime) }
        sharedViewModel<> {  }
        val adapter = SurveyResponseListAdapter()
        viewBinding.recyclerView.adapter = adapter

        viewModel.getSurvey(args.entityId).observe(owner = this) { status ->
            if (status.isSuccess()) {
                val data = status.data ?: return@observe
                val (survey, responses) = data

                val isDisabled = survey.isDisabled(reactionTime)
                val isAltTextShown = survey.isAltTextShown(reactionTime)

                viewBinding.txtTitle.apply {
                    text = survey.title
                    setTextColor(ContextCompat.getColor(context, if (isDisabled) R.color.color_gray else R.color.color_dark_gray))
                    setTypeface(null, if (isDisabled) Typeface.BOLD else Typeface.NORMAL)
                }

                viewBinding.txtMessage.apply {
                    text = survey.message
                    setTextColor(ContextCompat.getColor(context, if (isDisabled) R.color.color_gray else R.color.color_dark_gray))
                }

                viewBinding.txtTriggerTime.apply {
                    text = Formatter.formatSameDateTime(context, survey.actualTriggerTime, reactionTime)
                    setTextColor(ContextCompat.getColor(context, if (isDisabled) R.color.color_gray else R.color.color_blue))
                    setTypeface(null, if (isDisabled) Typeface.BOLD else Typeface.NORMAL)
                }

                viewBinding.txtInstruction.apply {
                    text = survey.instruction
                    setTextColor(ContextCompat.getColor(context, if (isDisabled) R.color.color_gray else R.color.color_dark_gray))
                }

                adapter.bind(responses, !isDisabled, isAltTextShown)
                crossFade(viewBinding.contentContainer, viewBinding.progressBar, shortAnimDuration)
            } else if (status.isFailure()) {
                showSnackBar(viewBinding.root, status.error)
                crossFade(viewBinding.contentContainer, viewBinding.txtError, shortAnimDuration)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_survey_response, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }
            R.id.menu_save -> {
                viewModel.save(
                        entityId = args.entityId,
                        responses = responseAdapter.getResponses(),
                        reactionTime = reactionTime,
                        responseTime = System.currentTimeMillis()
                ).observe(owner = this) { status ->
                    if (status.isLoading()) {

                    }
                }
                findNavController().popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun beforeExecutePendingBindings() {
        innerViewBinding.txt
        this.


        viewModel.setting.observe(owner = this) { status ->
            const val EXTRA_SURVEY_TITLE = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_TITLE"
            const val EXTRA_SURVEY_MESSAGE = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_MESSAGE"
            const val EXTRA_SURVEY_DELIVERED_TIME = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_DELIVERED_TIME"
        }

        viewModel.setting.observe(this) { data ->
            if (data == null) return@observe
            val (questions, available, showAltText) = data

            responseAdapter.bind(questions = questions, isAvailable = available, isAltTextShown = showAltText)
            showOptionMenu = available
            invalidateOptionsMenu()
        }

        val showFromList: Boolean = intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)

        if (showFromList) {
            val entityId: Long = intent.getLongExtra(EXTRA_ENTITY_ID, 0)

            ViewCompat.setTransitionName(innerViewBinding.txtHeader, sharedViewNameForTitle(entityId))
            ViewCompat.setTransitionName(innerViewBinding.txtMessage, sharedViewNameForMessage(entityId))
            ViewCompat.setTransitionName(innerViewBinding.txtDeliveredTime, sharedViewNameForDeliveredTime(entityId))
        }
        innerViewBinding.recyclerView.adapter = responseAdapter
        innerViewBinding.recyclerView.itemAnimator = DefaultItemAnimator()
    }

    override fun navigateStore() {
        val showFromList: Boolean = intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)

        if (showFromList) supportFinishAfterTransition() else finish()
    }



    override fun onBackPressed() {
        val showFromList: Boolean = intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)

        if (showFromList) supportFinishAfterTransition() else finish()
    }




    companion object {
        const val EXTRA_ENTITY_ID = "${BuildConfig.APPLICATION_ID}.EXTRA_ENTITY_ID"
        const val EXTRA_SHOW_FROM_LIST = "${BuildConfig.APPLICATION_ID}.EXTRA_SHOW_FROM_LIST"
        const val EXTRA_SURVEY_TITLE = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_TITLE"
        const val EXTRA_SURVEY_MESSAGE = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_MESSAGE"
        const val EXTRA_SURVEY_DELIVERED_TIME = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_DELIVERED_TIME"
    }
}