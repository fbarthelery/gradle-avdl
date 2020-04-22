package com.geekorum.gradle.avdl.providers.androidsdk

import com.geekorum.gradle.avdl.DeviceProviderPlugin
import com.geekorum.gradle.avdl.DeviceSetup
import com.geekorum.gradle.avdl.VirtualDeviceController
import com.geekorum.gradle.avdl.VirtualDeviceDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import javax.inject.Inject

@Suppress("UnstableApiUsage", "UnstableApiUsage")
open class AdbRemoteProvider @Inject constructor(
        objectFactory: ObjectFactory,
        private val execOperations: ExecOperations
) : DeviceProviderPlugin(objectFactory) {

    @OptIn(ExperimentalStdlibApi::class)
    override fun createController(configuration: ByteArray): VirtualDeviceController {
        val config = AdbRemoteConfiguration.fromByteArray(configuration)
        return AdbRemoteController(execOperations, config)
    }
}

fun VirtualDeviceDefinition.adbRemote(configure: AdbRemoteConfiguration.() -> Unit): DeviceSetup {
    val configuration = AdbRemoteConfiguration().apply(configure)
            .apply {
                adbExecutable = adbExecutable ?: findAdb(project)?.absolutePath ?: "adb"
            }
    return DeviceSetup(AdbRemoteProvider::class.qualifiedName!!, configuration.toByteArray())
}

@Suppress("UnstableApiUsage")
private class AdbRemoteController(
        private val execOperations: ExecOperations,
        private val config: AdbRemoteConfiguration
) : VirtualDeviceController {
    override fun startDevice() {
        execOperations.exec {
            commandLine(config.adbExecutable, "connect", config.connectString)
            println("command line is $commandLine")
        }
    }

    override fun stopDevice() {
        execOperations.exec {
            commandLine(config.adbExecutable, "disconnect", config.connectString )
        }
    }
}

@Serializable
class AdbRemoteConfiguration {
    var host: String = ""
    var port: Int = 5555

    var adbExecutable: String? = null

    internal val connectString
        get() = "$host:$port"

    @OptIn(ExperimentalStdlibApi::class, UnstableDefault::class)
    fun toByteArray(): ByteArray {
        val json = Json.stringify(serializer(), this)
        return json.encodeToByteArray()
    }

    companion object {
        @OptIn(ExperimentalStdlibApi::class, UnstableDefault::class)
        fun fromByteArray(blob: ByteArray): AdbRemoteConfiguration {
            // the bytes to short conversion may add an addition \0 at the end
            val json = blob.decodeToString().trim {
                it.isWhitespace() || it == '\u0000'
            }
            return Json.parse(serializer(), json)
        }
    }
}
