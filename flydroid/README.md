Flydroid Provider
=====================

Gradle Avdl is a gradle plugin that allows you to launch and stop Android Virtual Devices during
your build.

The Flydroid Provider plugin provides Android Virtual devices through the use of the Flydroid platform.

Flydroid provider
===================

The Flydroid provider uses the Flydroid API to create an instance of a Android Virtual device and
connect it to adb

Usage
-----

In `settings.gradle` file, add the necessary repository in your Plugin management block.

```
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            url = uri("https://kotlin.bintray.com/kotlinx")
        }
    }
}
```

Add the plugin in your plugins block

```
plugins {
    id("com.geekorum.gradle.avdl.providers.flydroid") version "0.0.2"
}
```

Use the FlydroidProvider to declare the device

```
import com.geekorum.gradle.avdl.providers.flydroid.FlydroidProvider

avdl {
    devices {
        "my-flydroid-device" {
            use (FlydroidProvider) {
                setup =  flydroid { // call the flydroid function to configure the device
                    url = "https://flydroid.example.com"
                    flydroidKey = "my api key"
                    image = "android-n" // or other images available on your provider
                    useTunnel = true // use websocket tunnel to connect to adb
                }
            }
        }
    }
}
```

Create tasks to use your devices

```
tasks.register<LaunchDeviceTask>("launchFlydroidDevice") {
    devices.set(listOf("my-flydroid-device"))
}

tasks.register<StopDeviceTask>("stopFlydroidDevice") {
    devices.set(listOf("my-flydroid-device"))
}
```
