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
    id("com.gradle.plugin-publish")
}

repositories {
    maven {
        url = uri("https://kotlin.bintray.com/kotlinx")
    }
    google()
}


dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))

    api(project(":plugin"))
    implementation("com.android.tools.build:gradle:3.6.3")

    implementation("com.squareup.retrofit2:retrofit:2.8.1")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
    implementation("com.squareup.okhttp3:okhttp:4.6.0")
    implementation("com.squareup.okio:okio:2.6.0")


    implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.3.5"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.2.1")
}

gradlePlugin {
    // Define the plugin
    val flydroid by plugins.registering {
        id = "com.geekorum.gradle.avdl.providers.flydroid"
        implementationClass = "com.geekorum.gradle.avdl.providers.flydroid.FlydroidPlugin"
        displayName = "Flydroid provider for the Gradle-avdl plugin"
        description = "Launch Android Virtual Devices during your build"
    }
}

pluginBundle {
    website = "https://github.com/fbarthelery/gradle-avdl/tree/master/flydroid"
    vcsUrl = "https://github.com/fbarthelery/gradle-avdl"
    tags = listOf("android", "devices", "testing", "integrationTesting")
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
