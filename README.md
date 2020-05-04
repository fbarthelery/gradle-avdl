Gradle Avdl
===========

Gradle Avdl is a gradle plugin that allows you to launch and stop Android Virtual Devices during
your build.

The plugin is extensible and can support different Virtual device providers.

Usage
=====

Add the plugin in your plugins block

```
plugins {
    id("com.geekorum.gradle.avdl") version "0.0.1"
}
```

Declare the different devices that your build may use

```
avdl {
    devices {
        register("my-first-device") {
            setup = your-provider-configuration-function {
                // configuration depending on your provider
            }
        }
        register("my-second-device") {
            setup = your-provider-configuration-function {
                // configuration depending on your provider
            }
        }
    }
}
```

Create tasks to use your devices

```
tasks.register<LaunchDeviceTask>("launchFirstDevice") {
    devices.set(listOf("my-first-device"))
}

tasks.register<StopDeviceTask>("stopAllDevices") {
    devices.set(listOf("my-first-device", "my-second-device"))
}
```


Virtual Device Providers API
============================

To create a Virtual Device Provider for Gradle Avdl, you must implement the
[com.geekorum.gradle.avdl.DeviceProviderPlugin](plugin/src/main/kotlin/com/geekorum/gradle/avdl/DeviceProvider.kt)
abstract class and provide an instance of DeviceSetupFunction so that users can call it in their [VirtualDeviceDefinition]

A DeviceSetupFunction takes a [VirtualDeviceDefinition] as receiver and returns a [DeviceSetup] instance.
It can has many parameters. It can be implemented as a DSL to configure your virtual device.

Implementation of DeviceProviderPlugin must not be final. They can have injected Gradle services

An exemple of Virtual device provider is provided in the [android-sdk](android-sdk) subproject
