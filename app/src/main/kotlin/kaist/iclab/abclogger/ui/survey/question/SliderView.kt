package kaist.iclab.abclogger.ui.survey.question

import android.content.Context
import android.util.AttributeSet
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import io.techery.progresshint.addition.widget.SeekBar as IndicatorSeekBark

class SliderView (context: Context, attrs: AttributeSet?) : IndicatorSeekBark(context, attrs) {
    constructor(context: Context) : this(context, null)
    var onAttributeChanged: (() -> Unit)? = null

    private var isBound: Boolean = false
    private var offset = 0

    init {
        hintDelegate.isPopupAlwaysShown = true
    }

    fun init(options: Array<String>, isAvailable: Boolean) {
        offset = options.firstOrNull()?.toIntOrNull() ?: 0
        max = options.lastOrNull()?.toIntOrNull() ?: 100 - offset
        isEnabled = isAvailable
        hintDelegate.setHintAdapter { _, progress -> "${offset + progress}" }
        hintDelegate.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                onAttributeChanged?.invoke()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) { }
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) { }
        })
    }

    private fun setResponse(responses: Array<String>) {
        val value = responses.firstOrNull()?.toIntOrNull() ?: offset
        progress = value - offset
    }

    fun bind(options: Array<String>, isAvailable: Boolean, responses: Array<String>) {
        if(!isBound) {
            init(options, isAvailable)
            isBound = true
        }
        setResponse(responses)
    }

    fun getResponse() : Array<String> {
        return arrayOf((offset + progress).toString())
    }
}

@BindingAdapter("options", "isAvailable", "responses")
fun bind(view: SliderView, options: Array<String>, isAvailable: Boolean, responses: Array<String>) {
    view.bind(options, isAvailable, responses)
}

@InverseBindingAdapter(attribute = "responses")
fun getResponses(view: SliderView) : Array<String> = view.getResponse()

@BindingAdapter("responsesAttrChanged")
fun setListener(view: SliderView, responseAttrChanged: InverseBindingListener) {
    view.onAttributeChanged = { responseAttrChanged.onChange() }
}