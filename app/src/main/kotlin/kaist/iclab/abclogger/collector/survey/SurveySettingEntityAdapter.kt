package kaist.iclab.abclogger.collector.survey

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.databinding.SurveySettingListItemBinding

class SurveySettingEntityAdapter : RecyclerView.Adapter<SurveySettingEntityAdapter.ViewHolder>() {
    var onPreviewClick: ((String?) -> Unit)? = null
    var onRemoveClick: ((SurveySettingEntity) -> Unit)? = null

    var items: ArrayList<SurveySettingEntity> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ViewHolder(val binding: SurveySettingListItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding : SurveySettingListItemBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context), R.layout.survey_setting_list_item, parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items.getOrNull(position)?.let { item ->
            holder.binding.item = item
            holder.binding.btnRemoveItem?.setOnClickListener {
                onRemoveClick?.invoke(item)
            }
            holder.binding.btnPreview?.setOnClickListener {
                onPreviewClick?.invoke(item.url)
            }
        }
    }
}