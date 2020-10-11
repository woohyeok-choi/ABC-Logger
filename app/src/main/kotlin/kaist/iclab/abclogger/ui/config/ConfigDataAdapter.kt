package kaist.iclab.abclogger.ui.config

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.databinding.ItemConfigHeaderBinding
import kaist.iclab.abclogger.databinding.ItemConfigItemBinding
import kaist.iclab.abclogger.structure.config.Config
import kaist.iclab.abclogger.structure.config.ConfigData
import kaist.iclab.abclogger.structure.config.ConfigHeader
import kaist.iclab.abclogger.structure.config.ConfigItem


class ConfigDataAdapter : RecyclerView.Adapter<ConfigDataAdapter.ViewHolder>() {
    var config: Config = Config()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    interface OnItemClickListener {
        fun onItemClick(position: Int, item: ConfigItem<*>)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    fun setOnItemClickListener(block: ((position: Int, item: ConfigItem<*>) -> Unit)) {
        onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int, item: ConfigItem<*>) {
                block.invoke(position, item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        when (viewType) {
            VIEW_TYPE_ITEM -> ConfigItemViewHolder(
                    DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_config_item, parent, false),
                    onItemClickListener
            )
            else -> ConfigHeaderViewHolder(
                    DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_config_header, parent, false)
            )
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = config.getOrNull(position) ?: return
        holder.onBind(position, item)
    }

    override fun getItemViewType(position: Int): Int =
        when (config.getOrNull(position)) {
            is ConfigHeader -> VIEW_TYPE_HEADER
            else -> VIEW_TYPE_ITEM
        }

    override fun getItemCount(): Int = config.size

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun onBind(position: Int, item: ConfigData)
    }

    data class ConfigHeaderViewHolder(private val binding: ItemConfigHeaderBinding) : ViewHolder(binding.root) {
        override fun onBind(position: Int, item: ConfigData) {
            if (item !is ConfigHeader) return
            binding.item = item
        }
    }

    data class ConfigItemViewHolder(
            private val binding: ItemConfigItemBinding,
            private val listener: OnItemClickListener?
    ): ViewHolder(binding.root) {
        @Suppress("UNCHECKED_CAST")
        override fun onBind(position: Int, item: ConfigData) {
            if (item !is ConfigItem<*>) return
            binding.item = item as ConfigItem<Any>
            binding.root.setOnClickListener {
                listener?.onItemClick(position, item)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0x0
        private const val VIEW_TYPE_ITEM = 0x1
    }
}