package kaist.iclab.abclogger.ui.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthResult
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.tedpark.tedpermission.rx2.TedRx2Permission
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import kaist.iclab.abclogger.ABC
import kaist.iclab.abclogger.PermissionDeniedException
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.checkWhitelist
import kaist.iclab.abclogger.ui.main.SignInActivity
import org.koin.android.ext.android.inject
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

class SplashActivity : BaseAppCompatActivity(), PermissionListener {

    /**
     * Check permissions ->
     * (if granted) Check whitelisted ->
     * (if granted) Google sign In ->
     * (if granted) Firebase authorize ->
     * (if granted) Start main activity.
     */
    private val abc : ABC by inject()

    private val permissionSingle = TedRx2Permission.with(this)
            .setRationaleTitle(R.string.dialog_title_permission_request)
            .setRationaleMessage(R.string.dialog_message_permission_request)
            .setPermissions(*abc.getAllRequiredPermissions().toTypedArray())

            .request()
    private val whiteListSingle = SingleSubject.create<Boolean>()
    private val googleSignInSigle = SingleSubject.create<Intent>()
    private val firebaseAuthSingle = SingleSubject.create<AuthResult>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun finishActivity() {

    }

    private fun requestPermissions() {
        SingleSubject
        TedRx2Permission.with(this).request()
        TedPermission.with(this)
                .setRationaleTitle(R.string.dialog_title_permission_request)
                .setRationaleMessage(R.string.dialog_message_permission_request)
                .setPermissions(*abc.getAllRequiredPermissions().toTypedArray())
                .setPermissionListener(this)
                .check()
    }

    private fun requestWhitelist() {
        val manager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isWhitelisted = manager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestGoogleSignIn() {
        val option = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_api_key))
                .requestEmail()
                .build()
        val client = GoogleSignIn.getClient(this, option)
        val intent = client.signInIntent

        startActivityForResult(intent, SignInActivity.REQUEST_CODE_GOOGLE_SIGN_IN)
    }

    private fun requestFirebaseAuthorize(data: Intent?) {
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPermissionGranted() {
        if(checkWhitelist())
    }

    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {

    }

    companion object {

    }
}