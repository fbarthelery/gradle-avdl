/*
 * gradle-avdl is a Gradle plugin to launch and stop Android
 * Virtual devices
 *
 * Copyright (C) 2020 by Frederic-Charles Barthelery.
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
package com.geekorum.gradle.avdl.providers.flydroid

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.test.Ignore

class FlydroidPluginFunctionalTest {
    companion object {
        @JvmStatic
        fun gradleVersionProvider() = listOf(
                GradleVersion.version("6.3"),
                GradleVersion.current()
        )
    }

    @TempDir
    lateinit var projectDir: File

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    fun `can create device`(gradleVersion: GradleVersion) {
        projectDir.resolve("settings.gradle")
        projectDir.resolve("build.gradle.kts").writeText("""
            import com.geekorum.gradle.avdl.providers.flydroid.flydroid

            plugins {
                id("com.geekorum.gradle.avdl.providers.flydroid")
            }

            avdl {
                devices {
                    register("test") {
                        setup = flydroid {
                            url = "https://flydroid.example.com"
                            flydroidKey = "my api key"
                            image = "android-n"
                            useTunnel = true
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
                .withGradleVersion(gradleVersion.version)
                .forwardOutput()
                .withPluginClasspath().apply {
                    withPluginClasspath(pluginClasspath + listOf(
                            // weird issue: plugin is not found during compilation of gradle kotlin script.
                            // but it is found if we slightly modify its path
                            with(pluginClasspath.first { it.name.matches("""plugin-.*\.jar""".toRegex()) }) {
                                File(parent, "../${parentFile.name}/$name")
                            }
                    ))
                }
                .withArguments(":devices")
                .withProjectDir(projectDir)
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":devices")!!.outcome)
        assertTrue {
            result.output.contains("""name test
            |plugin com.geekorum.gradle.avdl.providers.flydroid.FlydroidProvider
            """.trimMargin())
        }
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    fun `can create device in groovy`(gradleVersion: GradleVersion) {
        projectDir.resolve("settings.gradle")
        projectDir.resolve("build.gradle").writeText("""
            import com.geekorum.gradle.avdl.providers.flydroid.FlydroidProvider

            plugins {
                id("com.geekorum.gradle.avdl.providers.flydroid")
            }

            avdl {
                devices {
                    "test" {
                        use (FlydroidProvider) {
                            setup = flydroid {
                                url = "https://flydroid.example.com"
                                flydroidKey = "my api key"
                                image = "android-n"
                                useTunnel = true
                            }
                        }
                    }
                }
            }

            tasks.register("devices") {
                doLast {
                    def device = avdl.devices["test"]
                    println("name ${'$'}{device.name}")
                    println("plugin ${'$'}{device.setup?.deviceProviderPlugin}")
                }
            }
        """)

        // Run the build
        val result = GradleRunner.create()
                .withGradleVersion(gradleVersion.version)
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("--stacktrace", ":devices")
                .withProjectDir(projectDir)
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":devices")!!.outcome)
        assertTrue {
            result.output.contains("""name test
            |plugin com.geekorum.gradle.avdl.providers.flydroid.FlydroidProvider
            """.trimMargin())
        }
    }

    @Ignore("Needs to run with java 17")
    @Test
    fun `can create device with android plugin`() {
        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                    google()
                }
            }
            """
        )
        projectDir.resolve("build.gradle.kts").writeText("""
            import com.geekorum.gradle.avdl.providers.flydroid.flydroid

            plugins {
                id("com.android.application") version "8.0.0"
                id("com.geekorum.gradle.avdl.providers.flydroid")
            }

            android {
                namespace = "com.geekorum.gradle.avdl.testproject.android"
                compileSdk = 33
            }

            avdl {
                devices {
                    register("test") {
                        setup = flydroid {
                            url = "https://flydroid.example.com"
                            flydroidKey = "my api key"
                            image = "android-n"
                            useTunnel = true
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
            .withGradleVersion(GradleVersion.current().version)
            .forwardOutput()
            .withPluginClasspath().apply {
                withPluginClasspath(pluginClasspath + listOf(
                    // weird issue: plugin is not found during compilation of gradle kotlin script.
                    // but it is found if we slightly modify its path
                    with(pluginClasspath.first { it.name.matches("""plugin-.*\.jar""".toRegex()) }) {
                        File(parent, "../${parentFile.name}/$name")
                    }
                ))
            }
            .withArguments(":devices")
            .withProjectDir(projectDir)
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":devices")!!.outcome)
        assertTrue {
            result.output.contains("""name test
                |plugin com.geekorum.gradle.avdl.providers.flydroid.FlydroidProvider
                """.trimMargin())
        }
    }

}
