package kaist.iclab.abclogger.core

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.grpc.*
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.grpc.DataOperationsGrpcKt
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class AbstractNetworkRepository (context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
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
                    headers?.put(Metadata.Key.of("auth_token", Metadata.ASCII_STRING_MARSHALLER), BuildConfig.AUTH_TOKEN)
                    super.start(responseListener, headers)
                }
            }
        }
    }

    private val channel = ManagedChannelBuilder.forTarget(BuildConfig.SERVER_ADDRESS)
        .usePlaintext()
        .directExecutor()
        .intercept(interceptor)
        .build()

    protected val stub = DataOperationsGrpcKt.DataOperationsCoroutineStub(channel)

    protected val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Suppress("BlockingMethodInNonBlockingContext")
    protected fun shutdown() {
        channel.shutdown().awaitTermination(10, TimeUnit.SECONDS)
    }
}