package kaist.iclab.abclogger.ui.settings.keylog

import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.ui.settings.AbstractSettingActivity
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.commons.getActivityResult
import kaist.iclab.abclogger.commons.getColorFromAttr
import kaist.iclab.abclogger.databinding.LayoutSettingKeyLogBinding
import kaist.iclab.abclogger.dialog.VersatileDialog
import org.koin.androidx.viewmodel.ext.android.stateSharedViewModel
import org.koin.androidx.viewmodel.ext.android.stateViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class KeyLogSettingActivity :
    AbstractSettingActivity<LayoutSettingKeyLogBinding, KeyLogViewModel>() {
    override val viewModel: KeyLogViewModel by stateViewModel()

    private val options = arrayOf(
        KeyLogCollector.KEYBOARD_TYPE_CHUNJIIN,
        KeyLogCollector.KEYBOARD_TYPE_QWERTY_KOR,
        KeyLogCollector.KEYBOARD_TYPE_OTHERS
    )

    override fun getInnerViewBinding(inflater: LayoutInflater): LayoutSettingKeyLogBinding =
        LayoutSettingKeyLogBinding.inflate(inflater)

    override fun afterToolbarCreated() {
        childBinding.containerKeyboard.setOnClickListener {
            lifecycleScope.launchWhenCreated {
                val items = options.mapNotNull { type ->
                    keyboardTypeToString(type)
                }.toTypedArray()

                val idx = VersatileDialog.singleChoice(
                    manager = supportFragmentManager,
                    owner = this@KeyLogSettingActivity,
                    title = getString(R.string.setting_key_log_keyboard_type_title),
                    value = options.indexOf(viewModel.keyboardType),
                    items = items
                )

                if (idx != null) {
                    viewModel.keyboardType = options[idx]
                    updateUi(viewModel.keyboardType)
                }
            }
        }

        childBinding.containerAccessibility.setOnClickListener {
            lifecycleScope.launchWhenCreated {
                getActivityResult(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                    ActivityResultContracts.StartActivityForResult()
                )
                updateUi(viewModel.isAccessibilityServiceAllowed())
            }
        }

        lifecycleScope.launchWhenCreated {
            val keyboardType = viewModel.keyboardType
            val isAllowed = viewModel.isAccessibilityServiceAllowed()

            viewModel.saveState(KEY_KEYBOARD_TYPE, keyboardType)

            updateUi(keyboardType)
            updateUi(isAllowed)
        }
    }

    override fun undo() {
        val keyboardType = viewModel.loadState(KEY_KEYBOARD_TYPE) ?: -1
        val isAllowed = viewModel.isAccessibilityServiceAllowed()

        viewModel.keyboardType = keyboardType

        updateUi(keyboardType)
        updateUi(isAllowed)
    }

    private fun updateUi(keyboardType: Int) {
        childBinding.txtKeyboardText.text = keyboardTypeToString(keyboardType)
    }

    private fun updateUi(isAllowed: Boolean) {
        childBinding.txtAccessibilityText.text = accessibilityToString(isAllowed)
        val textColor = getColorFromAttr(this, if (isAllowed) R.attr.colorPrimary else R.attr.colorError) ?: return
        childBinding.txtAccessibilityText.setTextColor(textColor)
    }

    private fun keyboardTypeToString(type: Int?) = when (type) {
        KeyLogCollector.KEYBOARD_TYPE_CHUNJIIN -> R.string.setting_key_log_keyboard_type_text_chunjiin
        KeyLogCollector.KEYBOARD_TYPE_QWERTY_KOR -> R.string.setting_key_log_keyboard_type_text_qwerty
        KeyLogCollector.KEYBOARD_TYPE_OTHERS -> R.string.setting_key_log_keyboard_type_text_others
        else -> null
    }?.let { getString(it) }

    private fun accessibilityToString(isAllowed: Boolean) = getString(
        if (isAllowed) {
            R.string.setting_key_log_accessibility_text_allowed
        } else {
            R.string.setting_key_log_accessibility_text_rejected
        }
    )

    companion object {
        private const val KEY_KEYBOARD_TYPE = "KEY_KEYBOARD_TYPE"
    }
}