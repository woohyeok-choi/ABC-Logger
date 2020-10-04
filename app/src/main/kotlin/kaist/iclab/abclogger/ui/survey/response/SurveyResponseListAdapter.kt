package kaist.iclab.abclogger.ui.question

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.survey.Question
import kaist.iclab.abclogger.collector.survey.ResponseEntity
import kaist.iclab.abclogger.databinding.*

class SurveyResponseListAdapter : RecyclerView.Adapter<SurveyResponseListAdapter.ViewHolder>() {
    private var responses: Array<ResponseEntity> = arrayOf()
    private var isAltTextShown: Boolean = false

    var isDisabled: Boolean = false

    fun bind(responses: Collection<ResponseEntity>, isDisabled: Boolean, isAltTextShown: Boolean) {
        this.responses = responses.sortedBy { it.index }.toTypedArray()
        this.isDisabled = isDisabled
        this.isAltTextShown = isAltTextShown

        notifyDataSetChanged()
    }

    fun getResponses() : List<ResponseEntity> = responses.toList()

    override fun getItemViewType(position: Int): Int =
            responses.getOrNull(position)?.question?.type?.ordinal ?: Question.Type.NONE.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            when (viewType) {
                Question.Type.CHECK_BOX.ordinal -> CheckResponseViewHolder(
                        DataBindingUtil.inflate(
                                LayoutInflater.from(parent.context),
                                R.layout.item_response_check,
                                parent,
                                false
                        )
                )
                Question.Type.RADIO_BUTTON.ordinal -> RadioResponseViewHolder(
                        DataBindingUtil.inflate(
                                LayoutInflater.from(parent.context),
                                R.layout.item_response_radio,
                                parent,
                                false
                        )
                )
                Question.Type.FREE_TEXT.ordinal -> FreeTextResponseViewHolder(
                        DataBindingUtil.inflate(
                                LayoutInflater.from(parent.context),
                                R.layout.item_response_free_text,
                                parent,
                                false
                        )
                )
                Question.Type.LINEAR_SCALE.ordinal -> LinearScaleResponseViewHolder(
                        DataBindingUtil.inflate(
                                LayoutInflater.from(parent.context),
                                R.layout.item_response_linear_scale,
                                parent,
                                false
                        )
                )
                Question.Type.DROPDOWN.ordinal -> DropdownResponseViewHolder(
                        DataBindingUtil.inflate(
                                LayoutInflater.from(parent.context),
                                R.layout.item_response_dropdown,
                                parent,
                                false
                        )
                )
                else -> NonResponseViewHolder(View(parent.context))
            }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val response = responses.getOrNull(position) ?: return
        holder.onBind(response, isDisabled, isAltTextShown)
    }

    override fun getItemCount(): Int = responses.size

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun onBind(response: ResponseEntity, isDisabled: Boolean, showAltText: Boolean)
    }

    data class NonResponseViewHolder(private val view: View): ViewHolder(view) {
        override fun onBind(response: ResponseEntity, isDisabled: Boolean, showAltText: Boolean) {
            view.visibility = View.GONE
        }
    }

    data class CheckResponseViewHolder(private val binding: ItemResponseCheckBinding) : ViewHolder(binding.root) {
        override fun onBind(response: ResponseEntity, isDisabled: Boolean, showAltText: Boolean) {
            binding.response = response
            binding.isDisabled = isDisabled
            binding.isShowAltText = showAltText
        }
    }

    data class DropdownResponseViewHolder(private val binding: ItemResponseDropdownBinding) : ViewHolder(binding.root) {
        override fun onBind(response: ResponseEntity, isDisabled: Boolean, showAltText: Boolean) {
            binding.response = response
            binding.isDisabled = isDisabled
            binding.isShowAltText = showAltText
        }
    }

    data class FreeTextResponseViewHolder(private val binding: ItemResponseFreeTextBinding) : ViewHolder(binding.root) {
        override fun onBind(response: ResponseEntity, isDisabled: Boolean, showAltText: Boolean) {
            binding.response = response
            binding.isDisabled = isDisabled
            binding.isShowAltText = showAltText
        }
    }

    data class LinearScaleResponseViewHolder(private val binding: ItemResponseLinearScaleBinding) : ViewHolder(binding.root) {
        override fun onBind(response: ResponseEntity, isDisabled: Boolean, showAltText: Boolean) {
            binding.response = response
            binding.isDisabled = isDisabled
            binding.isShowAltText = showAltText
        }
    }

    data class RadioResponseViewHolder(private val binding: ItemResponseRadioBinding) : ViewHolder(binding.root) {
        override fun onBind(response: ResponseEntity, isDisabled: Boolean, showAltText: Boolean) {
            binding.response = response
            binding.isDisabled = isDisabled
            binding.isShowAltText = showAltText
        }
    }
}