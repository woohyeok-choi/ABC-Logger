package kaist.iclab.abclogger.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.tedpark.tedpermission.rx2.TedRx2Permission
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class SplashViewModel(private val permissions: Array<String>,
                      navigator: SplashNavigator,
                      private val activity: AppCompatActivity) : BaseViewModel<SplashNavigator>(navigator) {

    override suspend fun onLoad(extras: Bundle?) { }

    override suspend fun onStore() { }

    fun doGoogleSignIn() = launch {
        try {
            GoogleApiAvailability.getInstance()
                    .makeGooglePlayServicesAvailable(activity).toCoroutine(throwable = GooglePlayServiceOutdatedException())

            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(activity.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            val client = GoogleSignIn.getClient(activity, options)

            nav?.navigateGoogleSignIn(client.signInIntent)
        } catch (e: Exception) {
            nav?.navigateError(e)
        }
    }

    fun doFirebaseAuth(intent: Intent?) = launch {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(intent).toCoroutine() ?: throw NoSignedGoogleAccountException()
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val user = FirebaseAuth.getInstance().signInWithCredential(credential).toCoroutine()?.user ?: throw FirebaseInvalidCredentialException()

            nav?.navigateFirebaseAuth(user)
        } catch (e: Exception) {
            nav?.navigateError(e)
        }
    }

    fun doPermissions(title: String, message: String) = launch {
        val result= TedRx2Permission.with(activity)
                .setRationaleTitle(title)
                .setRationaleMessage(message)
                .setPermissions(*permissions)
                .request().toCoroutine()

        val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:${activity.packageName}")
            }

        nav?.navigatePermission(result?.isGranted == true, intent)
    }

    fun doPermissionsAgain() = launch {
        val result = activity.checkPermission(permissions)
        if (!result) {
            nav?.navigateError(PermissionDeniedException())
        } else {
            nav?.navigatePermissionAgain()
        }
    }

    @SuppressLint("BatteryLife")
    fun doWhiteList() = launch {
        val packageName = activity.packageName
        val result = checkWhiteList(packageName)
        if (!result) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${packageName}")
            }
            nav?.navigateWhiteList(intent)
        } else {
            nav?.navigateSuccess()
        }
    }

    fun doWhiteListAgain() = launch {
        val packageName = activity.packageName
        val result = checkWhiteList(packageName)
        if (!result) {
            nav?.navigateError(WhiteListDeniedException())
        } else {
            nav?.navigateSuccess()
        }
    }

    private fun checkWhiteList(packageName: String) : Boolean {
        val manager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        return manager.isIgnoringBatteryOptimizations(packageName)
    }
}