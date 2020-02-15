package kaist.iclab.abclogger.ui.question.item

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.addTextChangedListener
import kaist.iclab.abclogger.R
import io.techery.progresshint.addition.widget.SeekBar as IndicatorSeekBar

class SliderView(context: Context, attrs: AttributeSet?) : QuestionView(context, attrs) {
    private var offset = 0

    private val seekBar: IndicatorSeekBar = IndicatorSeekBar(context).apply {
        id = View.generateViewId()
        hintDelegate.isPopupAlwaysShown = false
        hintDelegate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                attrChanged?.onChange()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        hintDelegate.setHintAdapter { _, progress -> "${offset + progress}" }
    }

    private val btnEtc: CheckBox = CheckBox(context).apply {
        id = View.generateViewId()
        text = context.getString(R.string.general_etc)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        setOnCheckedChangeListener { _, isChecked ->
            edtEtc.isEnabled = isChecked
            seekBar.isEnabled = !isChecked
            attrChanged?.onChange()
        }
    }

    private val edtEtc: EditText = EditText(context).apply {
        id = View.generateViewId()
        setHint(R.string.general_free_text)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        addTextChangedListener({ _, _, _, _ -> }, { _, _, _, _ -> attrChanged?.onChange() }, {})
    }

    init {
        addView(seekBar, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(btnEtc, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(edtEtc, LayoutParams(0, LayoutParams.WRAP_CONTENT))

        ConstraintSet().also { constraint ->
            constraint.clone(this)

            constraint.connect(seekBar.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraint.connect(seekBar.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            constraint.connect(seekBar.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)

            constraint.connect(btnEtc.id, ConstraintSet.TOP, seekBar.id, ConstraintSet.BOTTOM)
            constraint.connect(btnEtc.id, ConstraintSet.BOTTOM, edtEtc.id, ConstraintSet.BOTTOM)
            constraint.connect(btnEtc.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)

            constraint.connect(edtEtc.id, ConstraintSet.TOP, seekBar.id, ConstraintSet.BOTTOM)
            constraint.connect(edtEtc.id, ConstraintSet.LEFT, btnEtc.id, ConstraintSet.RIGHT)
            constraint.connect(edtEtc.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        }.applyTo(this)
    }

    override fun setAvailable(isAvailable: Boolean) {
        edtEtc.isEnabled = isAvailable
        btnEtc.isEnabled = isAvailable
        seekBar.isEnabled = isAvailable
    }

    override fun setShowEtc(showEtc: Boolean) {
        btnEtc.visibility = if (showEtc) View.VISIBLE else View.GONE
        edtEtc.visibility = if (showEtc) View.VISIBLE else View.GONE
    }

    override fun setOptions(options: Array<String>) {
        offset = options.firstOrNull()?.toIntOrNull() ?: 0
        seekBar.apply {
            max = (options.lastOrNull()?.toIntOrNull() ?: 100) - offset
        }
    }

    override var responses: Array<String> = arrayOf()
        get() = arrayOf((if (btnEtc.isChecked) edtEtc.text?.toString() else (seekBar.progress + offset).toString())
                ?: "")
        set(value) {
            if (field.firstOrNull() == value.firstOrNull()) return
            val response = value.firstOrNull() ?: return

            if (response.toIntOrNull() == null) {
                btnEtc.isChecked = true
                edtEtc.setText(response)
            } else {
                val progress = response.toInt() - offset
                if (progress in (0..seekBar.max)) {
                    seekBar.progress = progress
                } else {
                    btnEtc.isChecked = true
                    edtEtc.setText(progress.toString())
                }
            }
        }
}