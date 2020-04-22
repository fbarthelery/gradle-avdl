package com.geekorum.gradle.avdl

import org.gradle.api.tasks.Input

open class VirtualDeviceDefinition(
        @Input
        val name: String
) {
    @Input
    var setup: DeviceSetup? = null
}
