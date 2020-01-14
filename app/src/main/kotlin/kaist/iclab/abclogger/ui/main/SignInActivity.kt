package kaist.iclab.abclogger.ui.main

import android.content.Intent
import android.os.Bundle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import kaist.iclab.abclogger.Utils
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseAppCompatActivity

class SignInActivity : BaseAppCompatActivity() {
    private fun initListeners() {
        btn_sign_in.setOnClickListener {
            val option = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.google_api_key))
                    .requestEmail()
                    .build()
            val client = GoogleSignIn.getClient(this, option)
            val intent = client.signInIntent

            startActivityForResult(intent, REQUEST_CODE_GOOGLE_SIGN_IN)
        }
    }

    private fun authorize(data: Intent?) = GoogleSignIn.getSignedInAccountFromIntent(data)
            .continueWithTask { task ->
                val credential = GoogleAuthProvider.getCredential(task.result?.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
            }.addOnSuccessListener { result ->
                Utils.showToast(
                        this, Utils.join(getString(R.string.msg_sign_in_success), result.user?.displayName, separator = " ")
                )
                startActivity(MainActivity.newIntent(this))
                finish()
            }.addOnFailureListener { exception ->
                val msg = when (exception) {
                    is ApiException -> Utils.join(getString(R.string.error_google_api_exception), exception.message, separator = ": ")
                    is FirebaseAuthInvalidUserException -> getString(R.string.error_firebase_auth_invalid_user)
                    is FirebaseAuthInvalidCredentialsException -> getString(R.string.error_firebase_auth_invalid_credential)
                    is FirebaseAuthUserCollisionException -> getString(R.string.error_firebase_auth_collision)
                    else -> Utils.join(getString(R.string.error_general), exception.message, separator = ": ")
                }
                Utils.showToast(this, msg)
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        initListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_GOOGLE_SIGN_IN) authorize(data)
    }

    companion object {
        const val REQUEST_CODE_GOOGLE_SIGN_IN = 0xff
    }
}