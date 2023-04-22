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
package com.geekorum.gradle.avdl.providers.flydroid

import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler
import org.gradle.kotlin.dsl.get
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlydroidPluginTest {

    @Test
    fun `plugin applies base plugin`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.geekorum.gradle.avdl.providers.flydroid")

        // Verify the result
        assertNotNull(project.plugins.findPlugin("com.geekorum.gradle.avdl"))
    }

    @Test
    fun `plugin add repositories for flydroid`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.geekorum.gradle.avdl.providers.flydroid")

        // Verify the result
        assertNotNull(project.repositories.named(DefaultRepositoryHandler.GOOGLE_REPO_NAME))
        assertNotNull(project.repositories.named(DefaultRepositoryHandler.GRADLE_PLUGIN_PORTAL_REPO_NAME))
    }

    @Test
    fun `plugin add dependencies in avdl configuration`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.geekorum.gradle.avdl.providers.flydroid")

        // Verify the result
        val dependencies = project.configurations["avdl"].dependencies
        assertTrue {
            dependencies.any { it.group == "com.geekorum.gradle.avdl" && it.name == "flydroid" }
        }
        assertTrue {
            dependencies.any { it.group == "org.jetbrains.kotlinx" && it.name == "kotlinx-coroutines-core" }
        }
    }

}
