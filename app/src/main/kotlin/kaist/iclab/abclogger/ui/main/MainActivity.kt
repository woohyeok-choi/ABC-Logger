package kaist.iclab.abclogger.ui.main

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseAppCompatActivity() {
    private var backPressedTime : Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!checkServiceRunning<ABCLogger.ABCLoggerService>())
            startForegroundService<ABCLogger.ABCLoggerService>()

        setContentView(R.layout.activity_main)

        val navController = navigation_host_fragment.findNavController()
        val config = AppBarConfiguration(
                setOf(R.id.navigation_survey_list, R.id.navigation_config)
        )
        setupActionBarWithNavController(navController, config)

        navigation.setupWithNavController(navController)

        navigation.setOnNavigationItemReselectedListener {  }
    }

    override fun onBackPressed() {
        val curTime = System.currentTimeMillis()
        if (curTime - backPressedTime < BACK_TWICE_EXIT_LATENCY) {
            finish()
        } else {
            backPressedTime = curTime
            showToast(R.string.msg_back_twice_exit)
        }
    }

    companion object {
        const val BACK_TWICE_EXIT_LATENCY = 2000
    }
}