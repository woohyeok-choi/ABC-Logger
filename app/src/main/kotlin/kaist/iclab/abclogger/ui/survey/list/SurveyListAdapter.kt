package kaist.iclab.abclogger.ui.survey.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.databinding.SurveyListItemBinding
import kaist.iclab.abclogger.ui.survey.sharedViewNameForDeliveredTime
import kaist.iclab.abclogger.ui.survey.sharedViewNameForMessage
import kaist.iclab.abclogger.ui.survey.sharedViewNameForTitle

class SurveyListAdapter: PagedListAdapter<SurveyEntity, SurveyListAdapter.ViewHolder>(DIFF_CALLBACK) {
    var onItemClick : ((item: SurveyEntity?, binding: SurveyListItemBinding) -> Unit)? = null

    override fun getItemId(position: Int): Long = getItem(position)?.id ?: -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding : SurveyListItemBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context), R.layout.survey_list_item, parent,false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        holder.binding.entity = item
        holder.binding.isAvailable = item.isAvailable()

        holder.setOnClick {
            onItemClick?.invoke(item, holder.binding)
        }

        ViewCompat.setTransitionName(
                holder.binding.txtHeader,
                sharedViewNameForTitle(item.id)
        )
        ViewCompat.setTransitionName(
                holder.binding.txtMessage,
                sharedViewNameForMessage(item.id)
        )
        ViewCompat.setTransitionName(
                holder.binding.txtDeliveredTime,
                sharedViewNameForDeliveredTime(item.id)
        )
    }

    class ViewHolder (val binding: SurveyListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun setOnClick(onClick: (() -> Unit)? = null) {
            itemView.setOnClickListener { onClick?.invoke() }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SurveyEntity>() {
            override fun areItemsTheSame(oldItem: SurveyEntity, newItem: SurveyEntity): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SurveyEntity, newItem: SurveyEntity): Boolean = oldItem == newItem
        }
    }
}