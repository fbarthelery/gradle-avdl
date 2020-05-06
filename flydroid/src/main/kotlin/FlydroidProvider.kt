/*
 * gradle-avdl is a Gradle plugin to launch and stop Android
 * Virtual devices
 *
 * Copyright (C) 2020 by Frederic-Charles Barthelery.
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

import com.geekorum.gradle.avdl.DeviceProviderPlugin
import com.geekorum.gradle.avdl.DeviceSetup
import com.geekorum.gradle.avdl.VirtualDeviceController
import com.geekorum.gradle.avdl.VirtualDeviceDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.Okio
import okio.buffer
import okio.source
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.Socket
import javax.inject.Inject

private val LOGGER = LoggerFactory.getLogger("Flydroid")

open class FlydroidProvider @Inject constructor(
        objectFactory: ObjectFactory,
        private val execOperations: ExecOperations
) : DeviceProviderPlugin(objectFactory) {
    override fun createController(configuration: ByteArray): VirtualDeviceController {
        val config = FlydroidConfiguration.fromByteArray(configuration)
        return FlydroidController(execOperations, avdlClasspath, config)
    }
}

fun VirtualDeviceDefinition.flydroid(configure: FlydroidConfiguration.() -> Unit) : DeviceSetup {
    val configuration = FlydroidConfiguration(name).apply(configure)
            .apply {
                adbExecutable = adbExecutable ?: findAdb(project)?.absolutePath ?: "adb"
                adbkey = adbkey ?: getUserAdbKey()
            }
    //TODO check configuration
    return DeviceSetup(FlydroidProvider::class.qualifiedName!!, configuration.toByteArray())
}

@Serializable
class FlydroidConfiguration(
        internal val name: String
) {
    var url: String? = null
    var email: String? = null
    var adbkey: String? = null
    var flydroidKey: String? = null
    var useTunnel = false
    var adbExecutable: String? = null
    var image: String? = null

    @OptIn(ExperimentalStdlibApi::class, UnstableDefault::class)
    fun toByteArray() : ByteArray {
        val json = Json.stringify(serializer(), this)
        return json.encodeToByteArray()
    }

    internal fun getUserAdbKey(): String? {
        return try {
            val userKeyFile = File(System.getProperty("user.home"), ".android/adbkey")
            userKeyFile.readText()
        } catch (e: IOException) {
            null
        }
    }

    companion object {
        @OptIn(ExperimentalStdlibApi::class, UnstableDefault::class)
        fun fromByteArray(blob: ByteArray): FlydroidConfiguration {
            // the bytes conversion may add an addition \0 at the end
            val json = blob.decodeToString().trim {
                it.isWhitespace() || it == '\u0000'
            }
            return Json.parse(serializer(), json)
        }
    }
}

internal class FlydroidController(
        private val execOperations: ExecOperations,
        private val classpath: FileCollection,
        private val configuration: FlydroidConfiguration
) : VirtualDeviceController {

    private val service: FlydroidService by lazy {
        createFlydroidService(configuration.url!!, configuration.flydroidKey)
    }

    private val adb = configuration.adbExecutable!!

    @OptIn(ExperimentalStdlibApi::class)
    override fun startDevice(): Unit = runBlocking {
        LOGGER.info("starting device on flydroid server ${configuration.url}")
        LOGGER.debug("use tunnel ${configuration.useTunnel}")
        val request = StartRequest(
                name = configuration.name,
                image = configuration.image!!,
                email = configuration.email!!,
                adbkey = configuration.adbkey!!)
        val response = service.start(request)
        LOGGER.trace("api response $response")

        val adbConnectString = if (configuration.useTunnel) {
            val tunnelString = "${response.ip}:${response.adbPort}"
            val tunnelServer = requireNotNull(configuration.url)
            val command = buildList<String> {
                this += listOf("java", "-cp", classpath.asPath,
                        "com.geekorum.gradle.avdl.providers.flydroid.FlydroidTunnelKt")
                configuration.flydroidKey?.let {
                    this += listOf("-k", it)
                }
                this += listOf("-L", tunnelString,
                        tunnelServer)
            }

            // exec wait for process to finish, so we can't use it. we need to run the tunnel
            // as a separate process
            val process = ProcessBuilder()
                    .command(command)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .start()

            val processOutput = flow {
                process.inputStream.source().buffer().use { buffer ->
                    do {
                        val line = buffer.readUtf8Line()
                        line?.let { emit(it) }
                    } while (line != null)
                }
            }

            val listeningPort = processOutput.first {
                it.startsWith("Listening port:")
            }.split(":")[1].trim()

            "localhost:$listeningPort"
        } else {
            "${response.ip}:${response.adbPort}"
        }

        // TODO need to find a way to know when the allocation is started ?
        // otherwise adb connect but the device is offline and change of status later
        // during wait-for-device
        waitForConnection(adbConnectString)

        execOperations.exec {
            commandLine(adb, "connect", adbConnectString)
        }
        execOperations.exec {
            commandLine(adb, "-s", adbConnectString, "wait-for-device")
        }

        waitForBootComplete(adbConnectString)
        //TODO add some timeout for the wait operations ?
    }

    private suspend fun waitForBootComplete(adbConnectString: String) {
        do {
            val collectedOuput = Buffer()
            execOperations.exec {
                commandLine(adb, "-s", adbConnectString, "shell", "getprop", "dev.bootcomplete")
                standardOutput = collectedOuput.outputStream()
            }

            val line = collectedOuput.readUtf8Line()
            val hasBooted = line == "1"
            delay(3_000)
        } while (!hasBooted)
    }

    override fun stopDevice() = runBlocking {
        val request = StopRequest(configuration.name)
        val virtualDevice = service.stop(request)
        if (virtualDevice != null) {
            // TODO get the device info, so that we can disconnect the correct adb
            val connectionString = if (configuration.useTunnel) {
                //TODO
                // should not be needed if we make the tunnel close automatically
                "TODO"
            } else {
                "${virtualDevice.ip}:${virtualDevice.adbPort}"
            }

            execOperations.exec {
                // the device may be disconnected automatically before
                isIgnoreExitValue = true
                commandLine(adb, "disconnect", connectionString)
            }
        }
        LOGGER.trace("api response $virtualDevice")
    }
}

private suspend fun waitForConnection(connectionString: String) = withContext(Dispatchers.IO) {
    val (host, port) = connectionString.split(":")
    var connected = false
    while (!connected) {
        try {
            Socket(host, port.toInt()).use {
                connected = true
            }
        } catch (e: IOException) {
            delay(2000)
        }
    }

}
