package wasabi375.dynamicgradletask

import kotlinx.coroutines.channels.*
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.selects.SelectClause2
import wasabi375.dynamicgradletask.client.Log
import java.io.*
import java.lang.UnsupportedOperationException
import java.util.*
import kotlin.reflect.KProperty

// CLEANUP should be done using delegation in 1.3.30? (KT-27435): https://youtrack.jetbrains.com/issue/KT-27435
@ExperimentalCoroutinesApi
inline class LineChannel(private val channel: ReceiveChannel<String>) : ReceiveChannel<String>{
    override val isClosedForReceive: Boolean
        get() = channel.isClosedForReceive
    override val isEmpty: Boolean
        get() = channel.isEmpty
    override val onReceive: SelectClause1<String>
        get() = channel.onReceive
    override val onReceiveOrNull: SelectClause1<String?>
        get() = channel.onReceiveOrNull

    override fun cancel() {
        channel.cancel()
    }

    @ObsoleteCoroutinesApi
    override fun cancel(cause: Throwable?): Boolean = channel.cancel(cause)

    override fun iterator(): ChannelIterator<String> = channel.iterator()

    override fun poll(): String? = channel.poll()

    override suspend fun receive(): String = channel.receive()

    @ObsoleteCoroutinesApi
    override suspend fun receiveOrNull(): String? = channel.receiveOrNull()
}

private fun ReceiveChannel<String>.asLineChannel() = LineChannel(this)

@ExperimentalCoroutinesApi
suspend fun InputStream.toLineChannel(interruptSignal: KProperty<Boolean>? = null): LineChannel
        = coroutineScope {
    produce {
        val scanner = Scanner(this@toLineChannel)
        while(true) {
            withContext(Dispatchers.IO) {
                while (!scanner.hasNextLine()) {
                    delay(10)
                }
                send(scanner.nextLine())
            }

            if(interruptSignal != null && interruptSignal.call()) {
                break
            }
        }
    }
}.asLineChannel()

@ExperimentalCoroutinesApi
inline class SendLineChannel(private val stream: BufferedWriter) : SendChannel<String> {

    override val isClosedForSend: Boolean
        get() = false
    override val isFull: Boolean
        get() = false
    override val onSend: SelectClause2<String, SendChannel<String>>
        get() = throw UnsupportedOperationException()   // TODO figure out

    override fun close(cause: Throwable?): Boolean  {
        cause?.let {Log.exception(it) }
        stream.close()
        return true
    }

    override fun invokeOnClose(handler: (cause: Throwable?) -> Unit) = throw UnsupportedOperationException()

    override fun offer(element: String): Boolean {
        stream.write("$element\n")
        return true
    }

    override suspend fun send(element: String){
        offer(element)
    }
}

@ExperimentalCoroutinesApi
fun BufferedWriter.toSendLineChannel() = SendLineChannel(this)

@ExperimentalCoroutinesApi
fun OutputStream.toSendLineChannel() = BufferedWriter(PrintWriter(this)).toSendLineChannel()