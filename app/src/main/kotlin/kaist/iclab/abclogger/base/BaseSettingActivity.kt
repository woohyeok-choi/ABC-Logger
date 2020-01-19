package kaist.iclab.abclogger.base

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.CallSuper
import kaist.iclab.abclogger.R
import kotlinx.android.synthetic.main.activity_setting_base.*
import kotlinx.coroutines.*

abstract class BaseSettingActivity : BaseAppCompatActivity() {
    @CallSuper
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_survey_question, menu)
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
        layoutInflater.inflate(contentLayoutRes, container, false)?.let { view ->
            container.addView(view)
            initializeSetting()
        }
    }

    @CallSuper
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when(item.itemId) {
        android.R.id.home -> {
            setResult(Activity.RESULT_CANCELED)
            finish()
            true
        }
        R.id.menu_activity_settings_save -> {
            MainScope().launch {
                val intent = async { generateResultIntent() }
                setResult(Activity.RESULT_OK, intent.await())
                finish()
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    abstract val contentLayoutRes : Int
    abstract val titleStringRes : Int
    abstract fun initializeSetting()
    abstract suspend fun generateResultIntent() : Intent

}