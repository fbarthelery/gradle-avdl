package com.geekorum.gradle.avdl.providers.androidsdk

import com.android.build.gradle.BaseExtension
import com.geekorum.gradle.avdl.AvdlPlugin
import com.google.common.base.Charsets
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import java.io.File
import java.io.IOException
import java.util.*

class AndroidSdkPlugin : Plugin<Project>{
    override fun apply(target: Project) {
        target.apply<AvdlPlugin>()
    }
}

internal fun findAdb(project: Project): File? {
    // first try to get it from the android plugin
    project.extensions.findByType(BaseExtension::class.java)?.let {
        return it.adbExecutable
    }
    return findSdkLocation(project)?.resolve("platform-tools/adb")
}

internal fun findSdkLocation(project: Project): File? {
    // first try to get it from the android plugin
    project.extensions.findByType(BaseExtension::class.java)?.let {
        return it.sdkDirectory
    }

    val rootDir = project.rootDir
    val localProperties = File(rootDir, "local.properties")
    val properties = Properties()

    if (localProperties.isFile) {
        try {
            localProperties.bufferedReader(Charsets.UTF_8).use {
                properties.load(it)
            }
        } catch (e: IOException) {
            throw RuntimeException(
                    "Unable to read ${localProperties.absolutePath}.",
                    e
            )
        }
    }
    val (sdkLocation, _) = findSdkLocation(properties, rootDir)
    return sdkLocation
}

/**
 * Find the location of the SDK.
 *
 * Returns a Pair<File></File>, Boolean>, where getFirst() returns a File of the SDK location, and
 * getSecond() returns a Boolean indicating whether it is a regular SDK.
 *
 * Returns Pair.of(null, true) when SDK is not found.
 *
 * @param properties Properties, usually configured in local.properties file.
 * @param rootDir directory for resolving relative paths.
 * @return Pair of SDK location and boolean indicating if it's a regular SDK.
 */
private fun findSdkLocation(
        properties: Properties,
        rootDir: File
): Pair<File?, Boolean> {
    fun absoluteOrRelativeSdk(path: String): Pair<File?, Boolean> {
        var sdk = File(path)
        if (!sdk.isAbsolute) {
            sdk = File(rootDir, path)
        }
        return Pair(sdk, true)
    }

    properties.getProperty("sdk.dir")?.let {
        return absoluteOrRelativeSdk(it)
    }

    properties.getProperty("android.dir")?.let {
        return Pair(File(rootDir, it), false)
    }

    val envVar: String? = System.getenv("ANDROID_SDK_ROOT") ?: System.getenv("ANDROID_HOME")
    envVar?.let {
        return absoluteOrRelativeSdk(it)
    }

    System.getProperty("android.home")?.let {
        return Pair(File(it), true)
    }

    return Pair(null, true)
}
