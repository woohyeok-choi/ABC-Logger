package kaist.iclab.abclogger.ui.base

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import kaist.iclab.abclogger.commons.AppLog

abstract class BaseActivity<T : ViewDataBinding, VM : BaseViewModel<*>>: AppCompatActivity() {
    protected val TAG: String = javaClass.name

    @get:LayoutRes abstract val layoutId: Int
    abstract val viewModelVariable: Int
    abstract val viewModel: VM
    lateinit var dataBinding: T

    abstract fun beforeExecutePendingBindings()

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.d(TAG, "onCreate()")
        dataBinding = DataBindingUtil.setContentView(this, layoutId)
        dataBinding.setVariable(viewModelVariable, viewModel)
        dataBinding.lifecycleOwner = this

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