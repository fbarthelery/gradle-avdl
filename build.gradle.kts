/*
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

import com.geekorum.build.SourceLicenseCheckerPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    `maven-publish`
    alias(libs.plugins.gradle.plugin.publish) apply false
}


allprojects {
    group = "com.geekorum.gradle.avdl"
    version = "0.0.5-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    apply<SourceLicenseCheckerPlugin>()

    plugins.withType<MavenPublishPlugin> {
        publishing {
            repositories {
                maven {
                    this.name = "local"
                    this.url = uri("$rootDir/repo")
                }
            }
        }
    }
}
