package kaist.iclab.abclogger.ui.survey.response

import android.app.PendingIntent
import android.content.Context
import android.view.*
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.ui.base.BaseViewModelFragment
import kaist.iclab.abclogger.collector.survey.InternalResponseEntity
import kaist.iclab.abclogger.collector.survey.InternalSurveyEntity
import kaist.iclab.abclogger.commons.AbcError
import kaist.iclab.abclogger.commons.Formatter
import kaist.iclab.abclogger.commons.crossFade
import kaist.iclab.abclogger.commons.showSnackBar
import kaist.iclab.abclogger.core.NotificationRepository
import kaist.iclab.abclogger.databinding.FragmentSurveyResponseBinding
import kaist.iclab.abclogger.ui.State
import kaist.iclab.abclogger.ui.survey.sharedViewNameForDeliveredTime
import kaist.iclab.abclogger.ui.survey.sharedViewNameForMessage
import kaist.iclab.abclogger.ui.survey.sharedViewNameForTitle
import kaist.iclab.abclogger.ui.survey.SurveyViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.stateSharedViewModel

class SurveyResponseFragment : BaseViewModelFragment<FragmentSurveyResponseBinding, SurveyViewModel>() {
    override val viewModel: SurveyViewModel by stateSharedViewModel()

    private val args: SurveyResponseFragmentArgs by navArgs()
    private val cache: CachedResponse? by lazy { viewModel.loadState<CachedResponse>(KEY_RESPONSE) }
    private val adapter by lazy { SurveyResponseListAdapter() }

    private var isAlreadyAnswered: Boolean = false
    private var isResponseTimeout: Boolean = false

    private val entityId by lazy { if (args.restore) cache?.id ?: -1 else args.entityId }
    private val title by lazy { if (args.restore) cache?.title else args.title }
    private val message by lazy { if (args.restore) cache?.message else args.message }
    private val triggerTime by lazy { if (args.restore) cache?.triggerTime ?: 0 else args.triggerTime }
    private val reactionTime by lazy { if (args.restore) cache?.reactionTime ?: System.currentTimeMillis() else System.currentTimeMillis() }

    override fun getViewBinding(inflater: LayoutInflater): FragmentSurveyResponseBinding =
            FragmentSurveyResponseBinding.inflate(inflater)

    override fun initView(viewBinding: FragmentSurveyResponseBinding) {
        setHasOptionsMenu(true)

        NotificationRepository.cancelSurvey(requireContext(), entityId)

        ViewCompat.setTransitionName(viewBinding.txtTitle, sharedViewNameForTitle(entityId))
        ViewCompat.setTransitionName(viewBinding.txtMessage, sharedViewNameForMessage(entityId))
        ViewCompat.setTransitionName(viewBinding.txtTriggeredTime, sharedViewNameForDeliveredTime(entityId))

        viewBinding.txtTitle.text = title
        viewBinding.txtMessage.text = message
        viewBinding.txtTriggeredTime.text = Formatter.formatSameDateTime(requireContext(), triggerTime, reactionTime)

        viewBinding.txtError.visibility = View.GONE

        viewBinding.progressBar.visibility = View.VISIBLE

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
    }

    override fun afterViewCreated(viewBinding: FragmentSurveyResponseBinding) {
        lifecycleScope.launchWhenResumed {
            viewModel.get(id = entityId).collectLatest { state ->
                when (state) {
                    is State.Loading -> {
                        viewBinding.progressBar.visibility = View.VISIBLE
                        viewBinding.recyclerView.visibility = View.GONE
                        viewBinding.txtError.visibility = View.GONE
                    }
                    is State.Failure -> {
                        showSnackBar(viewBinding.root, AbcError.wrap(state.error).toSimpleString(requireContext()))

                        crossFade(
                                fadeIn = viewBinding.txtError,
                                fadeOut = viewBinding.progressBar
                        )
                    }
                    is State.Success<*> -> {
                        (state.data as? InternalSurveyEntity)?.let { entity ->
                            bind(
                                survey = entity,
                                responses = entity.responses,
                                viewBinding = viewBinding
                            )
                        }

                        crossFade(
                                fadeIn = viewBinding.recyclerView,
                                fadeOut = viewBinding.progressBar
                        )
                    }
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.survey_response, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    findNavController().navigateUp()
                    true
                }
                R.id.menu_save -> {
                    if (save()) findNavController().popBackStack()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun bind(survey: InternalSurveyEntity, responses: List<InternalResponseEntity>, viewBinding: FragmentSurveyResponseBinding) {
        val enabled = survey.isEnabled(reactionTime)
        val isAltTextShown = survey.isAltTextShown(reactionTime)

        viewBinding.txtTitle.apply {
            text = survey.title.text(isAltTextShown)
            isEnabled = enabled
        }
        viewBinding.txtMessage.apply {
            text = survey.message.text(isAltTextShown)
            isEnabled = enabled
        }
        viewBinding.txtTriggeredTime.apply {
            text = Formatter.formatSameDateTime(context, survey.actualTriggerTime, reactionTime)
            isEnabled = enabled
        }
        viewBinding.txtInstruction.apply {
            text = survey.instruction.text(isAltTextShown)
            isEnabled = enabled
        }
        adapter.bind(responses, enabled, isAltTextShown)

        isAlreadyAnswered = survey.isAnswered()
        isResponseTimeout = survey.isExpired(reactionTime)

        if (isAlreadyAnswered) {
            showSnackBar(viewBinding.root, R.string.survey_msg_already_answered)
        } else if (isResponseTimeout) {
            showSnackBar(viewBinding.root, R.string.survey_msg_response_timeout)
        }
    }

    private fun save(): Boolean {
        if (isAlreadyAnswered) {
            showSnackBar(viewBinding.root, R.string.survey_msg_already_answered)
            return false
        }

        if (isResponseTimeout) {
            showSnackBar(viewBinding.root, R.string.survey_msg_response_timeout)
            return false
        }


        if (adapter.responses.any { it.answer.isEmptyAnswer() }) {
            showSnackBar(viewBinding.root, R.string.survey_msg_answer_required)
            adapter.responses.forEach { response ->
                response.answer.isInvalid = response.answer.isEmptyAnswer()
            }
            return false
        }

        val responses = adapter.responses.toList()

        viewModel.saveState(KEY_RESPONSE, CachedResponse(
            id = entityId,
            triggerTime = triggerTime,
            reactionTime = reactionTime,
            title = title,
            message = message,
            responses = responses
        ))

        viewModel.post(
                id = entityId,
                responses = responses,
                reactionTime = reactionTime,
                responseTime = System.currentTimeMillis()
        )
        return true
    }

    companion object {
        fun pendingIntent(
            context: Context,
            entityId: Long,
            title: String,
            message: String,
            triggerTime: Long,
        ): PendingIntent =
            NavDeepLinkBuilder(context)
                .setGraph(R.navigation.main)
                .setDestination(R.id.survey_response)
                .setArguments(SurveyResponseFragmentArgs(
                    title = title,
                    message = message,
                    entityId = entityId,
                    triggerTime = triggerTime,
                    restore = false
                ).toBundle()).createPendingIntent()

        private const val KEY_RESPONSE = "KEY_RESPONSE"
    }
}