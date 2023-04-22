/**
 * gradle-avdl is a Gradle plugin to launch and stop Android
 * Virtual devices
 *
 * Copyright (C) 2020-2023 by Frederic-Charles Barthelery.
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

import com.geekorum.gradle.avdl.AvdlExtension
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.the
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class StopDeviceTask@Inject constructor(
        objectFactory: ObjectFactory,
        private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    @get:Input
    val devices = objectFactory.listProperty<String>()

    @get:Nested
    internal val deviceDefinitions = project.the<AvdlExtension>().devices

    @Suppress("UnstableApiUsage")
    @TaskAction
    fun stopDevices() {
        val classpath = project.configurations["avdl"]
        devices.get().forEach {
            val deviceDefinition = deviceDefinitions[it]
            workerExecutor.noIsolation().submit(StopDeviceWork::class.java) {
                setDeviceDefinitionParams(deviceDefinition)
                avdlClasspath.from(classpath)
            }
        }
    }
}

@Suppress("UnstableApiUsage")
private abstract class StopDeviceWork @Inject constructor(
        objectFactory: ObjectFactory
) : DeviceWorker<ProviderWorkerParams>(objectFactory) {
    override fun execute() {
        controller.stopDevice()
    }
}
