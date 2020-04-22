package com.geekorum.gradle.avdl.tasks

import com.geekorum.gradle.avdl.AvdlExtension
import com.geekorum.gradle.avdl.DeviceProviderPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.the
import org.gradle.workers.WorkAction
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
        val classpath = project.buildscript.configurations["classpath"]
        devices.get().forEach {
            val deviceDefinition = deviceDefinitions[it]
            workerExecutor.noIsolation().submit(LaunchDeviceWork::class.java) {
                val deviceProviderPlugin = checkNotNull(deviceDefinition.setup?.deviceProviderPlugin)
                val configurationBlob = checkNotNull(deviceDefinition.setup?.configuration)
                buildScriptClasspath.set(classpath.asPath)
                pluginClass.set(deviceProviderPlugin)
                setConfiguration(configurationBlob)
            }
        }
    }
}

@Suppress("UnstableApiUsage")
private abstract class LaunchDeviceWork @Inject constructor(
        private val objectFactory: ObjectFactory
) : WorkAction<ProviderWorkerParams> {
    override fun execute() {
        val plugin = createPlugin()
        val controller = plugin.createController(parameters.getConfigurationBlob())
        controller.startDevice()
    }

    private fun createPlugin(): DeviceProviderPlugin {
        val pluginClassName = parameters.pluginClass.get()
        val pluginClass = Class.forName(pluginClassName)
        return (objectFactory.newInstance(pluginClass) as DeviceProviderPlugin).apply {
            buildscriptClasspath.from(parameters.buildScriptClasspath.get().split(":"))
        }
    }

}
