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
package com.geekorum.gradle.avdl

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A simple functional test for the 'com.geekorum.gradle.avdl.greeting' plugin.
 */
class AvdlPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `can run task`() {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('com.geekorum.gradle.avdl')
            }
        """)

        // Run the build
        val result = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(":tasks")
                .withProjectDir(projectDir)
                .build();

        // Verify the result
        assertEquals(result.task(":tasks")!!.outcome, TaskOutcome.SUCCESS)
    }

    @Test
    fun `can create some virtual devices definition`() {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('com.geekorum.gradle.avdl')
            }

            avdl {
                devices {
                    create("first")
                    register("second")
                }
            }
        """)

        // Run the build
        val result = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(":tasks")
                .withProjectDir(projectDir).build();

        // Verify the result
        assertEquals(result.task(":tasks")!!.outcome, TaskOutcome.SUCCESS)
    }

}
