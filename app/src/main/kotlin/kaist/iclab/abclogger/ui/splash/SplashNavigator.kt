package kaist.iclab.abclogger.ui.splash

import android.content.Intent
import com.google.firebase.auth.FirebaseUser
import kaist.iclab.abclogger.ui.base.BaseNavigator

interface SplashNavigator : BaseNavigator {
    fun navigateGoogleSignIn(intent: Intent)
    fun navigateFirebaseAuth(user: FirebaseUser)
    fun navigatePermission(isGranted: Boolean, intent: Intent)
    fun navigatePermissionAgain()
    fun navigateWhiteList(intent: Intent)
    fun navigateSuccess()
}