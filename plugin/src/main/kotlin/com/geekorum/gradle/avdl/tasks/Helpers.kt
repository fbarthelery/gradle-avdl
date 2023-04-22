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

import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.util.*

/**
 * Set up order of tasks so that target is executed between this.
 * Execution order
 *   - launchDeviceTask
 *   - target
 *   - stopDeviceTask
 */
fun Pair<TaskProvider<LaunchDeviceTask>, TaskProvider<StopDeviceTask>>.orderForTask(
        target: TaskProvider<*>
): Pair<TaskProvider<LaunchDeviceTask>, TaskProvider<StopDeviceTask>> {
    val (startTask, stopTask) = this
    stopTask.configure {
        mustRunAfter(target)
    }

    target.configure {
        dependsOn(startTask)
    }
    return this
}

/**
 * Register a pair of LaunchDeviceTask/StopDeviceTask for devices.
 * The Launch task is finalized by the Stop task.
 */
fun TaskContainer.registerAvdlDevicesTask(
        name: String, devices: List<String>
): Pair<TaskProvider<LaunchDeviceTask>, TaskProvider<StopDeviceTask>> {
    val stopTask  = register<StopDeviceTask>("stopAvdl${name.capitalize()}") {
        this.devices.set(devices)
    }
    val launchTask = register<LaunchDeviceTask>("launchAvdl${name.capitalize()}") {
        finalizedBy(stopTask)
        this.devices.set(devices)
    }
    return launchTask to stopTask
}

private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
