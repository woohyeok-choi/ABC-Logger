package kaist.iclab.abclogger.ui.config.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.databinding.ItemConfigHeaderBinding
import kaist.iclab.abclogger.databinding.ItemConfigItemBinding
import kaist.iclab.abclogger.ui.config.ConfigData
import kaist.iclab.abclogger.ui.config.ConfigHeader
import kaist.iclab.abclogger.ui.config.ConfigItem

class ConfigListAdapter : RecyclerView.Adapter<ConfigListAdapter.ViewHolder>() {
    var items: Array<ConfigData> = arrayOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var onItemClick: ((position: Int, item: ConfigItem<*>) -> Unit)? = null

    private val internalListener: ((position: Int, item: ConfigItem<*>) -> Unit) = { position, item ->
        onItemClick?.invoke(position, item)
    }

    fun setOnItemClickListener(block: ((position: Int, item: ConfigItem<*>) -> Unit)?) {
        onItemClick = block
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        when (viewType) {
            VIEW_TYPE_ITEM -> ConfigItemViewHolder(
                    DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_config_item, parent, false),
                    internalListener
            )
            else -> ConfigHeaderViewHolder(
                    DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_config_header, parent, false)
            )
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(position, items.getOrNull(position))
    }

    override fun getItemViewType(position: Int): Int =
        when (items.getOrNull(position)) {
            is ConfigItem<*> -> VIEW_TYPE_ITEM
            else -> VIEW_TYPE_HEADER
        }

    override fun getItemCount(): Int = items.size

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun <T : ConfigData> onBind(position: Int, item: T?)
    }

    data class ConfigHeaderViewHolder(private val binding: ItemConfigHeaderBinding) : ViewHolder(binding.root) {
        override fun <T : ConfigData> onBind(position: Int, item: T?) {
            if (item !is ConfigHeader) return
            binding.item = item
            binding.isFirstItem = position == 0
        }
    }

    data class ConfigItemViewHolder(
            private val binding: ItemConfigItemBinding,
            private val onItemClick: (position: Int, item: ConfigItem<*>) -> Unit
    ): ViewHolder(binding.root) {
        override fun <T : ConfigData> onBind(position: Int, item: T?) {
            if (item !is ConfigItem<*>) return

            binding.item = item
            binding.isClickable = item is ReadWriteConfigItem<*>
            binding.root.setOnClickListener {
                onItemClick.invoke(position, item)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0x0
        private const val VIEW_TYPE_ITEM = 0x1
    }
}