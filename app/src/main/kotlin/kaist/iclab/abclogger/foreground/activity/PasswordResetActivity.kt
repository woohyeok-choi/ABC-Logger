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
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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
import kotlinx.android.synthetic.main.activity_password_reset.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class PasswordResetActivity : BaseAppCompatActivity() {
    private lateinit var emailWatcher: ErrorWatcher
    private lateinit var loadStateLiveData: MutableLiveData<LoadState>

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
        container.addView(LayoutInflater.from(this).inflate(R.layout.activity_password_reset, container, false))
        emailWatcher = ErrorWatcher(edtEmail, getString(R.string.edt_error_email_invalid)) { FormatUtils.validateEmail(it) }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.activity_title_password_reset)
            setDisplayHomeAsUpEnabled(true)
        }

    }

    private fun setupObservers() {
        loadStateLiveData = MutableLiveData()

        loadStateLiveData.observe(this, Observer {
            edtEmail.isEnabled = it?.status != LoadStatus.RUNNING

            when(it?.status) {
                LoadStatus.RUNNING -> btnNewPassword.startWith()
                LoadStatus.SUCCESS -> btnNewPassword.succeedWith(false) {
                    ViewUtils.showToast(this, R.string.msg_auth_request_verify_password)
                    finish()
                }
                LoadStatus.FAILED -> btnNewPassword.failedWith {
                    ViewUtils.showToast(this,
                        when (it.error) {
                            is FirebaseAuthInvalidUserException -> R.string.error_no_account
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
        edtEmail.editText?.addTextChangedListener(emailWatcher)

        btnNewPassword.setOnClickListener {
            loadStateLiveData.postValue(LoadState.LOADING)

            val executor = Executors.newSingleThreadExecutor()
            Tasks.call(executor, Callable {
                if(!NetworkUtils.isNetworkAvailable(this)) throw NoNetworkAvailableException()
                if(!FormatUtils.validateEmail(edtEmail)) throw InvalidContentException()
                edtEmail.editText?.text?.toString()!!
            }).onSuccessTask (executor, SuccessContinuation<String, Void> { result ->
                val auth = FirebaseAuth.getInstance()
                auth.useAppLanguage()
                auth.sendPasswordResetEmail(result!!)
            }).addOnSuccessListener { _ ->
                loadStateLiveData.postValue(LoadState.LOADED)
            }.addOnFailureListener {exception ->
                loadStateLiveData.postValue(LoadState.ERROR(exception))
            }
        }
    }

    companion object {
        fun newIntent(context: Context) : Intent = Intent(context, PasswordResetActivity::class.java)
    }
}