package kaist.iclab.abclogger.ui.main

import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.commons.showToast
import kaist.iclab.abclogger.databinding.ActivityMainBinding
import kaist.iclab.abclogger.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {
    private var backPressedTime : Long = 0

    override val layoutId: Int = R.layout.activity_main

    override val viewModelVariable: Int = BR.viewModel

    override val viewModel: MainViewModel by viewModel()

    override fun beforeExecutePendingBindings() {
        val navController = nav_host_fragment.findNavController()
        val config = AppBarConfiguration(
                setOf(R.id.navigation_survey_list, R.id.navigation_config)
        )
        setupActionBarWithNavController(navController, config)
        dataBinding.navigationBottom.setupWithNavController(navController)
        dataBinding.navigationBottom.setOnNavigationItemReselectedListener {  }
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