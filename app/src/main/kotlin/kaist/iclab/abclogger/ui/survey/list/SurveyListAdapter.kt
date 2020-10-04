package kaist.iclab.abclogger.ui.survey.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.survey.InternalSurveyEntity
import kaist.iclab.abclogger.commons.Formatter
import kaist.iclab.abclogger.databinding.ItemSurveyListBinding
import kaist.iclab.abclogger.databinding.ItemSurveyListHeaderBinding
import kaist.iclab.abclogger.ui.survey.sharedViewNameForDeliveredTime
import kaist.iclab.abclogger.ui.survey.sharedViewNameForMessage
import kaist.iclab.abclogger.ui.survey.sharedViewNameForTitle

class SurveyListAdapter(private val header: String, private val isEnabled: Boolean?) :
    PagingDataAdapter<InternalSurveyEntity, SurveyListAdapter.ViewHolder>(DIFF_CALLBACK) {
    interface OnItemClickListener {
        fun onItemClick(position: Int, binding: ItemSurveyListBinding, item: InternalSurveyEntity)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        onItemClickListener = listener
    }

    fun setOnItemClickListener(block: (position: Int, binding: ItemSurveyListBinding, item: InternalSurveyEntity) -> Unit) {
        onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(
                position: Int,
                binding: ItemSurveyListBinding,
                item: InternalSurveyEntity
            ) {
                block.invoke(position, binding, item)
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position)?.id == -1L) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        if (viewType == VIEW_TYPE_ITEM) {
            ItemViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context), R.layout.item_survey_list, parent, false
                ),
                onItemClickListener
            )
        } else {
            HeaderViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_survey_list_header,
                    parent,
                    false
                )
            )
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentTime = System.currentTimeMillis()
        val item = getItem(position) ?: return

        if (holder is ItemViewHolder) {
            holder.bind(position, item, currentTime, isEnabled)
        }

        if (holder is HeaderViewHolder) {
            holder.bind(header)
        }
    }

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class ItemViewHolder(
        private val binding: ItemSurveyListBinding,
        private val onItemClickListener: OnItemClickListener?
    ) : ViewHolder(binding.root) {

        fun bind(
            position: Int,
            item: InternalSurveyEntity,
            currentTime: Long,
            isGlobalEnabled: Boolean?
        ) {
            val isAltTextShown = item.isAltTextShown(currentTime)
            val isEnabled = isGlobalEnabled ?: item.isEnabled(currentTime)

            binding.title = item.title.text(isAltTextShown)
            binding.message = item.message.text(isAltTextShown)
            binding.triggeredTime = Formatter.formatSameDateTime(
                binding.root.context,
                item.actualTriggerTime,
                currentTime
            )
            binding.isEnabled = isEnabled

            ViewCompat.setTransitionName(
                binding.txtTitle, sharedViewNameForTitle(item.id)
            )

            ViewCompat.setTransitionName(
                binding.txtMessage, sharedViewNameForMessage(item.id)
            )

            ViewCompat.setTransitionName(
                binding.txtTitle, sharedViewNameForDeliveredTime(item.id)
            )

            binding.root.setOnClickListener {
                onItemClickListener?.onItemClick(position, binding, item)
            }
        }
    }

    class HeaderViewHolder(private val binding: ItemSurveyListHeaderBinding) :
        ViewHolder(binding.root) {
        fun bind(header: String) {
            binding.header = header
        }
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0x1
        private const val VIEW_TYPE_HEADER = 0x2

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<InternalSurveyEntity>() {
            override fun areItemsTheSame(
                oldItem: InternalSurveyEntity,
                newItem: InternalSurveyEntity
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: InternalSurveyEntity,
                newItem: InternalSurveyEntity
            ): Boolean = oldItem == newItem
        }
    }
}