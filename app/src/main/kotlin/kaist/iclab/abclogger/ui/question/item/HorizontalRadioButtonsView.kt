package kaist.iclab.abclogger.ui.question.item

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import kaist.iclab.abclogger.R

class HorizontalRadioButtonsView(context: Context, attributeSet: AttributeSet?) : QuestionView(context, attributeSet) {
    private val layoutRadioGroup = RadioGroup(context).apply {
        id = View.generateViewId()
        orientation = LinearLayout.HORIZONTAL
        setOnCheckedChangeListener { _, _ -> attrChanged?.onChange() }
    }

    private val edtEtc: EditText = EditText(context).apply {
        id = View.generateViewId()
        setHint(R.string.general_free_text)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        addTextChangedListener({ _, _, _, _ -> }, { _, _, _, _ -> attrChanged?.onChange() }, {})
    }

    private val btnEtc: CheckBox = CheckBox(context).apply {
        id = View.generateViewId()
        text = context.getString(R.string.general_etc)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
        setOnCheckedChangeListener { _, isChecked ->
            edtEtc.isEnabled = isChecked
            layoutRadioGroup.isEnabled = !isChecked
            layoutRadioGroup.children.forEach { (it as? CompoundButton)?.isEnabled = !isChecked }
            attrChanged?.onChange()
        }
    }

    init {
        addView(layoutRadioGroup, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(btnEtc, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(edtEtc, LayoutParams(0, LayoutParams.WRAP_CONTENT))

        ConstraintSet().also { constraint ->
            constraint.clone(this)

            constraint.connect(layoutRadioGroup.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraint.connect(layoutRadioGroup.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            constraint.connect(layoutRadioGroup.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)

            constraint.connect(btnEtc.id, ConstraintSet.TOP, layoutRadioGroup.id, ConstraintSet.BOTTOM)
            constraint.connect(btnEtc.id, ConstraintSet.BOTTOM, edtEtc.id, ConstraintSet.BOTTOM)
            constraint.connect(btnEtc.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)

            constraint.connect(edtEtc.id, ConstraintSet.TOP, layoutRadioGroup.id, ConstraintSet.BOTTOM)
            constraint.connect(edtEtc.id, ConstraintSet.LEFT, btnEtc.id, ConstraintSet.RIGHT)
            constraint.connect(edtEtc.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        }.applyTo(this)
    }

    override fun setAvailable(isAvailable: Boolean) {
        (layoutRadioGroup.children + btnEtc + edtEtc).forEach { view -> view.isEnabled = isAvailable }
    }

    override fun setShowEtc(showEtc: Boolean) {
        btnEtc.visibility = if (showEtc) View.VISIBLE else View.GONE
        edtEtc.visibility = if (showEtc) View.VISIBLE else View.GONE
    }

    override fun setOptions(options: Array<String>) {
        layoutRadioGroup.removeAllViews()

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.listChoiceIndicatorSingle, typedValue, true)

        options.map { option ->
            RadioButton(context).apply {
                id = View.generateViewId()
                text = option
                buttonDrawable = null
                if(typedValue.resourceId != 0) setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(context, typedValue.resourceId), null, null)
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_text))
            }
        }.forEach { button ->
            layoutRadioGroup.addView(button,
                    RadioGroup.LayoutParams(0, RadioGroup.LayoutParams.WRAP_CONTENT).apply { weight = 1.0F }
            )
        }
    }

    override var responses: Array<String> = arrayOf()
        get() {
            val response = if (btnEtc.isChecked) {
                edtEtc.text?.toString()
            } else {
                (layoutRadioGroup.children.find { view ->
                    view.id == layoutRadioGroup.checkedRadioButtonId
                } as? CompoundButton)?.text?.toString()
            }
            return if (response.isNullOrBlank()) {
                arrayOf()
            } else {
                arrayOf(response)
            }
        }
        set(value) {
            if(field.firstOrNull() == value.firstOrNull()) return

            val response = value.firstOrNull()
            if (response.isNullOrBlank()) return

            val button = layoutRadioGroup.children.find { view ->
                (view as? CompoundButton)?.text == response
            }

            if (button != null) {
                (button as? CompoundButton?)?.isChecked = true
            } else {
                btnEtc.isChecked = true
                edtEtc.setText(response)
            }
        }
}