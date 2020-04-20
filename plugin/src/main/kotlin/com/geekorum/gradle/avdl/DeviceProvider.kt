package com.geekorum.gradle.avdl

/*
 * Api for providers of VirtualDevice
 */

/**
 * To create a device provider for Gradle Avdl, you must implement this class and provide an
 * instance of DeviceSetupFunction so that users can call it in their [VirtualDeviceDefinition]
 *
 * A DeviceSetupFunction takes a [VirtualDeviceDefinition] as receiver and returns a [DeviceSetup] instance.
 * It can has many parameters
 */
abstract class DeviceProviderPlugin {
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
        val deviceProviderPlugin: String,

        /**
         * Configuration blob
         */
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
