package kaist.iclab.abclogger.base

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.crossFade
import kaist.iclab.abclogger.databinding.ActivitySettingBinding

abstract class BaseSettingActivity<V : ViewBinding, VM : BaseViewModel> : BaseViewModelActivity<ActivitySettingBinding, VM>() {
    @get:StringRes
    abstract val titleRes: Int
    lateinit var childBinding: V

    private val shortAnimDuration by lazy { resources.getInteger(android.R.integer.config_shortAnimTime).toLong() }

    override fun getViewBinding(inflater: LayoutInflater): ActivitySettingBinding =
            ActivitySettingBinding.inflate(inflater)

    protected abstract fun getInnerViewBinding(inflater: LayoutInflater) : V

    @CallSuper
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.save, menu)
        return true
    }

    override fun afterViewInflate() {
        viewBinding.nestedScrollView.visibility = View.GONE
        viewBinding.progressBar.visibility = View.VISIBLE

        setSupportActionBar(viewBinding.toolBar)

        supportActionBar?.apply {
            title = getString(titleRes)
            setDisplayHomeAsUpEnabled(true)
        }
        childBinding = getInnerViewBinding(layoutInflater)

        viewBinding.container.addView(childBinding.root)

        afterToolbarCreated()
        crossFade(viewBinding.nestedScrollView, viewBinding.progressBar, shortAnimDuration)
    }

    private fun saveInternal() = lifecycleScope.launchWhenCreated {
        crossFade(viewBinding.progressBar, viewBinding.nestedScrollView, shortAnimDuration)

        if (save()) {
            finish()
        } else {
            crossFade(viewBinding.nestedScrollView, viewBinding.progressBar, shortAnimDuration)
        }
    }

    abstract fun afterToolbarCreated()

    abstract suspend fun save(): Boolean

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_save -> {
                saveInternal()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}