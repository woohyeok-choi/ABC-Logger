package kaist.iclab.abclogger.sync

import android.content.Context
import io.grpc.ManagedChannel
import io.grpc.android.AndroidChannelBuilder
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.collector.Base
import kaist.iclab.abclogger.grpc.DataOperationsCoroutineGrpc
import kaist.iclab.abclogger.grpc.DatumProto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlin.coroutines.CoroutineContext


class GrpcApi(val context: Context) {

}