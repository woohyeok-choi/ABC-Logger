package kaist.iclab.abclogger.ui.survey.response

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.survey.*
import kaist.iclab.abclogger.databinding.*
import kaist.iclab.abclogger.structure.survey.*

class SurveyResponseListAdapter : RecyclerView.Adapter<SurveyResponseListAdapter.ViewHolder>() {
    var responses: Array<InternalResponseEntity> = arrayOf()

    private var isAltTextShown: Boolean = false
    private var isEnabled: Boolean = false

    fun bind(responses: Collection<InternalResponseEntity>, isEnabled: Boolean, isAltTextShown: Boolean) {
        this.responses = responses.sortedBy { it.index }.toTypedArray()
        this.isEnabled = isEnabled
        this.isAltTextShown = isAltTextShown

        notifyDataSetChanged()
    }

    fun setResponses(responses: Collection<InternalResponseEntity>) {
        this.responses = responses.sortedBy { it.index }.toTypedArray()
        notifyDataSetChanged()
    }

    fun setAltTextShown(isAltTextShown: Boolean) {
        this.isAltTextShown = isAltTextShown
        notifyDataSetChanged()
    }

    fun setIsEnabled(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
            responses.getOrNull(position)?.question?.option?.type?.ordinal ?: Option.Type.NONE.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            Option.Type.FREE_TEXT.ordinal -> R.layout.item_response_free_text
            Option.Type.RADIO_BUTTON.ordinal -> R.layout.item_response_radio
            Option.Type.CHECK_BOX.ordinal -> R.layout.item_response_checkbox
            Option.Type.SLIDER.ordinal -> R.layout.item_response_slider
            Option.Type.RANGE.ordinal -> R.layout.item_response_range
            Option.Type.LINEAR_SCALE.ordinal -> R.layout.item_response_linear_scale
            Option.Type.DROPDOWN.ordinal -> R.layout.item_response_dropdown
            else -> null
        } ?: return NonResponseViewHolder(View(parent.context))

        return ResponseViewHolder(
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        layoutId,
                        parent,
                        false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        responses.getOrNull(position)?.let {
            holder.onBind(it, isEnabled, isAltTextShown)
        }
    }

    override fun getItemCount(): Int = responses.size

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun onBind(response: InternalResponseEntity, isEnabled: Boolean, isAltTextShown: Boolean)
    }

    data class NonResponseViewHolder(private val view: View) : ViewHolder(view) {
        override fun onBind(response: InternalResponseEntity, isEnabled: Boolean, isAltTextShown: Boolean) {
            view.visibility = View.GONE
        }
    }

    data class ResponseViewHolder(private val binding: ViewDataBinding) : ViewHolder(binding.root) {
        override fun onBind(response: InternalResponseEntity, isEnabled: Boolean, isAltTextShown: Boolean) {
            if (binding is ItemResponseFreeTextBinding) {
                binding.answer = response.answer
                binding.isOthersShown = response.question.isOtherShown
                binding.question = response.question.title.text(isAltTextShown)
                binding.isEnabled = isEnabled
                binding.option = response.question.option as? FreeTextOption
            }

            if (binding is ItemResponseRadioBinding) {
                binding.answer = response.answer
                binding.isOthersShown = response.question.isOtherShown
                binding.question = response.question.title.text(isAltTextShown)
                binding.isEnabled = isEnabled
                binding.option = response.question.option as? RadioButtonOption
            }

            if (binding is ItemResponseCheckboxBinding) {
                binding.answer = response.answer
                binding.isOthersShown = response.question.isOtherShown
                binding.question = response.question.title.text(isAltTextShown)
                binding.isEnabled = isEnabled
                binding.option = response.question.option as? CheckBoxOption
            }

            if (binding is ItemResponseSliderBinding) {
                binding.answer = response.answer
                binding.isOthersShown = response.question.isOtherShown
                binding.question = response.question.title.text(isAltTextShown)
                binding.isEnabled = isEnabled
                binding.option = response.question.option as? SliderOption
            }

            if (binding is ItemResponseRangeBinding) {
                binding.answer = response.answer
                binding.isOthersShown = response.question.isOtherShown
                binding.question = response.question.title.text(isAltTextShown)
                binding.isEnabled = isEnabled
                binding.option = response.question.option as? RangeOption
            }

            if (binding is ItemResponseLinearScaleBinding) {
                binding.answer = response.answer
                binding.isOthersShown = response.question.isOtherShown
                binding.question = response.question.title.text(isAltTextShown)
                binding.isEnabled = isEnabled
                binding.option = response.question.option as? LinearScaleOption
            }

            if (binding is ItemResponseDropdownBinding) {
                binding.answer = response.answer
                binding.isOthersShown = response.question.isOtherShown
                binding.question = response.question.title.text(isAltTextShown)
                binding.isEnabled = isEnabled
                binding.option = response.question.option as? DropdownOption
            }
        }
    }
}