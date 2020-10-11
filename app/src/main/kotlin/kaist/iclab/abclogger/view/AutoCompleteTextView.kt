package kaist.iclab.abclogger.view

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.ArrayAdapter
import kaist.iclab.abclogger.R

class AutoCompleteTextView(context: Context, attrs: AttributeSet?, defStyleAttr: Int): androidx.appcompat.widget.AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {
    constructor(context: Context): this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    private val defaultAdapter = ArrayAdapter<String>(context, R.layout.item_spinner)

    init {
        setAdapter(defaultAdapter)
        inputType = InputType.TYPE_NULL
    }

    var items: Array<String> = arrayOf()
        set(value) {
            field = value
            updateAdapter(items)
        }

    private fun updateAdapter(items: Array<String>) {
        defaultAdapter.clear()
        defaultAdapter.addAll(*items)
        defaultAdapter.notifyDataSetChanged()
    }
}