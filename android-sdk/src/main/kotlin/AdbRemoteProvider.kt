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
package com.geekorum.gradle.avdl.providers.androidsdk

import com.geekorum.gradle.avdl.DeviceProviderPlugin
import com.geekorum.gradle.avdl.DeviceSetup
import com.geekorum.gradle.avdl.VirtualDeviceController
import com.geekorum.gradle.avdl.VirtualDeviceDefinition
import groovy.lang.Closure
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import javax.inject.Inject
import com.geekorum.gradle.avdl.providers.androidsdk.adbRemote as groovyAdbRemote

open class AdbRemoteProvider @Inject constructor(
        objectFactory: ObjectFactory,
        private val execOperations: ExecOperations
) : DeviceProviderPlugin(objectFactory) {

    override fun createController(configuration: ByteArray): VirtualDeviceController {
        val config = AdbRemoteConfiguration.fromByteArray(configuration)
        return AdbRemoteController(execOperations, config)
    }

    // for Groovy support
    companion object {
        @JvmStatic
        fun adbRemote(deviceDefinition: VirtualDeviceDefinition, fn: Closure<*>): DeviceSetup {
            return groovyAdbRemote(deviceDefinition, fn)
        }
    }
}

fun VirtualDeviceDefinition.adbRemote(configure: AdbRemoteConfiguration.() -> Unit): DeviceSetup {
    val configuration = AdbRemoteConfiguration().apply(configure)
            .apply {
                adbExecutable = adbExecutable ?: findAdb(project)?.absolutePath ?: "adb"
            }
    return DeviceSetup(AdbRemoteProvider::class.qualifiedName!!, configuration.toByteArray())
}

private class AdbRemoteController(
        private val execOperations: ExecOperations,
        private val config: AdbRemoteConfiguration
) : VirtualDeviceController {
    override fun startDevice() {
        execOperations.exec {
            commandLine(config.adbExecutable, "connect", config.connectString)
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
    var host: String = "localhost"
    var port: Int = 5555

    var adbExecutable: String? = null

    internal val connectString
        get() = "$host:$port"

    fun toByteArray(): ByteArray {
        val json = Json.encodeToString(serializer(), this)
        return json.encodeToByteArray()
    }

    companion object {
        fun fromByteArray(blob: ByteArray): AdbRemoteConfiguration {
            // the bytes to short conversion may add an addition \0 at the end
            val json = blob.decodeToString().trim {
                it.isWhitespace() || it == '\u0000'
            }
            return Json.decodeFromString(serializer(), json)
        }
    }
}
