package kaist.iclab.abclogger.ui.config

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.databinding.ConfigDataListItemBinding
import kaist.iclab.abclogger.databinding.ConfigHeaderListItemBinding
import kaist.iclab.abclogger.databinding.ConfigSimpleListItemBinding
import kaist.iclab.abclogger.databinding.ConfigSwitchListItemBinding

class ConfigListAdapter : RecyclerView.Adapter<ConfigListAdapter.ViewHolder>() {
    private fun getDiffCallback(oldValue: ArrayList<ConfigData>, newValue: ArrayList<ConfigData>): DiffUtil.Callback = object : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldValue.getOrNull(oldItemPosition) ?: return false
            val newItem = newValue.getOrNull(newItemPosition) ?: return false

            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldValue.getOrNull(oldItemPosition) ?: return false
            val newItem = newValue.getOrNull(newItemPosition) ?: return false

            if (oldItem is ConfigHeader && newItem is ConfigHeader) {
                return oldItem.title == newItem.title
            }

            if (oldItem is ConfigItem && newItem is ConfigItem) {
                return oldItem == newItem
            }

            return false
        }

        override fun getOldListSize(): Int = oldValue.size

        override fun getNewListSize(): Int = newValue.size
    }

    var items: ArrayList<ConfigData> = arrayListOf()
        set(value) {
            if (field.isNotEmpty()) {
                val result = DiffUtil.calculateDiff(getDiffCallback(field, value))
                field = value
                result.dispatchUpdatesTo(this)
            } else {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            when (viewType) {
                VIEW_TYPE_SIMPLE -> SimpleConfigItemViewHolder(
                        DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.config_simple_list_item, parent, false)
                )
                VIEW_TYPE_SWITCH -> SwitchConfigItemViewHolder(
                        DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.config_switch_list_item, parent, false)
                )
                VIEW_TYPE_DATA -> DataConfigItemViewHolder(
                        DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.config_data_list_item, parent, false)
                )
                else -> ConfigHeaderViewHolder(
                        DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.config_header_list_item, parent, false)
                )
            }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items.getOrNull(position)?.let { data -> holder.onBind(position, data) }
    }

    override fun getItemViewType(position: Int): Int =
            items.getOrNull(position)?.let { item ->
                when (item) {
                    is SimpleConfigItem -> VIEW_TYPE_SIMPLE
                    is SwitchConfigItem -> VIEW_TYPE_SWITCH
                    is DataConfigItem -> VIEW_TYPE_DATA
                    else -> null
                }
            } ?: VIEW_TYPE_HEADER

    override fun getItemCount(): Int = items.size

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun onBind(position: Int, configData: ConfigData)

    }

    class ConfigHeaderViewHolder(val binding: ConfigHeaderListItemBinding) : ViewHolder(binding.root) {
        override fun onBind(position: Int, configData: ConfigData) {
            (configData as? ConfigHeader)?.let { item ->
                binding.item = item
                binding.executePendingBindings()
            }
        }
    }


    class SimpleConfigItemViewHolder(val binding: ConfigSimpleListItemBinding) : ViewHolder(binding.root) {
        override fun onBind(position: Int, configData: ConfigData) {
            (configData as? SimpleConfigItem)?.let { item ->
                binding.item = item
                binding.root.setOnClickListener {
                    item.onAction?.invoke()
                }
                binding.executePendingBindings()
            }
        }
    }

    class SwitchConfigItemViewHolder(val binding: ConfigSwitchListItemBinding) : ViewHolder(binding.root) {
        override fun onBind(position: Int, configData: ConfigData) {
            (configData as? SwitchConfigItem)?.let { item ->
                binding.item = item
                binding.root.setOnClickListener { binding.switchOnOff.toggle() }
                binding.switchOnOff.setOnCheckedChangeListener { _, isChecked ->
                    if (item.isChecked != isChecked) item.onChange?.invoke(isChecked)
                }
                binding.executePendingBindings()
            }
        }
    }

    class DataConfigItemViewHolder(val binding: ConfigDataListItemBinding) : ViewHolder(binding.root) {
        override fun onBind(position: Int, configData: ConfigData) {
            (configData as? DataConfigItem)?.let { item ->
                binding.item = item
                binding.root.setOnClickListener {
                    if (!item.isChecked) item.onAction?.invoke()
                }
                binding.switchOnOff.setOnCheckedChangeListener { _, isChecked ->
                    if (item.isChecked != isChecked) item.onChange?.invoke(isChecked)
                }
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0x1
        private const val VIEW_TYPE_SIMPLE = 0x2
        private const val VIEW_TYPE_SWITCH = 0x3
        private const val VIEW_TYPE_DATA = 0x4
    }
}