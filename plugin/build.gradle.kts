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
plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.gradle.plugin.publish)
}

kotlin {
    jvmToolchain(8)
}

// Add a source set for the functional test suite
val functionalTest: SourceSet by sourceSets.creating

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation(kotlin("stdlib-jdk8"))

    // Use the Kotlin test library.
    testImplementation(kotlin("test"))

    // Use the Kotlin JUnit integration.
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit.juniper.api)
    testImplementation(libs.junit.juniper.params)
    testRuntimeOnly(libs.junit.juniper.engine)
    "functionalTestRuntimeOnly"(libs.junit.juniper.api)
}

gradlePlugin {
    website.set("https://github.com/fbarthelery/gradle-avdl")
    vcsUrl.set("https://github.com/fbarthelery/gradle-avdl")

    // Define the plugin
    val avdlPlugin by plugins.registering {
        id = "com.geekorum.gradle.avdl"
        displayName = "Plugin to launch Android Virtual Devices during build"
        implementationClass = "com.geekorum.gradle.avdl.AvdlPlugin"
        description = "Launch Android Virtual Devices during your build"
        tags.set(listOf("android", "devices", "testing", "integrationTesting"))
    }
}

gradlePlugin.testSourceSets(functionalTest)
configurations.getByName("functionalTestImplementation").extendsFrom(configurations.getByName("testImplementation"))


tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    // Add a task to run the functional tests
    val functionalTest by creating(Test::class) {
        testClassesDirs = functionalTest.output.classesDirs
        classpath = functionalTest.runtimeClasspath
    }

    val check by getting(Task::class) {
        // Run the functional tests as part of `check`
        dependsOn(functionalTest)
    }
}
