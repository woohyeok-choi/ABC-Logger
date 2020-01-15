package kaist.iclab.abclogger.ui.survey.question

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import kaist.iclab.abclogger.R
import io.techery.progresshint.addition.widget.SeekBar as IndicatorSeekBark

class SliderView (context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    constructor(context: Context) : this(context, null)
    var onAttributeChanged: (() -> Unit)? = null

    private var isBound: Boolean = false
    private var offset = 0

    private val seekBar : IndicatorSeekBark = IndicatorSeekBark(context, attrs).apply {
        hintDelegate.isPopupAlwaysShown = true
    }

    private val checkBox: CheckBox = CheckBox(context).apply {
        id = View.generateViewId()
        text = context.getString(R.string.general_etc)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        setOnCheckedChangeListener { _, _ -> onAttributeChanged?.invoke() }
    }

    private val editText : EditText = EditText(context).apply {
        id = View.generateViewId()
        setHint(R.string.general_free_text)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        addTextChangedListener({_, _, _, _ -> }, {_, _, _, _ -> onAttributeChanged?.invoke()}, {})
    }

    fun init(options: Array<String>, showEtc: Boolean, isAvailable: Boolean) {
        offset = options.firstOrNull()?.toIntOrNull() ?: 0
        seekBar.apply {
            max = options.lastOrNull()?.toIntOrNull() ?: 100 - offset
            isEnabled = isAvailable
            hintDelegate.setHintAdapter { _, progress -> "${offset + progress}" }
            hintDelegate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    onAttributeChanged?.invoke()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) { }
                override fun onStopTrackingTouch(seekBar: SeekBar?) { }
            })
        }
        checkBox.apply {
            isEnabled = isAvailable
            visibility = if(showEtc) View.VISIBLE else View.GONE
        }

        editText.apply {
            isEnabled = isAvailable
            visibility = if(showEtc) View.VISIBLE else View.GONE
        }
    }

    private fun setResponse(responses: Array<String>) {
        if(responses.isEmpty()) return

        if (responses.first().toIntOrNull() == null) {
            /**
             * Meaning that this is at least not a number
             */
            checkBox.isChecked = true
            editText.setText(responses.first(), TextView.BufferType.EDITABLE)
        } else {
            val value = responses.first().toInt() - offset
            /**
             * Check whether a value is in a range.
             */
            if (value in 0..seekBar.max) {
                seekBar.progress = value
            } else {
                checkBox.isChecked = true
                editText.setText(responses.first(), TextView.BufferType.EDITABLE)
            }
        }
    }

    fun bind(options: Array<String>, showEtc: Boolean, isAvailable: Boolean, responses: Array<String>) {
        if(!isBound) {
            init(options, showEtc, isAvailable)
            isBound = true
        }
        setResponse(responses)
    }

    fun getResponse() : Array<String> =
        if(checkBox.isChecked) {
            arrayOf(editText.text?.toString() ?: "")
        } else {
            arrayOf((offset + seekBar.progress).toString())
        }
}

@BindingAdapter("options", "showEtc", "isAvailable", "responses")
fun bind(view: SliderView, options: Array<String>?, showEtc: Boolean?, isAvailable: Boolean?, responses: Array<String>?) {
    view.bind(options ?: arrayOf(), showEtc ?: false, isAvailable ?: false, responses ?: arrayOf())
}

@InverseBindingAdapter(attribute = "responses")
fun getResponses(view: SliderView) : Array<String> = view.getResponse()

@BindingAdapter("responsesAttrChanged")
fun setListener(view: SliderView, responseAttrChanged: InverseBindingListener) {
    view.onAttributeChanged = { responseAttrChanged.onChange() }
}