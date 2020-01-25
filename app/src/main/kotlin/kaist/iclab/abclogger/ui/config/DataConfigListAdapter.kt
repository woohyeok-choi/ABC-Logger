package kaist.iclab.abclogger.ui.config

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.collector.descriptionRes
import kaist.iclab.abclogger.collector.hasStarted
import kaist.iclab.abclogger.collector.nameRes
import kaist.iclab.abclogger.databinding.DataConfigListItemBinding

class DataConfigListAdapter : RecyclerView.Adapter<DataConfigListAdapter.ViewHolder>() {
    var onClick: ((collector: BaseCollector) -> Unit)? = null
    var onCheckedChanged: ((collector: BaseCollector, isChecked: Boolean) -> Unit)? = null

    var items: Array<BaseCollector> = arrayOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun updateItemView(collector: BaseCollector) {
        val index = items.indexOf(collector)
        if (index > 0) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding : DataConfigListItemBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context), R.layout.data_config_list_item, parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items.getOrNull(position)?.let { holder.onBind(it, onClick, onCheckedChanged) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(val binding: DataConfigListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(collector: BaseCollector,
                   onClick: ((collector: BaseCollector) -> Unit)? = null,
                   onCheckedChanged: ((collector: BaseCollector, isChecked: Boolean) -> Unit)? = null
        ) {
            binding.collector = collector
            binding.name = collector.nameRes()?.let{ itemView.context.getString(it) } ?: ""
            binding.description = collector.descriptionRes()?.let{ itemView.context.getString(it) } ?: ""
            binding.hasStarted = collector.hasStarted()

            binding.container?.setOnClickListener {
                if(collector.hasStarted() || collector.checkAvailability()) {
                    binding.switchOnOff.toggle()
                } else {
                    onClick?.invoke(collector)
                }
            }

            binding.switchOnOff?.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChanged?.invoke(collector, isChecked)
            }
        }
    }
}