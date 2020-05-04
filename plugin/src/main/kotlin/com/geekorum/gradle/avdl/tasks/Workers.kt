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
package com.geekorum.gradle.avdl.tasks

import com.geekorum.gradle.avdl.DeviceProviderPlugin
import com.geekorum.gradle.avdl.VirtualDeviceController
import com.geekorum.gradle.avdl.VirtualDeviceDefinition
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import javax.inject.Inject


internal interface ProviderWorkerParams : WorkParameters {
    val avdlClasspath: ConfigurableFileCollection
    val pluginClass: Property<String>
    // Seems like gradle can't serialize WorkParameters if they are Byte or primitive array
    // like ShortArray. Works fine with Array<Short> so we need to convert it
    val configuration : Property<Array<Short>>

}

@Suppress("UnstableApiUsage")
internal abstract class DeviceWorker<T :  ProviderWorkerParams> @Inject constructor(
        private val objectFactory: ObjectFactory
) : WorkAction<T> {

    val plugin: DeviceProviderPlugin by lazy { createPlugin() }

    val controller: VirtualDeviceController by lazy {
        plugin.createController(parameters.getConfigurationBlob())
    }

    private fun createPlugin(): DeviceProviderPlugin {
        val pluginClassName = parameters.pluginClass.get()
        val pluginClass = Class.forName(pluginClassName)
        return (objectFactory.newInstance(pluginClass) as DeviceProviderPlugin).apply {
            avdlClasspath.from(parameters.avdlClasspath)
        }
    }
}

internal fun ProviderWorkerParams.setDeviceDefinitionParams(deviceDefinition: VirtualDeviceDefinition) {
    val deviceProviderPlugin = checkNotNull(deviceDefinition.setup?.deviceProviderPlugin)
    val configurationBlob = checkNotNull(deviceDefinition.setup?.configuration)
    pluginClass.set(deviceProviderPlugin)
    setConfiguration(configurationBlob)
}

internal fun ProviderWorkerParams.setConfiguration(blob: ByteArray) {
    configuration.set(blob.toShortArray().toTypedArray())
}

internal fun ProviderWorkerParams.getConfigurationBlob(): ByteArray {
    return configuration.get().toShortArray().toByteArray()
}


private fun ByteArray.toShortArray(): ShortArray {
    val resultSize = size / 2 + size % 2
    return ShortArray(resultSize) {
        val high: Short = (this[it * 2].toInt() shl 8).toShort()
        val low = getOrElse(it * 2 + 1, { 0 } ).toShort()
        (high + low).toShort()
    }
}

private fun ShortArray.toByteArray(): ByteArray {
    val res = ByteArray(this.size * 2)
    forEachIndexed { i  , v->
        res[ i * 2] = ((v.toInt() and 0xFF00) shr 8).toByte()
        res [ i * 2 + 1 ] = (v.toInt() and 0x00FF).toByte()
    }
    return res
}
