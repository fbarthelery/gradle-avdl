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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `maven-publish`
    kotlin("plugin.serialization")
    id("com.gradle.plugin-publish")
}

repositories {
    google()
}

kotlin {
    jvmToolchain(11)
}

// Add a source set for the functional test suite
val functionalTest: SourceSet by sourceSets.creating

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    "functionalTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.7.1")

    implementation("com.android.tools.build:gradle:8.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")

    api(project(":plugin"))
}

gradlePlugin {
    website.set("https://github.com/fbarthelery/gradle-avdl/tree/master/android-sdk")
    vcsUrl.set("https://github.com/fbarthelery/gradle-avdl")

    // Define the plugin
    val androidsdk by plugins.registering {
        id = "com.geekorum.gradle.avdl.providers.android-sdk"
        implementationClass = "com.geekorum.gradle.avdl.providers.androidsdk.AndroidSdkPlugin"
        displayName = "Android SDK provider for the Gradle-avdl plugin"
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
    withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
    }
}
