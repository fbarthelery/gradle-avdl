package com.geekorum.gradle.avdl.providers.flydroid

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select
import okhttp3.*
import okio.*
import org.apache.log4j.LogManager
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket
import java.util.*

private var tunnelId: Long = 0


// TODO make tunnel ask for a specific nomad allocation (by name)
// so that we can stop the tunnel if the allocation is stopped
fun main(args: Array<String>) {
    val parser = ArgParser("wstunnel")
    val tunnelServer by parser.argument(ArgType.String, "tunnelEndpoint",
            "wstunnel server endpoint url")
    val tunnelDesc by parser.option(ArgType.String, "localToRemote", "L",
            "tunnel local to remote PORT:HOST:PORT").required()

    val apiKey by parser.option(ArgType.String, "apiKey", "k",
            "Api key for the Flydroid service")

    parser.parse(args)

    val destination = with(tunnelDesc) {
        val parts = split(":").takeLast(2)
        Destination("tcp", parts[0], parts[1].toInt())
    }
    val localPort = with(tunnelDesc) {
        val parts = split(":")
        if (parts.size >= 3) {
            parts[0].toInt()
        } else
            0
    }

    ServerSocket(localPort).use { serverSocket ->
        println("Listening port: ${serverSocket.localPort}")
        println("Destination: $destination")
        println("Via: $tunnelServer")
        while (true) {
            val socket = serverSocket.accept()
            val tunnel = Tunnel("${tunnelId++}", socket, tunnelServer.trim('/'), destination, apiKey)
            println("accept a connection. start tunnel ${tunnel.tunnelId}")
            tunnel.run()
        }
    }

}


private data class Destination(
        val protocol: String,
        val host: String,
        val port: Int
)

@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
private class Tunnel(
        val tunnelId: String = UUID.randomUUID().toString(),
        private val local: Socket,
        private val wsUrl: String,
        private val destination: Destination,
        private val apiKey: String? = null
) : CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private var webSocket: WebSocket? = null

    // read from local and produces bytes
    private val localIn = produce {
        @Suppress("BlockingMethodInNonBlockingContext")
        local.source().buffer().use {
            val buffer = Buffer()
            while (true) {
                val read = it.read(buffer, 4096)
                if (read != -1L) {
                    send(buffer.readByteString())
                } else {
                    return@produce
                }
            }
        }
    }

    // writes bytes to local
    private val localOut = actor<ByteString> {
        @Suppress("BlockingMethodInNonBlockingContext")
        local.sink().buffer().use {
            for (bytes in channel) {
                it.write(bytes)
                it.flush()
            }
        }
    }

    // read from destination websocket
    private var destinationIncoming: Channel<ByteString> = Channel()
    // writes to destination websockt
    private var destinationOutgoing = actor<ByteString> {
        for (bytes in channel) {
            webSocket?.send(bytes)
        }
    }

    private val websocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("$tunnelId: websocket connected, $response")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            launch {
                destinationIncoming.send(bytes)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("$tunnelId: websocket failed, $response, t")
            t.printStackTrace()
            disconnect()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            println("$tunnelId: websocket closing, $code, $reason")
            disconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            println("$tunnelId: websocket closed, $code, $reason")
            disconnect()
        }
    }

    fun run() = launch {
        connectToDestination()
        try {
            while (local.isConnected) {
                transferPacket()
            }
        } catch (e: ClosedReceiveChannelException) {
            println("$tunnelId: One end of the tunnel was closed $e" )
            e.printStackTrace()
        } finally {
            disconnect()
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    private suspend fun transferPacket() {
        select<Unit> {
            localIn.onReceiveOrClosed { bytesOrClosed ->
                if (bytesOrClosed.isClosed) {
                    println("$tunnelId: local socket is closed ${bytesOrClosed.closeCause}")
                    bytesOrClosed.closeCause?.printStackTrace()
                    throw ClosedReceiveChannelException("local socket closed").apply {
                        initCause(bytesOrClosed.closeCause)
                    }
                } else {
                    destinationOutgoing.send(bytesOrClosed.value)
                }
            }

            destinationIncoming.onReceiveOrClosed { bytesOrClosed ->
                if (bytesOrClosed.isClosed) {
                    println("$tunnelId: websocket is closed ${bytesOrClosed.closeCause}")
                    bytesOrClosed.closeCause?.printStackTrace()
                    throw ClosedReceiveChannelException("websocket closed").apply {
                        initCause(bytesOrClosed.closeCause)
                    }
                } else {
                    localOut.send(bytesOrClosed.value)
                }
            }
        }
    }

    private fun disconnect() = launch {
        println("$tunnelId: Closing tunnel $local")
        webSocket?.close(1000, null)
        @Suppress("BlockingMethodInNonBlockingContext")
        local.close()
        cancel()
    }

    private fun connectToDestination() {
        val okHttpClient = OkHttpClient.Builder().build()
        val request = with(destination) {
            Request.Builder()
                    .url("$wsUrl/wstunnel/$protocol/$host/$port")
                    .apply {
                        if (apiKey != null) {
                            header("X-FLYDROID-KEY", apiKey)
                        }
                    }
                    .build()
        }
        webSocket = okHttpClient.newWebSocket(request, websocketListener)
    }
}
