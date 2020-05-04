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
package com.geekorum.gradle.avdl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import javax.inject.Inject

/*
 * Api for providers of VirtualDevice
 */

/**
 * To create a device provider for Gradle Avdl, you must implement this class and provide an
 * instance of DeviceSetupFunction so that users can call it in their [VirtualDeviceDefinition]
 *
 * A DeviceSetupFunction takes a [VirtualDeviceDefinition] as receiver and returns a [DeviceSetup] instance.
 * It can has many parameters.
 *
 * Implementation of this class must not be final. They can have injected Gradle services
 */
abstract class DeviceProviderPlugin @Inject constructor(
        objectFactory: ObjectFactory
) {
        val avdlClasspath = objectFactory.fileCollection()

        /**
         * Create the VirtualDeviceController for this configuration
         */
        abstract fun createController(configuration: ByteArray) : VirtualDeviceController
}

interface VirtualDeviceController {
        fun startDevice()

        fun stopDevice()
}

/**
 * Allows a DeviceProvider to register a VirtualDevice configuration
 */
data class DeviceSetup(
        /**
         * Fully qualified name of the class implementing the [DeviceProviderPlugin]
         */
        @Input
        val deviceProviderPlugin: String,

        /**
         * Configuration blob
         * This configuration blob will be the parameter of your plugin [DeviceProviderPlugin.createController]
         * method
         */
        @Input
        val configuration: ByteArray
) {

        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as DeviceSetup

                if (deviceProviderPlugin != other.deviceProviderPlugin) return false
                if (!configuration.contentEquals(other.configuration)) return false

                return true
        }

        override fun hashCode(): Int {
                var result = deviceProviderPlugin.hashCode()
                result = 31 * result + configuration.contentHashCode()
                return result
        }
}
