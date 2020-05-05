Android Sdk Provider
=====================

Gradle Avdl is a gradle plugin that allows you to launch and stop Android Virtual Devices during
your build.

The Android Sdk Provider plugin provides Android Virtual devices through the use of Android SDK.

Adb Remote provider
===================

The Adb Remote provider use `adb` to connect to an Android device over network.

Usage
-----

Add the plugin in your plugins block

```
plugins {
    id("com.geekorum.gradle.avdl.providers.android-sdk") version "0.0.1"
}
```

Use the AdbRemoteProvider to declare the device

```
import com.geekorum.gradle.avdl.providers.androidsdk.AdbRemoteProvider

avdl {
    devices {
        "my-adb-remote-device" {
            use (AdbRemoteProvider) {
                setup =  adbRemote { // call the adbRemote function to configure the device
                    host = "192.168.1.42"
                    port = 5555
                }
            }
        }
    }
}
```

Create tasks to use your devices

```
tasks.register<LaunchDeviceTask>("launchAdbRemoteDevice") {
    devices.set(listOf("my-adb-remote-device"))
}

tasks.register<StopDeviceTask>("stopAdbRemoteDevice") {
    devices.set(listOf("my-adb-remote-device"))
}
```
