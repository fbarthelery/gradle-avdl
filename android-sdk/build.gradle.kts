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
plugins {
    `kotlin-dsl`
    `maven-publish`
    kotlin("plugin.serialization")
}

repositories {
    google()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))

    implementation("com.android.tools.build:gradle:3.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")

    api(project(":plugin"))
}

gradlePlugin {
    // Define the plugin
    val androidsdk by plugins.registering {
        id = "com.geekorum.gradle.avdl.providers.android-sdk"
        implementationClass = "com.geekorum.gradle.avdl.providers.androidsdk.AndroidSdkPlugin"
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations.getByName("functionalTestImplementation").extendsFrom(configurations.getByName("testImplementation"))


tasks {

    // Add a task to run the functional tests
    val functionalTest by creating(Test::class) {
        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath
    }

    val check by getting(Task::class) {
        // Run the functional tests as part of `check`
        dependsOn(functionalTest)
    }
}
