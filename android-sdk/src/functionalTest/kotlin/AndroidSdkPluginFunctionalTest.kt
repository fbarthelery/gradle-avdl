package com.geekorum.gradle.avdl.providers.androidsdk

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidSdkPluginFunctionalTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    @Test
    fun `can create device`() {
        projectDir.root.resolve("settings.gradle")
        projectDir.root.resolve("build.gradle.kts").writeText("""
            import com.geekorum.gradle.avdl.providers.androidsdk.adbRemote

            plugins {
                id("com.geekorum.gradle.avdl.providers.android-sdk")
            }

            avdl {
                devices {
                    register("test") {
                        setup = adbRemote {
                            host = "192.168.1.42"
                            port = 4242
                        }
                    } 
                }
            }

            tasks.register("devices") {
                doLast {
                    val device = avdl.devices["test"]
                    println("name ${'$'}{device.name}")
                    println("plugin ${'$'}{device.setup?.deviceProviderPlugin}")
                }
            }
        """)

        // Run the build
        val result = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath().apply {
                    withPluginClasspath(pluginClasspath + listOf(
                            // weird issue: plugin is not found during compilation of gradle kotlin script.
                            // but it is found if we slightly modify its path
                            with(pluginClasspath.first { it.name.matches("""plugin-.*\.jar""".toRegex()) }) {
                                File(parent, "../${parentFile.name}/$name")
                            }
                    ))
                }.withDebug(true)
                .withArguments(":devices")
                .withProjectDir(projectDir.root)
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":devices")!!.outcome)
        assertTrue {
            result.output.contains("""name test
            |plugin com.geekorum.gradle.avdl.providers.androidsdk.AdbRemoteProvider
            """.trimMargin())
        }
    }
}
