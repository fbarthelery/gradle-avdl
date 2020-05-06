package com.geekorum.gradle.avdl.tasks

import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

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
