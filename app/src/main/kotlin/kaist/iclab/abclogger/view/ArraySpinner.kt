package kaist.iclab.abclogger.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.AppCompatSpinner

class ArraySpinner(context: Context, attrs: AttributeSet?, defStyleAttr: Int, mode: Int): AppCompatSpinner(context, attrs, defStyleAttr, mode) {
    constructor(context: Context): this(context, null, 0, MODE_DROPDOWN)
    constructor(context: Context, mode: Int): this(context, null, 0, mode)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0, MODE_DROPDOWN)

    private val defaultAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, 0).apply {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    init {
        adapter = defaultAdapter
    }

    var items: Array<String> = arrayOf()
        set(value) {
            field = value
            updateAdapter(items)
        }

    private fun updateAdapter(items: Array<String>) {
        defaultAdapter.clear()
        Array(items.size + 1) {
            if (it == 0) "" else items[it - 1]
        }.forEach {
            defaultAdapter.add(it)
        }
        defaultAdapter.notifyDataSetChanged()
    }

    override fun getSelectedItem(): Any? {
        val position = selectedItemPosition

        return if (position <= 0 || defaultAdapter.isEmpty) {
            null
        } else {
            defaultAdapter.getItem(position - 1)
        }
    }

    fun setSelectedItem(item: Any?) {
        if (item == null || defaultAdapter.isEmpty) {
            setSelection(0)
        } else {
            setSelection(items.indexOf(item).coerceAtLeast(0))
        }
    }
}