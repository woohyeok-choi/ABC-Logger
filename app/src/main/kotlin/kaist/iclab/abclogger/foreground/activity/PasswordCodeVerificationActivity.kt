package kaist.iclab.abclogger.foreground.activity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.ABCException
import kaist.iclab.abclogger.common.InvalidContentException
import kaist.iclab.abclogger.common.NoNetworkAvailableException
import kaist.iclab.abclogger.common.base.BaseAppCompatActivity
import kaist.iclab.abclogger.common.type.LoadState
import kaist.iclab.abclogger.common.type.LoadStatus
import kaist.iclab.abclogger.common.util.*
import kaist.iclab.abclogger.foreground.listener.ErrorWatcher
import kotlinx.android.synthetic.main.activity_container_with_toolbar.*
import kotlinx.android.synthetic.main.activity_password_code_verification.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class PasswordCodeVerificationActivity : BaseAppCompatActivity() {
    private lateinit var loadStateLiveData: MutableLiveData<LoadState>
    private lateinit var passwordWatcher: ErrorWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupContentView()
        setupActionBar()
        setupObservers()
        setupListener()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun setupContentView() {
        setContentView(R.layout.activity_container_with_toolbar)
        container.addView(LayoutInflater.from(this).inflate(R.layout.activity_password_code_verification, container, false))

        passwordWatcher = ErrorWatcher(edtNewPassword, getString(R.string.edt_error_password_invalid)) { FormatUtils.validateTextLength(it, 8) }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.activity_title_password_code_verify)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupObservers() {
        loadStateLiveData = MutableLiveData()

        loadStateLiveData.observe(this, Observer {
            edtNewPassword.isEnabled = it?.status != LoadStatus.RUNNING

            when(it?.status) {
                LoadStatus.RUNNING -> btnSubmit.startWith()
                LoadStatus.SUCCESS -> btnSubmit.succeedWith(false) {
                    ViewUtils.showToast(this, R.string.msg_auth_password_changed)
                    finish()
                }
                LoadStatus.FAILED -> btnSubmit.failedWith {
                    ViewUtils.showToast(this,
                        when (it.error) {
                            is FirebaseAuthActionCodeException ->  R.string.error_auth_password_code_expired
                            is FirebaseAuthInvalidUserException -> R.string.error_no_account
                            is FirebaseAuthWeakPasswordException -> R.string.error_weak_password
                            is ABCException -> it.error.getErrorStringRes()
                            else -> R.string.error_general_error
                        }
                    )
                }
                else -> { }
            }
        })
    }

    private fun setupListener() {
        edtEmail.editText?.setText(intent.getStringExtra(EXTRA_PASSWORD_RESET_EMAIL))
        edtNewPassword.editText?.addTextChangedListener(passwordWatcher)

        btnSubmit.setOnClickListener {
            val executor = Executors.newSingleThreadExecutor()
            Tasks.call(executor, Callable {
                loadStateLiveData.postValue(LoadState.LOADING)
                if(!NetworkUtils.isNetworkAvailable(this)) throw NoNetworkAvailableException()
                if(!FormatUtils.validateTextLength(edtNewPassword, 8)) throw InvalidContentException()
                edtNewPassword.editText?.text?.toString()!!
            }).onSuccessTask(executor, SuccessContinuation<String, Void> { result ->
                val auth = FirebaseAuth.getInstance()
                auth.useAppLanguage()
                auth.confirmPasswordReset(intent.getStringExtra(EXTRA_PASSWORD_RESET_CODE), result!!)
            }).addOnSuccessListener { _ ->
                loadStateLiveData.postValue(LoadState.LOADED)
            }.addOnFailureListener { exception ->
                loadStateLiveData.postValue(LoadState.ERROR(exception))
            }
        }
    }

    companion object {
        private val EXTRA_PASSWORD_RESET_CODE = "${PasswordCodeVerificationActivity::class.java.canonicalName}.EXTRA_PASSWORD_RESET_CODE"
        private val EXTRA_PASSWORD_RESET_EMAIL = "${PasswordCodeVerificationActivity::class.java.canonicalName}.EXTRA_PASSWORD_RESET_EMAIL"

        fun newIntent(context: Context, code: String, email: String) : Intent = Intent(context, PasswordCodeVerificationActivity::class.java)
            .putExtra(EXTRA_PASSWORD_RESET_CODE, code)
            .putExtra(EXTRA_PASSWORD_RESET_EMAIL, email)
    }
}