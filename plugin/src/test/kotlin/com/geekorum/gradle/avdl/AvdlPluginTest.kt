package com.geekorum.gradle.avdl

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'com.geekorum.gradle.avdl' plugin.
 */
class AvdlPluginTest {
    @Test
    fun `plugin registers extension`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.geekorum.gradle.avdl")

        // Verify the result
        assertNotNull(project.extensions.findByName("avdl"))
    }

    @Test
    fun `plugin registers configuration`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.geekorum.gradle.avdl")

        // Verify the result
        assertNotNull(project.configurations.findByName("avdl"))
    }

}
