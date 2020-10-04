package kaist.iclab.abclogger.ui.settings

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.core.ui.BaseViewModel
import kaist.iclab.abclogger.core.ui.BaseViewModelActivity
import kaist.iclab.abclogger.commons.crossFade
import kaist.iclab.abclogger.databinding.ActivitySettingBinding

abstract class AbstractSettingActivity<V : ViewBinding, VM : BaseViewModel> : BaseViewModelActivity<ActivitySettingBinding, VM>() {
    lateinit var childBinding: V

    override fun getViewBinding(inflater: LayoutInflater): ActivitySettingBinding =
            ActivitySettingBinding.inflate(inflater)

    protected abstract fun getInnerViewBinding(inflater: LayoutInflater) : V

    @CallSuper
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.setting, menu)
        return true
    }

    override fun afterViewInflate() {
        viewBinding.nestedScrollView.visibility = View.GONE
        viewBinding.progressBar.visibility = View.VISIBLE

        setSupportActionBar(viewBinding.toolBar)

        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }
        childBinding = getInnerViewBinding(layoutInflater)

        viewBinding.container.addView(childBinding.root)

        afterToolbarCreated()
        crossFade(viewBinding.nestedScrollView, viewBinding.progressBar)
    }

    abstract fun afterToolbarCreated()

    abstract fun undo()

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_undo -> {
                undo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}