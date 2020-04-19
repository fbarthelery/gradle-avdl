package com.geekorum.gradle.avdl

/*
 * Api for providers of VirtualDevice
 */

/**
 * To create a device provider for Gradle Avdl, you must implement this class and provide an
 * instance of [DeviceSetupFunction] so that users can call it in their [VirtualDeviceDefinition]
 */
class DeviceProviderPlugin

typealias DeviceSetupFunction = VirtualDeviceDefinition.() -> DeviceSetup

/**
 * Allows a DeviceProvider to register a VirtualDevice configuration
 */
data class DeviceSetup(
        val deviceProviderPlugin: DeviceProviderPlugin,
        val configuration: Any
)
