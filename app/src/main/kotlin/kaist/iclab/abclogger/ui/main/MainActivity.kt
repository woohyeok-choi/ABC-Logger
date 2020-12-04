package kaist.iclab.abclogger.ui.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.textview.MaterialTextView
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.ui.base.BaseActivity
import kaist.iclab.abclogger.commons.showToast
import kaist.iclab.abclogger.databinding.ActivityMainBinding
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.core.*
import kaist.iclab.abclogger.core.sync.HeartBeatRepository
import kaist.iclab.abclogger.core.sync.SyncRepository
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity<ActivityMainBinding>() {
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val collectorRepository: CollectorRepository by inject()

    private var backPressedTime: Long = 0

    private var bottomNavAnimator: ViewPropertyAnimator? = null
    private var bottomNavHeight: Float = 0F

    override fun getViewBinding(inflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(inflater)

    override fun afterViewInflate() {
        initHeaderView(viewBinding.navigationView.getHeaderView(0))
        setSupportActionBar(viewBinding.toolBar)

        val navController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.survey_all_list,
                R.id.survey_answered_list,
                R.id.survey_not_answered_list,
                R.id.survey_expired_list,
                R.id.config,
                R.id.survey_response
            ), viewBinding.root
        )

        viewBinding.toolBar.setupWithNavController(navController, appBarConfiguration)
        viewBinding.navigationView.setupWithNavController(navController)
        viewBinding.navigationBottom.setupWithNavController(navController)
        viewBinding.navigationBottom.setOnNavigationItemReselectedListener { }

        bottomNavHeight = viewBinding.navigationBottom.measuredHeight.toFloat()

        findNavController(R.id.nav_host_fragment).addOnDestinationChangedListener { _, destination, _ ->
            val id = destination.id

            animateToBottomNav(id == R.id.config_collector || id == R.id.survey_response)
        }

        lifecycleScope.launchWhenCreated {
            Log.sendReports()

            if (Preference.isAutoSync) SyncRepository.syncNow(
                this@MainActivity,
                Preference.isAutoSync
            )

            HeartBeatRepository.start(applicationContext)
            collectorRepository.restart()
        }
    }

    private fun initHeaderView(headerView: View) {
        val imgAvatar = headerView.findViewById<ImageView>(R.id.img_avatar)
        val txtEmail = headerView.findViewById<MaterialTextView>(R.id.txt_email)
        val txtName = headerView.findViewById<MaterialTextView>(R.id.txt_name)

        Glide.with(imgAvatar)
            .load(AuthRepository.avatarUrl(this))
            .apply(
                RequestOptions
                    .circleCropTransform()
                    .placeholder(R.mipmap.ic_launcher_circle)
            )
            .circleCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imgAvatar)
        txtEmail.text = AuthRepository.email
        txtName.text = AuthRepository.name
    }

    private fun animateToBottomNav(slideDown: Boolean) {
        bottomNavAnimator?.also {
            it.cancel()
            viewBinding.navigationBottom.clearAnimation()
        }

        val duration = if (slideDown) {
            175L
        } else {
            225L
        }

        val interpolator = if (slideDown) {
            AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
        } else {
            AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
        }

        val y = if (slideDown) resources.getDimension(R.dimen.bottom_nav_height) else 0F
        bottomNavAnimator = viewBinding.navigationBottom
            .animate()
            .translationY(y)
            .setInterpolator(interpolator)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    if (!slideDown) {
                        viewBinding.navigationBottom.visibility = View.VISIBLE
                    }
                }

                override fun onAnimationEnd(animation: Animator?) {
                    bottomNavAnimator = null

                    if (slideDown) {
                        viewBinding.navigationBottom.visibility = View.GONE
                    }
                }
            })
    }

    override fun onBackPressed() {
        val id = findNavController(R.id.nav_host_fragment).currentDestination?.id
        if (id == R.id.survey_all_list) {
            val curTime = System.currentTimeMillis()
            if (curTime - backPressedTime < BACK_TWICE_EXIT_LATENCY) {
                finish()
            } else {
                backPressedTime = curTime
                showToast(R.string.main_msg_back_twice_exit)
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.d(javaClass, "onSupportNavigateUp()")

        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object {
        const val BACK_TWICE_EXIT_LATENCY = 2000
    }
}