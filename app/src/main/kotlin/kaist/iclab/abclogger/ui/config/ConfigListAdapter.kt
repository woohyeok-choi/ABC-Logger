package kaist.iclab.abclogger.ui.config

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.databinding.ConfigDataListItemBinding
import kaist.iclab.abclogger.databinding.ConfigHeaderListItemBinding
import kaist.iclab.abclogger.databinding.ConfigSimpleListItemBinding
import kaist.iclab.abclogger.databinding.ConfigSwitchListItemBinding

class ConfigListAdapter : RecyclerView.Adapter<ConfigListAdapter.ViewHolder>() {
    var items: ArrayList<ConfigData> = arrayListOf()
        set(value) {
            if (field.isNotEmpty()) {
                val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val oldItem = field.getOrNull(oldItemPosition) ?: return false
                        val newItem = value.getOrNull(newItemPosition) ?: return false

                        if (oldItem is ConfigHeader && newItem is ConfigHeader) {
                            return oldItem.title == newItem.title
                        }

                        if (oldItem is ConfigItem && newItem is ConfigItem) {
                            return if (oldItem.key.isEmpty() && newItem.key.isEmpty()) {
                                oldItem.title == newItem.title
                            } else {
                                oldItem.key == newItem.key
                            }
                        }
                        return false
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val oldItem = field.getOrNull(oldItemPosition) ?: return false
                        val newItem = value.getOrNull(newItemPosition) ?: return false

                        if (oldItem is ConfigHeader && newItem is ConfigHeader) {
                            return oldItem.title == newItem.title
                        }

                        if (oldItem is ConfigItem && newItem is ConfigItem) {
                            return oldItem == newItem
                        }
                        return false
                    }

                    override fun getOldListSize(): Int = field.size

                    override fun getNewListSize(): Int = value.size
                })
                field = value
                result.dispatchUpdatesTo(this)
            } else {
                field = value
                notifyItemRangeInserted(0, value.size)
            }
        }

    var onClick: ((key: String, item: ConfigData) -> Unit)? = null
    var onCheckedChanged: ((key: String, item: ConfigData, isChecked: Boolean) -> Unit)? = null

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
        items.getOrNull(position)?.let {
            holder.onBind(position, it, onClick, onCheckedChanged)
        }
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
        abstract fun onBind(position: Int,
                            configData: ConfigData,
                            onClick: ((key: String, item: ConfigData) -> Unit)?,
                            onCheckedChanged: ((key: String, item: ConfigData, isChecked: Boolean) -> Unit)?
        )
    }

    class ConfigHeaderViewHolder(val binding: ConfigHeaderListItemBinding) : ViewHolder(binding.root) {
        override fun onBind(position: Int, configData: ConfigData, onClick: ((key: String, item: ConfigData) -> Unit)?, onCheckedChanged: ((key: String, item: ConfigData, isChecked: Boolean) -> Unit)?) {
            (configData as? ConfigHeader)?.let { item ->
                binding.item = item
                binding.executePendingBindings()
            }
        }
    }


    class SimpleConfigItemViewHolder(val binding: ConfigSimpleListItemBinding) : ViewHolder(binding.root) {
        override fun onBind(position: Int, configData: ConfigData, onClick: ((key: String, item: ConfigData) -> Unit)?, onCheckedChanged: ((key: String, item: ConfigData, isChecked: Boolean) -> Unit)?) {
            (configData as? SimpleConfigItem)?.let { item ->
                binding.item = item
                binding.root.setOnClickListener { onClick?.invoke(item.key, item) }
                binding.executePendingBindings()
            }
        }
    }

    class SwitchConfigItemViewHolder(val binding: ConfigSwitchListItemBinding) : ViewHolder(binding.root) {
        override fun onBind(position: Int, configData: ConfigData, onClick: ((key: String, item: ConfigData) -> Unit)?, onCheckedChanged: ((key: String, item: ConfigData, isChecked: Boolean) -> Unit)?) {
            (configData as? SwitchConfigItem)?.let { item ->
                binding.item = item
                binding.root.setOnClickListener { binding.switchOnOff.toggle() }
                binding.switchOnOff.setOnCheckedChangeListener { _, isChecked ->
                    if (item.isChecked != isChecked) onCheckedChanged?.invoke(item.key, item, isChecked)
                }
                binding.executePendingBindings()
            }
        }
    }

    class DataConfigItemViewHolder(val binding: ConfigDataListItemBinding) : ViewHolder(binding.root) {
        override fun onBind(position: Int, configData: ConfigData, onClick: ((key: String, item: ConfigData) -> Unit)?, onCheckedChanged: ((key: String, item: ConfigData, isChecked: Boolean) -> Unit)?) {
            (configData as? DataConfigItem)?.let { item ->
                binding.item = item
                binding.root.setOnClickListener {
                    if (!item.isChecked) onClick?.invoke(item.key, item)
                }
                binding.switchOnOff.setOnCheckedChangeListener { _, isChecked ->
                    if (item.isChecked != isChecked) onCheckedChanged?.invoke(item.key, item, isChecked)
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