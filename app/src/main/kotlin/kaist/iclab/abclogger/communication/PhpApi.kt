package kaist.iclab.abclogger.communication

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kaist.iclab.abclogger.common.*
import kaist.iclab.abclogger.common.util.FormatUtils
import kaist.iclab.abclogger.common.util.NetworkUtils
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object PhpApi {

    private fun getConnection(url: URL) : HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = TimeUnit.MINUTES.toMillis(1).toInt()
        conn.readTimeout = TimeUnit.MINUTES.toMillis(1).toInt()
        conn.setRequestProperty("charset", "utf-8")
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        return conn
    }

    fun request(context: Context, url: String?, postData: ByteArray?) : Task<String> {
        return Tasks.call(Executors.newSingleThreadExecutor(), Callable {
            if(!FormatUtils.validateUrl(url)) throw InvalidUrlException()
            if(!NetworkUtils.isNetworkAvailable(context)) throw NoNetworkAvailableException()

            try {
                val conn = getConnection(URL(url))

                val outputStream = DataOutputStream(conn.outputStream)
                outputStream.write(postData)
                outputStream.flush()
                outputStream.close()

                when {
                    conn.responseCode in 400 until 500 -> throw InvalidRequestException()
                    conn.responseCode >= 500 -> throw ServerUnavailableException()
                    else -> return@Callable BufferedInputStream(conn.inputStream).reader(Charsets.UTF_8).readText()
                }
            } catch (e: SocketTimeoutException) {
                throw TimeoutException()
            } catch (e: Exception) {
                throw e
            }
            throw InvalidRequestException()
        })
    }

    fun dataToByteArray(user_id: String, experiment: String, msBandFlag: Boolean, pacoFlag: Boolean, polarFlag: Boolean, abcFlag: Boolean): ByteArray? {
        val timestamp = System.currentTimeMillis()/1000
        val msBand = if (msBandFlag) "y" else "n"
        val paco = if (pacoFlag) "y" else "n"
        val polar = if (polarFlag) "y" else "n"
        val abc = if (abcFlag) "y" else "n"

        val msg = "Data1=6&Data2=$user_id&Data3=$experiment&Data4=$timestamp&Data5=$msBand&Data6=$paco&Data7=$polar&Data8=$abc&Data9=null&Data10=null&Data11=null"
        return msg.toByteArray(StandardCharsets.UTF_8)
    }
}