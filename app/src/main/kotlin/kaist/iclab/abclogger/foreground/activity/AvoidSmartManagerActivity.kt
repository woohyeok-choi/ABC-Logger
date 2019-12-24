package kaist.iclab.abclogger.foreground.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kaist.iclab.abclogger.base.BaseAppCompatActivity

class AvoidSmartManagerActivity: BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        finish()
    }

    companion object {
        fun newIntent(context: Context) : Intent = Intent(context, AvoidSmartManagerActivity::class.java)
    }
}