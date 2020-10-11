package kaist.iclab.abclogger.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.grpc.*
import io.grpc.kotlin.AbstractCoroutineStub
import kaist.iclab.abclogger.BuildConfig
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class AbstractNetworkRepository(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val interceptor = object :
        ClientInterceptor {
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            method: MethodDescriptor<ReqT, RespT>?,
            callOptions: CallOptions?,
            next: Channel?
        ): ClientCall<ReqT, RespT> {
            val newCall = next!!.newCall(method, callOptions)

            return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(newCall) {
                override fun start(responseListener: Listener<RespT>?, headers: Metadata?) {
                    headers?.put(Metadata.Key.of("auth_token", Metadata.ASCII_STRING_MARSHALLER), BuildConfig.SERVER_AUTH_TOKEN)
                    super.start(responseListener, headers)
                }
            }
        }
    }

    protected val channel: ManagedChannel by lazy {
        ManagedChannelBuilder.forTarget(BuildConfig.SERVER_ADDRESS)
            .usePlaintext()
            .directExecutor()
            .intercept(interceptor)
            .build()
    }

    protected val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    protected fun shutdown() {
        channel.shutdown().awaitTermination(10, TimeUnit.SECONDS)
    }
}