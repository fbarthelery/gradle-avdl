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
package com.geekorum.gradle.avdl

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.create

class AvdlPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        // add extension
        project.extensions.create<AvdlExtension>("avdl", project)

        project.configurations.register("avdl") {
            isVisible = false
            description = "Dependencies for avdl providers"
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
            }
        }
    }
}
