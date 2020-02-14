package kaist.iclab.abclogger.collector.survey.setting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.databinding.SurveySettingListItemBinding

class SurveySettingAdapter : RecyclerView.Adapter<SurveySettingAdapter.ViewHolder>() {
    var onPreviewClick: ((String?) -> Unit)? = null
    var onRemoveClick: ((SurveyCollector.Status.Setting) -> Unit)? = null

    var items: ArrayList<SurveyCollector.Status.Setting> = arrayListOf()
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