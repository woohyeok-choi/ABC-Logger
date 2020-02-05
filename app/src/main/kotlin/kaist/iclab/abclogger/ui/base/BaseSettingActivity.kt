package kaist.iclab.abclogger.ui.base

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.CallSuper
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import kaist.iclab.abclogger.R
import kotlinx.android.synthetic.main.activity_setting_base.*

abstract class BaseSettingActivity<T: ViewDataBinding, VM: ViewModel> : BaseAppCompatActivity() {
    @CallSuper
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_settings, menu)
        return true
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_base)
        setSupportActionBar(tool_bar)
        supportActionBar?.apply {
            title = getString(titleStringRes)
            setDisplayHomeAsUpEnabled(true)
        }

        dataBinding = DataBindingUtil.inflate(layoutInflater, contentLayoutRes, container, false)
        dataBinding.lifecycleOwner = this

        container.addView(dataBinding.root)
        initialize()
    }

    @CallSuper
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when(item.itemId) {
        android.R.id.home -> {
            setResult(Activity.RESULT_CANCELED)
            finish()
            true
        }
        R.id.menu_activity_settings_save -> {
            onSaveSelected()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    abstract val contentLayoutRes : Int
    abstract val titleStringRes : Int
    abstract fun initialize()
    abstract fun onSaveSelected()
    abstract val viewModel : VM
    lateinit var dataBinding : T
}