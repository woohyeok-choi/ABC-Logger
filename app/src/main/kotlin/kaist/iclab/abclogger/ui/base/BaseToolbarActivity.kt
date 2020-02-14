package kaist.iclab.abclogger.ui.base

import android.os.Bundle
import android.view.Menu
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import kaist.iclab.abclogger.commons.AppLog
import kaist.iclab.abclogger.R
import kotlinx.android.synthetic.main.activity_base_toolbar.*

abstract class BaseToolbarActivity<T : ViewDataBinding, VM : BaseViewModel<*>>: AppCompatActivity() {
    protected val TAG: String = javaClass.name

    @get:MenuRes abstract val menuId: Int
    @get:LayoutRes abstract val layoutRes : Int
    @get:StringRes abstract val titleRes : Int

    abstract val viewModelVariable: Int
    abstract val viewModel: VM
    lateinit var dataBinding: T

    abstract fun beforeExecutePendingBindings()

    @CallSuper
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(menuId, menu)
        return true
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_toolbar)
        setSupportActionBar(tool_bar)

        supportActionBar?.apply {
            title = getString(titleRes)
            setDisplayHomeAsUpEnabled(true)
        }

        dataBinding = DataBindingUtil.inflate(layoutInflater, layoutRes, container, false)
        dataBinding.setVariable(viewModelVariable, viewModel)
        dataBinding.lifecycleOwner = this

        container.addView(dataBinding.root)

        beforeExecutePendingBindings()

        viewModel.load(intent.extras)

        dataBinding.executePendingBindings()
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        AppLog.d(TAG, "onStart()")
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        AppLog.d(TAG, "onStop()")
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        AppLog.d(TAG, "onDestroy()")
    }

    @CallSuper
    override fun onRestart() {
        super.onRestart()
        AppLog.d(TAG, "onRestart()")
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        AppLog.d(TAG, "onResume()")
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        AppLog.d(TAG, "onPause()")
    }
}