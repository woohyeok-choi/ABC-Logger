package kaist.iclab.abclogger.ui.settings.survey

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.databinding.ItemSurveyConfigurationBinding
import kaist.iclab.abclogger.structure.survey.SurveyConfiguration
import kotlin.collections.ArrayList

class SurveyConfigurationListAdapter :
    RecyclerView.Adapter<SurveyConfigurationListAdapter.ViewHolder>() {
    interface OnItemClickListener {
        fun onItemClick(position: Int, item: SurveyConfiguration)
    }

    private var onItemClickListener: OnItemClickListener? = null

    private val items: ArrayList<SurveyConfiguration> = arrayListOf()

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        onItemClickListener = listener
    }

    fun setOnItemClickListener(block: (position: Int, item: SurveyConfiguration) -> Unit) {
        onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int, item: SurveyConfiguration) {
                block.invoke(position, item)
            }
        }
    }

    fun setItems(item: Collection<SurveyConfiguration>) {
        items.clear()
        items.addAll(item)
        notifyDataSetChanged()
    }

    fun getItems() = items.toList()

    fun addItem(item: SurveyConfiguration) {
        if (item.uuid in items.map { it.uuid }) return
        items.add(0, item)
        notifyItemInserted(0)
    }

    fun removeItem(item: SurveyConfiguration) {
        val idx = items.indexOf(item)
        if (idx != -1) {
            items.remove(item)
            notifyItemRemoved(idx)
        }
    }

    fun changeItem(prevItem: SurveyConfiguration, newItem: SurveyConfiguration) {
        val idx = items.indexOf(prevItem)
        if (idx != -1) {
            items[idx] = newItem
            notifyItemChanged(idx)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.item_survey_configuration, parent, false
        ),
        listener = onItemClickListener
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items.getOrNull(position) ?: return
        holder.onBind(position, item)
    }

    class ViewHolder(
        private val binding: ItemSurveyConfigurationBinding,
        private val listener: OnItemClickListener?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(position: Int, item: SurveyConfiguration) {
            binding.item = item
            binding.root.setOnClickListener {
                listener?.onItemClick(position, item)
            }
        }
    }
}