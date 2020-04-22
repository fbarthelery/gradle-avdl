package com.geekorum.gradle.avdl.providers.androidsdk

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class AndroidSdkPluginTest {
    @Test
    fun `plugin applies base plugin`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.geekorum.gradle.avdl.providers.android-sdk")

        // Verify the result
        assertNotNull(project.plugins.findPlugin("com.geekorum.gradle.avdl"))
    }
}
