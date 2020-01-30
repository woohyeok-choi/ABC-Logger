package kaist.iclab.abclogger.ui.question

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.survey.Survey
import kaist.iclab.abclogger.databinding.*

class SurveyQuestionListAdapter : RecyclerView.Adapter<SurveyQuestionListAdapter.ViewHolder>() {
    fun bind(questions: Array<Survey.Question>, isAvailable: Boolean, showAltText: Boolean) {
        this.questions = questions
        this.isAvailable = isAvailable
        this.showAltText = showAltText

        notifyDataSetChanged()
    }

    private var questions: Array<Survey.Question> = arrayOf()
    private var isAvailable: Boolean = true
    private var showAltText: Boolean = false


    override fun getItemViewType(position: Int): Int =
            when (questions.getOrNull(position)?.type) {
                Survey.QUESTION_RADIO_BUTTON -> QUESTION_RADIO_BUTTON
                Survey.QUESTION_HORIZONTAL_RADIO_BUTTON -> QUESTION_HORIZONTAL_RADIO_BUTTON
                Survey.QUESTION_CHECK_BOX -> QUESTION_CHECK_BOX
                Survey.QUESTION_SLIDER -> QUESTION_SLIDER
                else -> QUESTION_FREE_TEXT
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            when (viewType) {
                QUESTION_RADIO_BUTTON -> RadioButtonItemViewHolder(
                        DataBindingUtil.inflate(
                                LayoutInflater.from(parent.context), R.layout.question_radio_button_item, parent, false
                        )
                )
                QUESTION_HORIZONTAL_RADIO_BUTTON -> HorizontalRadioButtonItemViewHolder(
                        DataBindingUtil.inflate(
                                LayoutInflater.from(parent.context), R.layout.question_radio_button_item, parent, false
                        )
                )
                QUESTION_CHECK_BOX -> CheckBoxItemViewHolder(
                        DataBindingUtil.inflate(
                                LayoutInflater.from(parent.context), R.layout.question_check_box_item, parent, false
                        )
                )
                QUESTION_SLIDER -> SliderItemViewHolder(
                        DataBindingUtil.inflate(
                                LayoutInflater.from(parent.context), R.layout.question_slider_item, parent, false
                        )
                )
                else -> FreeTextItemViewHolder(
                        DataBindingUtil.inflate(
                                LayoutInflater.from(parent.context), R.layout.question_free_text_item, parent, false
                        )
                )
            }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.onBind(
                    questions.getOrNull(position), isAvailable, showAltText
            )

    override fun getItemCount(): Int = questions.size

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun onBind(item: Survey.Question? = null, isAvailable: Boolean, showAltText: Boolean)
    }

    class FreeTextItemViewHolder(private val binding: QuestionFreeTextItemBinding) : ViewHolder(binding.root) {
        override fun onBind(item: Survey.Question?, isAvailable: Boolean, showAltText: Boolean) {
            item ?: return

            binding.isAvailable = isAvailable
            binding.showAltText = showAltText
            binding.question = item
        }
    }

    class RadioButtonItemViewHolder(private val binding: QuestionRadioButtonItemBinding) : ViewHolder(binding.root) {
        override fun onBind(item: Survey.Question?, isAvailable: Boolean, showAltText: Boolean) {
            item ?: return

            binding.isAvailable = isAvailable
            binding.showAltText = showAltText
            binding.question = item
        }
    }

    class CheckBoxItemViewHolder(private val binding: QuestionCheckBoxItemBinding) : ViewHolder(binding.root) {
        override fun onBind(item: Survey.Question?, isAvailable: Boolean, showAltText: Boolean) {
            item ?: return

            binding.isAvailable = isAvailable
            binding.showAltText = showAltText
            binding.question = item
        }
    }

    class SliderItemViewHolder(private val binding: QuestionSliderItemBinding) : ViewHolder(binding.root) {
        override fun onBind(item: Survey.Question?, isAvailable: Boolean, showAltText: Boolean) {
            item ?: return

            binding.isAvailable = isAvailable
            binding.showAltText = showAltText
            binding.question = item
        }
    }

    class HorizontalRadioButtonItemViewHolder(private val binding: QuestionHorizontalRadioButtonItemBinding) : ViewHolder(binding.root) {
        override fun onBind(item: Survey.Question?, isAvailable: Boolean, showAltText: Boolean) {
            item ?: return

            binding.isAvailable = isAvailable
            binding.showAltText = showAltText
            binding.question = item
        }
    }

    companion object {
        private const val QUESTION_FREE_TEXT = 0
        private const val QUESTION_RADIO_BUTTON = 1
        private const val QUESTION_HORIZONTAL_RADIO_BUTTON = 2
        private const val QUESTION_CHECK_BOX = 3
        private const val QUESTION_SLIDER = 4
    }
}