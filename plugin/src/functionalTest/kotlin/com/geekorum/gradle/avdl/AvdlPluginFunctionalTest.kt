package com.geekorum.gradle.avdl

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A simple functional test for the 'com.geekorum.gradle.avdl.greeting' plugin.
 */
class AvdlPluginFunctionalTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    @Test
    fun `can run task`() {
        projectDir.root.resolve("settings.gradle").writeText("")
        projectDir.root.resolve("build.gradle").writeText("""
            plugins {
                id('com.geekorum.gradle.avdl')
            }
        """)

        // Run the build
        val result = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(":tasks")
                .withProjectDir(projectDir.root)
                .build();

        // Verify the result
        assertEquals(result.task(":tasks")!!.outcome, TaskOutcome.SUCCESS)
    }

    @Test
    fun `can create some virtual devices definition`() {
        projectDir.root.resolve("settings.gradle").writeText("")
        projectDir.root.resolve("build.gradle").writeText("""
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
                .withProjectDir(projectDir.root).build();

        // Verify the result
        assertEquals(result.task(":tasks")!!.outcome, TaskOutcome.SUCCESS)
    }

}
