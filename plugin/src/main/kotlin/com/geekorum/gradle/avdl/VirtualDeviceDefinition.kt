package com.geekorum.gradle.avdl

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

open class VirtualDeviceDefinition(
        @Internal
        val project: Project,
        @Input
        val name: String
) {
    @Nested
    var setup: DeviceSetup? = null
}
