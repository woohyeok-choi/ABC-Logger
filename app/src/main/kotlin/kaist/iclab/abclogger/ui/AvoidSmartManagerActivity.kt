package kaist.iclab.abclogger.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AvoidSmartManagerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        finish()
    }
}