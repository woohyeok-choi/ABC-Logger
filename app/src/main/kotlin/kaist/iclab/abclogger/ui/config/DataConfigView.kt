package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import kaist.iclab.abclogger.R

class DataConfigView (context: Context) : ConstraintLayout(context) {
    private val headerTextView: TextView
    private val descriptionTextView: TextView
    private val switch : Switch
    private val permissionButton: ImageButton
    private val settingButton: ImageButton

    init {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        if(typedValue.resourceId != 0) {
            setBackgroundResource(typedValue.resourceId)
        } else {
            setBackgroundColor(typedValue.data)
        }

        setPadding(resources.getDimensionPixelSize(R.dimen.section_vertical_space))
        isClickable = true
        isFocusable = true

        headerTextView = TextView(context).apply {
            id = View.generateViewId()
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.color_message))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_title_small))
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
        }

        descriptionTextView = TextView(context).apply {
            id = View.generateViewId()
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(context, R.color.color_message))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_small_text))
            maxLines = 5
        }

        switch = Switch(context).apply {
            id = View.generateViewId()
            showText = false
        }

        permissionButton = ImageButton(context).apply {
            id = View.generateViewId()
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            setImageResource(R.drawable.baseline_perm_device_information_24)
        }

        settingButton = ImageButton(context).apply {
            id = View.generateViewId()
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            setImageResource(R.drawable.baseline_check_circle_outline_24)
        }

        addView(headerTextView, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(descriptionTextView, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(switch, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(permissionButton, LayoutParams(0, LayoutParams.WRAP_CONTENT))
        addView(settingButton, LayoutParams(0, LayoutParams.WRAP_CONTENT))

        ConstraintSet().apply {
            connect(headerTextView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(headerTextView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(headerTextView.id, ConstraintSet.END, permissionButton.id, ConstraintSet.START)

            connect(descriptionTextView.id, ConstraintSet.TOP, headerTextView.id, ConstraintSet.BOTTOM)
            connect(descriptionTextView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(descriptionTextView.id, ConstraintSet.END, settingButton.id, ConstraintSet.START)

            connect(permissionButton.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(permissionButton.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            connect(settingButton.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(settingButton.id, ConstraintSet.RIGHT, permissionButton.id, ConstraintSet.LEFT)

            connect(switch.id, ConstraintSet.TOP, settingButton.id, ConstraintSet.BOTTOM)
            connect(switch.id, ConstraintSet.START, settingButton.id, ConstraintSet.START)
            connect(switch.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }.let { setConstraintSet(it) }
    }

    var header : String
        get() = headerTextView.text.toString()
        set(value) { headerTextView.text = value }

    var description : String
        get() = descriptionTextView.text.toString()
        set(value) { descriptionTextView.text = value }

    var checkable : Boolean
        get() = switch.isEnabled
        set(value) { switch.isEnabled = value }

    var checked : Boolean
        get() = switch.isChecked
        set(value) { switch.isChecked = value }

    var showSetting : Boolean
        get() = settingButton.visibility == View.VISIBLE
        set(value) { settingButton.visibility = if(value) View.VISIBLE else View.GONE }

    var showPermission : Boolean
        get() = permissionButton.visibility == View.VISIBLE
        set(value) { permissionButton.visibility = if(value) View.VISIBLE else View.GONE }

    fun setOnSettingButtonClicked(onClick: (view: DataConfigView) -> Unit) {
        settingButton.setOnClickListener { onClick.invoke(this) }
    }

    fun setOnPermissionButtonClicked(onClick: (view: DataConfigView) -> Unit){
        permissionButton.setOnClickListener { onClick.invoke(this) }
    }

    fun setOnCheckedListener(onCheck: (view: DataConfigView, check: Boolean) -> Unit) {
        switch.setOnCheckedChangeListener { _, isChecked ->  onCheck.invoke(this, isChecked)}
    }
}