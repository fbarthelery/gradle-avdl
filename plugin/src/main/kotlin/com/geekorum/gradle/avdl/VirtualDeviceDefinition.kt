package com.geekorum.gradle.avdl

open class VirtualDeviceDefinition(
        val name: String
) {
    var setup: DeviceSetup? = null
}
