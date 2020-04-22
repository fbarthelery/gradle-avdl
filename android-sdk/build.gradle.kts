plugins {
    `kotlin-dsl`
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
