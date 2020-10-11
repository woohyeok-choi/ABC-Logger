package kaist.iclab.abclogger.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import kaist.iclab.abclogger.core.Log

abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {
    lateinit var viewBinding: T

    protected abstract fun afterViewInflate()
    protected abstract fun getViewBinding(inflater: LayoutInflater): T

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(javaClass, "onCreate()")
        viewBinding = getViewBinding(layoutInflater)
        setContentView(viewBinding.root)

        afterViewInflate()
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        Log.d(javaClass, "onStart()")
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        Log.d(javaClass, "onStop()")
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        Log.d(javaClass, "onDestroy()")
    }

    @CallSuper
    override fun onRestart() {
        super.onRestart()
        Log.d(javaClass, "onRestart()")
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        Log.d(javaClass, "onResume()")
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        Log.d(javaClass, "onPause()")
    }
}