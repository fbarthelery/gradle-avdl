/**
 * gradle-avdl is a Gradle plugin to launch and stop Android
 * Virtual devices
 *
 * Copyright (C) 2020-2023 by Frederic-Charles Barthelery.
 *
 * This file is part of gradle-avdl.
 *
 * gradle-avdl is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gradle-avdl is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gradle-avdl.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import retrofit2.HttpException
import java.net.ServerSocket
import java.net.Socket
import java.util.*

private var tunnelId: Long = 0


suspend fun main(args: Array<String>) {
    val parser = ArgParser("wstunnel")
    val tunnelServer by parser.argument(ArgType.String, "tunnelEndpoint",
            "wstunnel server endpoint url")
    val tunnelDesc by parser.option(ArgType.String, "localToRemote", "L",
            "tunnel local to remote PORT:HOST:PORT")
    val flydroidTunnelDesc by parser.option(ArgType.String, "localToFlydroid", "F",
            """tunnel local to remote flydroid device PORT:flydroid-id:service where
                | flydroid-id is the id of your device
                | service is one of adb, console, grpc
            """.trimMargin())

    val apiKey by parser.option(ArgType.String, "apiKey", "k",
            "Api key for the Flydroid service")

    parser.parse(args)

    check(tunnelDesc == null || flydroidTunnelDesc == null) { "Option -L and -F are mutually exclusive"}
    val flydroidTunnelDestination = flydroidTunnelDesc?.let {
        val parts = it.split(":").takeLast(2)
        FlydroidDestination(parts[0], parts[1])
    }

    val ipDestination = tunnelDesc?.let {
        val parts = it.split(":").takeLast(2)
        Destination("tcp", parts[0], parts[1].toInt())
    }

    val destinationResolver = createDestinationResolver(ipDestination, flydroidTunnelDestination, tunnelServer, apiKey)

    val localPortDesc = if (ipDestination != null) tunnelDesc else flydroidTunnelDesc
    val localPort = with(localPortDesc!!) {
        val parts = split(":")
        if (parts.size >= 3) {
            parts[0].toInt()
        } else
            0
    }

    withContext(Dispatchers.IO) {
        ServerSocket(localPort).use { serverSocket ->
            println("Listening port: ${serverSocket.localPort}")
            println("Via: $tunnelServer")
            while (true) {
                val destination = destinationResolver.getDestination()
                if (destination == null) {
                    println("Destination is stopped. Quit")
                    break
                }
                println("Destination: $destination")
                val socket = serverSocket.accept()
                val tunnel = Tunnel("${tunnelId++}", socket, tunnelServer.trim('/'), destination, apiKey)
                println("accept a connection. start tunnel ${tunnel.tunnelId}")
                tunnel.run()
            }
        }
    }
}

private interface DestinationResolver {
    suspend fun getDestination(): Destination?
}

private fun createDestinationResolver(destination: Destination?, flydroidTunnelDestination: FlydroidDestination?,
                              tunnelServer: String, apiKey: String?
): DestinationResolver {
    return if (destination != null) {
        FixedDestinationResolver(destination)
    } else {
        FlydroidDestinationResolver(tunnelServer, flydroidTunnelDestination!!, apiKey)
    }
}

private class FixedDestinationResolver(
        private val destination: Destination
): DestinationResolver {
    override suspend fun getDestination(): Destination = destination
}

private class FlydroidDestinationResolver(
        private val tunnelServer: String,
        private val flydroidDestination: FlydroidDestination,
        private val apiKey: String? = null
): DestinationResolver {

    val flydroidService by lazy {
        val tunnelServerUrl = when {
            tunnelServer.startsWith("ws://") -> tunnelServer.replaceFirst("ws://", "http://")
            tunnelServer.startsWith("wss://") -> tunnelServer.replaceFirst("wss://", "https://")
            else -> tunnelServer
        }
        createFlydroidService(tunnelServerUrl, apiKey)
    }

    override suspend fun getDestination(): Destination? = withContext(Dispatchers.IO) {
        try {
            val device = flydroidService.findVirtualDevice(flydroidDestination.id).body()
            device?.let {
                when (flydroidDestination.service) {
                    FlydroidDestination.Service.ADB -> Destination("tcp", it.ip, it.adbPort)
                    FlydroidDestination.Service.CONSOLE -> Destination("tcp", it.ip, it.consolePort)
                    FlydroidDestination.Service.GRPC -> Destination("tcp", it.ip, it.grpcPort)
                }
            }
        } catch (e: HttpException) {
            null
        }
    }
}

private data class FlydroidDestination(
        val id: String,
        val service: Service
) {
    enum class Service {
        ADB, CONSOLE, GRPC
    }

    constructor(id: String, service: String) : this(id, Service.valueOf(service.toUpperCase()))
}

internal data class Destination(
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

    private suspend fun transferPacket() {
        select {
            localIn.onReceiveCatching { bytesOrClosed ->
                if (bytesOrClosed.isClosed) {
                    println("$tunnelId: local socket is closed ${bytesOrClosed.exceptionOrNull()}")
                    bytesOrClosed.exceptionOrNull()?.printStackTrace()
                    throw ClosedReceiveChannelException("local socket closed").apply {
                        initCause(bytesOrClosed.exceptionOrNull())
                    }
                } else {
                    destinationOutgoing.send(bytesOrClosed.getOrThrow())
                }
            }

            destinationIncoming.onReceiveCatching { bytesOrClosed ->
                if (bytesOrClosed.isClosed) {
                    println("$tunnelId: websocket is closed ${bytesOrClosed.exceptionOrNull()}")
                    bytesOrClosed.exceptionOrNull()?.printStackTrace()
                    throw ClosedReceiveChannelException("websocket closed").apply {
                        initCause(bytesOrClosed.exceptionOrNull())
                    }
                } else {
                    localOut.send(bytesOrClosed.getOrThrow())
                }
            }
        }
    }

    private fun disconnect() = launch {
        println("$tunnelId: Closing tunnel $local")
        webSocket?.close(1000, null)
        @Suppress("BlockingMethodInNonBlockingContext")
        local.close()
        this@Tunnel.cancel()
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
