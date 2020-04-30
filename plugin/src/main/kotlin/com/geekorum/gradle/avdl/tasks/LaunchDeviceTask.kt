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

abstract class LaunchDeviceTask @Inject constructor(
        objectFactory: ObjectFactory,
        private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    @get:Input
    val devices = objectFactory.listProperty<String>()

    @get:Nested
    internal val deviceDefinitions = project.the<AvdlExtension>().devices

    @Suppress("UnstableApiUsage")
    @TaskAction
    fun launchDevices() {
        val classpath = project.configurations["avdl"]
        devices.get().forEach {
            val deviceDefinition = deviceDefinitions[it]
            workerExecutor.noIsolation().submit(LaunchDeviceWork::class.java) {
                setDeviceDefinitionParams(deviceDefinition)
                avdlClasspath.from(classpath)
            }
        }
    }
}

@Suppress("UnstableApiUsage")
private abstract class LaunchDeviceWork @Inject constructor(
        objectFactory: ObjectFactory
) : DeviceWorker<ProviderWorkerParams>(objectFactory) {

    override fun execute() {
        controller.startDevice()
    }

}
