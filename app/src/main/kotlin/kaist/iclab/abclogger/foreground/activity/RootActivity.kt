package kaist.iclab.abclogger.foreground.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ActionCodeResult
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.base.BaseAppCompatActivity
import kaist.iclab.abclogger.common.util.NotificationUtils
import kaist.iclab.abclogger.prefs
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class RootActivity: BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationUtils.cancelSurveyRemained(this)
        NotificationUtils.cancelSurveyDelivered(this)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val hasNewVersion = intent?.hasExtra("new_version")
        val code = intent?.data?.getQueryParameter("oobCode")
        val auth = FirebaseAuth.getInstance()

        if(hasNewVersion == true) {
            // 아마 앱 버전관리용인듯.
            startActivity(newIntentForGooglePlayStore())
        } else if (!TextUtils.isEmpty(code)) {
            val executor = Executors.newSingleThreadExecutor()
            auth.checkActionCode(code!!)
                .continueWithTask(executor, Continuation<ActionCodeResult, Task<Intent>> {
                    when (it.result.operation) {
                        ActionCodeResult.VERIFY_EMAIL -> auth.applyActionCode(code)
                            .continueWithTask(executor, Continuation { _ -> getDefaultIntentTask(it.result.getData(ActionCodeResult.EMAIL)) })
                        ActionCodeResult.PASSWORD_RESET -> auth.verifyPasswordResetCode(code)
                            .continueWithTask(executor, Continuation { result -> getPasswordResetIntentTask(result.result, code) })
                        else -> getDefaultIntentTask()
                    }
                }).addOnCompleteListener {
                    if(it.isSuccessful) {
                        Toast.makeText(this@RootActivity, R.string.msg_verify_success, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@RootActivity, R.string.error_link_expired, Toast.LENGTH_SHORT).show()
                    }

                    startActivity(it.result)
                    finish()
                }
        } else {
            startActivity(getDefaultIntent())
            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun getPasswordResetIntentTask(email: String, code: String) : Task<Intent> {
        val executor = Executors.newSingleThreadExecutor()
        return Tasks.call(executor, Callable {
            PasswordCodeVerificationActivity.newIntent(this, email = email, code = code)
        })
    }

    private fun getDefaultIntent(email: String? = null) : Intent {
        Log.d(TAG, "getDefaultIntent ()")
        /*
        return if (FirebaseAuth.getInstance().currentUser?.isEmailVerified == true) {
            MainActivity.newIntent(this).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } else {
            SignInActivity.newIntent(this, email ?: "")
        }
        */
        //Log.d(TAG, "${prefs}, ${prefs.participantExists}")
        return if (prefs.participantSignedIn) {
            MainActivity.newIntent(this).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } else {
            SignInActivity.newIntent(this, email ?: "")
        }
    }

    private fun getDefaultIntentTask(email: String? = null) : Task<Intent> {
        Log.d(TAG, "getDefaultIntentTask ()")
        val executor = Executors.newSingleThreadExecutor()
        /*
        val currentUser = FirebaseAuth.getInstance().currentUser
        return if (currentUser == null || currentUser.isEmailVerified) {
            Tasks.call(executor, Callable { getDefaultIntent(email)})
        } else {
            currentUser.reload().continueWith(executor, Continuation { getDefaultIntent(email) })
        }
        */
        return if (prefs.participantSignedUp) {
            Tasks.call(executor, Callable { getDefaultIntent(prefs.participantEmail) })
        } else {
            Tasks.call(executor, Callable { getDefaultIntent(email) })
        }
    }

    companion object {
        fun newIntentForGooglePlayStore() : Intent = Intent(Intent.ACTION_VIEW)
            .setData(Uri.parse("https://play.google.com/store/apps/details?id=kaist.iclab.abc"))
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}